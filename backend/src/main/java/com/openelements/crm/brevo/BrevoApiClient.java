package com.openelements.crm.brevo;

import com.fasterxml.jackson.databind.JsonNode;
import com.openelements.crm.settings.SettingsService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * Client for the Brevo v3 REST API. Handles pagination, rate limiting, and retry logic.
 */
@Component
public class BrevoApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(BrevoApiClient.class);
    private static final String BREVO_API_KEY = "brevo.api-key";
    private static final String API_KEY_HEADER = "api-key";
    private static final long RATE_LIMIT_MS = 100;
    private static final int MAX_RETRIES = 3;

    private final SettingsService settingsService;
    private final RestClient restClient;
    private Instant lastRequestTime = Instant.EPOCH;

    /**
     * Creates a new BrevoApiClient.
     *
     * @param settingsService the settings service for reading the API key
     */
    public BrevoApiClient(final SettingsService settingsService) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService must not be null");
        this.restClient = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .build();
    }

    /**
     * Validates the given API key by calling the Brevo account endpoint.
     *
     * @param apiKey the API key to validate
     * @throws ResponseStatusException with status 400 if the key is invalid or the call fails
     */
    public void validateApiKey(final String apiKey) {
        Objects.requireNonNull(apiKey, "apiKey must not be null");
        try {
            restClient.get()
                    .uri("/account")
                    .header(API_KEY_HEADER, apiKey)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (final Exception e) {
            LOG.warn("Brevo API key validation failed: {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(400), "Invalid Brevo API key");
        }
    }

    /**
     * Fetches all companies from Brevo using pagination.
     *
     * @return the list of all Brevo companies
     */
    public List<BrevoCompany> fetchAllCompanies() {
        final String apiKey = getApiKey();
        final List<BrevoCompany> result = new ArrayList<>();
        final int limit = 50;
        int offset = 0;

        while (true) {
            final JsonNode body = executeWithRetry(
                    "/companies?limit=" + limit + "&offset=" + offset, apiKey);
            final JsonNode items = body.get("items");
            if (items == null || !items.isArray() || items.isEmpty()) {
                break;
            }
            for (final JsonNode item : items) {
                final String id = item.get("id").asText();
                final JsonNode attrs = item.get("attributes");
                final String name = attrs != null && attrs.has("name")
                        ? attrs.get("name").asText(null) : null;
                final String domain = attrs != null && attrs.has("domain")
                        ? attrs.get("domain").asText(null) : null;
                final List<Long> linkedContactsIds = new ArrayList<>();
                final JsonNode contactIds = item.get("linkedContactsIds");
                if (contactIds != null && contactIds.isArray()) {
                    for (final JsonNode cid : contactIds) {
                        linkedContactsIds.add(cid.asLong());
                    }
                }
                result.add(new BrevoCompany(id, name, domain, linkedContactsIds));
            }
            if (items.size() < limit) {
                break;
            }
            offset += limit;
        }
        return result;
    }

    /**
     * Fetches all contacts from Brevo using pagination.
     *
     * @return the list of all Brevo contacts
     */
    @SuppressWarnings("unchecked")
    public List<BrevoContact> fetchAllContacts() {
        final String apiKey = getApiKey();
        final List<BrevoContact> result = new ArrayList<>();
        final int limit = 1000;
        int offset = 0;

        while (true) {
            final JsonNode body = executeWithRetry(
                    "/contacts?limit=" + limit + "&offset=" + offset, apiKey);
            final JsonNode contacts = body.get("contacts");
            if (contacts == null || !contacts.isArray() || contacts.isEmpty()) {
                break;
            }
            for (final JsonNode contact : contacts) {
                final long id = contact.get("id").asLong();
                final String email = contact.has("email") ? contact.get("email").asText(null) : null;
                final JsonNode attrsNode = contact.get("attributes");
                final Map<String, Object> attributes;
                if (attrsNode != null && attrsNode.isObject()) {
                    attributes = new com.fasterxml.jackson.databind.ObjectMapper()
                            .convertValue(attrsNode, Map.class);
                } else {
                    attributes = Collections.emptyMap();
                }
                final boolean emailBlacklisted = contact.has("emailBlacklisted") && contact.get("emailBlacklisted").asBoolean(false);
                result.add(new BrevoContact(id, email, attributes, emailBlacklisted));
            }
            if (contacts.size() < limit) {
                break;
            }
            offset += limit;
        }
        return result;
    }

    private String getApiKey() {
        return settingsService.get(BREVO_API_KEY)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatusCode.valueOf(400), "Brevo API key not configured"));
    }

    private JsonNode executeWithRetry(final String uri, final String apiKey) {
        int attempt = 0;
        long backoffMs = 1000;

        while (true) {
            final long currentBackoffMs = backoffMs;
            try {
                waitForRateLimit();
                final JsonNode body = restClient.get()
                        .uri(uri)
                        .header(API_KEY_HEADER, apiKey)
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (request, response) -> {
                            final String resetHeader = response.getHeaders().getFirst("x-sib-ratelimit-reset");
                            long waitMs = currentBackoffMs;
                            if (resetHeader != null) {
                                try {
                                    waitMs = Long.parseLong(resetHeader) * 1000;
                                } catch (final NumberFormatException ignored) {
                                    // use default backoff
                                }
                            }
                            throw new RateLimitException(waitMs);
                        })
                        .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                            throw new ServerErrorException(response.getStatusCode().value());
                        })
                        .body(JsonNode.class);
                lastRequestTime = Instant.now();
                return body;
            } catch (final RateLimitException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    throw new RuntimeException(
                            "Brevo API rate limit exceeded after " + MAX_RETRIES + " retries");
                }
                LOG.warn("Rate limited by Brevo API, waiting {}ms before retry {}/{}",
                        e.getWaitMs(), attempt, MAX_RETRIES);
                sleep(e.getWaitMs());
            } catch (final ServerErrorException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    throw new RuntimeException(
                            "Brevo API server error (" + e.getStatusCode() + ") after "
                                    + MAX_RETRIES + " retries");
                }
                LOG.warn("Brevo API server error {}, retrying {}/{} after {}ms",
                        e.getStatusCode(), attempt, MAX_RETRIES, backoffMs);
                sleep(backoffMs);
                backoffMs *= 2;
            }
        }
    }

    private void waitForRateLimit() {
        final long elapsed = Instant.now().toEpochMilli() - lastRequestTime.toEpochMilli();
        if (elapsed < RATE_LIMIT_MS) {
            sleep(RATE_LIMIT_MS - elapsed);
        }
    }

    private void sleep(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting", e);
        }
    }

    private static class RateLimitException extends RuntimeException {

        private final long waitMs;

        RateLimitException(final long waitMs) {
            this.waitMs = waitMs;
        }

        long getWaitMs() {
            return waitMs;
        }
    }

    private static class ServerErrorException extends RuntimeException {

        private final int statusCode;

        ServerErrorException(final int statusCode) {
            this.statusCode = statusCode;
        }

        int getStatusCode() {
            return statusCode;
        }
    }
}
