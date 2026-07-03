package com.openelements.crm.enrich;

import com.fasterxml.jackson.databind.JsonNode;
import com.openelements.spring.base.services.settings.SettingsDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Client for the Dropcontact enrichment API (concrete, no shared provider abstraction).
 *
 * <p>Dropcontact enriches asynchronously: a batch is submitted and then polled by request id until the
 * result is ready. Enrichable output: verified email, phone, company name, position, and a LinkedIn
 * URL. There is no photo.
 *
 * <p>The real API contract is verified manually against a live key during acceptance; the automated
 * tests pin the request/response shape this client is coded against via a mock server.
 */
@Component
public class DropcontactClient {

    private static final Logger LOG = LoggerFactory.getLogger(DropcontactClient.class);
    static final String API_KEY = "dropcontact.api-key";
    private static final String TOKEN_HEADER = "X-Access-Token";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_POLLS = 12;

    private final SettingsDataService settingsService;
    private final RestClient restClient;
    private final long pollIntervalMs;

    public DropcontactClient(final SettingsDataService settingsService,
                             @Value("${enrichment.dropcontact.base-url:https://api.dropcontact.io}") final String baseUrl,
                             @Value("${enrichment.dropcontact.poll-interval-ms:1500}") final long pollIntervalMs) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService must not be null");
        this.pollIntervalMs = pollIntervalMs;
        final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        final JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
            .baseUrl(Objects.requireNonNull(baseUrl, "baseUrl must not be null"))
            .requestFactory(requestFactory)
            .build();
    }

    /** @return whether a Dropcontact API key is stored. */
    public boolean isConfigured() {
        return settingsService.get(API_KEY).isPresent();
    }

    /**
     * Validates a Dropcontact API key by submitting an empty batch. A 401/403 (or any transport
     * failure) is treated as an invalid key.
     *
     * @param apiKey the key to validate
     * @throws ResponseStatusException 400 if the key is invalid
     */
    public void validateApiKey(final String apiKey) {
        Objects.requireNonNull(apiKey, "apiKey must not be null");
        try {
            restClient.post()
                .uri("/batch")
                .header(TOKEN_HEADER, apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("data", List.of()))
                .retrieve()
                .onStatus(status -> status.value() == 401 || status.value() == 403, (request, response) -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Dropcontact API key");
                })
                .onStatus(HttpStatusCode::isError, (request, response) -> { /* empty batch may 400 — key is valid */ })
                .toBodilessEntity();
        } catch (final ResponseStatusException e) {
            throw e;
        } catch (final RuntimeException e) {
            LOG.warn("Dropcontact API key validation failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Dropcontact API key");
        }
    }

    /**
     * Enriches by email when present, otherwise by name + company.
     *
     * @return 0..1 candidates (Dropcontact returns a single best enrichment per input record)
     * @throws ResponseStatusException 503 if not configured, 502 on any downstream failure
     */
    public List<RawCandidate> search(final String email, final String firstName,
                                     final String lastName, final String company) {
        final String apiKey = settingsService.get(API_KEY).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Dropcontact is not configured"));
        final Map<String, Object> record = new LinkedHashMap<>();
        if (email != null && !email.isBlank()) {
            record.put("email", email.trim());
        } else {
            putIfPresent(record, "first_name", firstName);
            putIfPresent(record, "last_name", lastName);
            putIfPresent(record, "company", company);
        }
        final Map<String, Object> body = Map.of("data", List.of(record), "siren", false, "language", "en");
        try {
            final JsonNode result = submitAndAwait(apiKey, body);
            return parse(result);
        } catch (final ResponseStatusException e) {
            throw e;
        } catch (final RuntimeException e) {
            LOG.warn("Dropcontact search failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Enrichment currently unavailable");
        }
    }

    private JsonNode submitAndAwait(final String apiKey, final Map<String, Object> body) {
        final JsonNode submitResponse = restClient.post()
            .uri("/batch")
            .header(TOKEN_HEADER, apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (request, response) -> {
                throw new IllegalStateException("Dropcontact submit returned " + response.getStatusCode());
            })
            .body(JsonNode.class);
        if (submitResponse == null) {
            throw new IllegalStateException("Dropcontact submit returned no body");
        }
        // Some deployments return the enriched data inline; otherwise poll by request id.
        if (hasData(submitResponse)) {
            return submitResponse;
        }
        final String requestId = text(submitResponse, "request_id");
        if (requestId == null) {
            throw new IllegalStateException("Dropcontact submit returned neither data nor request_id");
        }
        for (int attempt = 0; attempt < MAX_POLLS; attempt++) {
            sleep(pollIntervalMs);
            final JsonNode polled = restClient.get()
                .uri("/batch/{id}", requestId)
                .header(TOKEN_HEADER, apiKey)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new IllegalStateException("Dropcontact poll returned " + response.getStatusCode());
                })
                .body(JsonNode.class);
            if (polled != null && hasData(polled)) {
                return polled;
            }
        }
        throw new IllegalStateException("Dropcontact enrichment did not complete in time");
    }

    private static boolean hasData(final JsonNode node) {
        final JsonNode data = node.get("data");
        return data != null && data.isArray() && !data.isEmpty();
    }

    private List<RawCandidate> parse(final JsonNode result) {
        final JsonNode data = result.get("data");
        if (data == null || !data.isArray() || data.isEmpty()) {
            return List.of();
        }
        final JsonNode record = data.get(0);
        final String email = firstEmail(record);
        final String phone = firstPhone(record);
        final String company = text(record, "company");
        final String position = text(record, "job");
        final String linkedin = text(record, "linkedin");

        final Map<String, String> socialLinks = new LinkedHashMap<>();
        if (linkedin != null) {
            socialLinks.put("LINKEDIN", linkedin);
        }
        if (email == null && phone == null && company == null && position == null && socialLinks.isEmpty()) {
            return List.of();
        }
        final EnrichmentPayloadDto payload =
            new EnrichmentPayloadDto(email, position, phone, socialLinks, company, null, null);
        final String name = (text(record, "first_name") + " " + text(record, "last_name")).trim();
        final String label = buildLabel(name, company, email);
        return List.of(new RawCandidate("dropcontact-0", label, payload));
    }

    private static String buildLabel(final String name, final String company, final String email) {
        final String personName = name == null ? "" : name.replace("null", "").trim();
        final String base = personName.isBlank() ? (email == null ? "?" : email) : personName;
        return company == null || company.isBlank() ? base : base + " @ " + company;
    }

    private static String firstEmail(final JsonNode record) {
        final JsonNode email = record.get("email");
        if (email == null) {
            return null;
        }
        if (email.isTextual()) {
            return blankToNull(email.asText());
        }
        if (email.isArray() && !email.isEmpty()) {
            return text(email.get(0), "email");
        }
        return null;
    }

    private static String firstPhone(final JsonNode record) {
        final JsonNode phone = record.get("phone");
        if (phone == null) {
            return text(record, "mobile_phone");
        }
        if (phone.isTextual()) {
            return blankToNull(phone.asText());
        }
        if (phone.isArray() && !phone.isEmpty()) {
            final JsonNode first = phone.get(0);
            return first.isTextual() ? blankToNull(first.asText()) : text(first, "number");
        }
        return null;
    }

    private static void putIfPresent(final Map<String, Object> map, final String key, final String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value.trim());
        }
    }

    private static String text(final JsonNode node, final String field) {
        if (node == null) {
            return null;
        }
        final JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : blankToNull(value.asText(null));
    }

    private static String blankToNull(final String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void sleep(final long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while polling Dropcontact", e);
        }
    }
}
