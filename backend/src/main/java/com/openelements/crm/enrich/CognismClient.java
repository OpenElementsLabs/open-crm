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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Client for the Cognism enrichment API (concrete, no shared provider abstraction).
 *
 * <p>Cognism enriches with position, company name, phone, and a LinkedIn URL — no photo. Unlike the
 * other services it may return several matches for a name+company query, which drives the candidate
 * selection list.
 *
 * <p>Cognism ships <strong>configured-but-inactive</strong>: this client is exercised only against
 * mocked responses in tests, and the real API contract is verified later, when the service is switched
 * on in production with a live key.
 */
@Component
public class CognismClient {

    private static final Logger LOG = LoggerFactory.getLogger(CognismClient.class);
    static final String API_KEY = "cognism.api-key";
    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private final SettingsDataService settingsService;
    private final RestClient restClient;

    public CognismClient(final SettingsDataService settingsService,
                         @Value("${enrichment.cognism.base-url:https://app.cognism.com}") final String baseUrl) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService must not be null");
        final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        final JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
            .baseUrl(Objects.requireNonNull(baseUrl, "baseUrl must not be null"))
            .requestFactory(requestFactory)
            .build();
    }

    /** @return whether a Cognism API key is stored. */
    public boolean isConfigured() {
        return settingsService.get(API_KEY).isPresent();
    }

    /**
     * Validates a Cognism API key. A 401/403 (or any transport failure) is treated as invalid.
     *
     * @param apiKey the key to validate
     * @throws ResponseStatusException 400 if the key is invalid
     */
    public void validateApiKey(final String apiKey) {
        Objects.requireNonNull(apiKey, "apiKey must not be null");
        try {
            restClient.get()
                .uri("/api/v1/account")
                .header(API_KEY_HEADER, apiKey)
                .retrieve()
                .onStatus(status -> status.value() == 401 || status.value() == 403, (request, response) -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Cognism API key");
                })
                .onStatus(HttpStatusCode::isError, (request, response) -> { /* other statuses: key accepted */ })
                .toBodilessEntity();
        } catch (final ResponseStatusException e) {
            throw e;
        } catch (final RuntimeException e) {
            LOG.warn("Cognism API key validation failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Cognism API key");
        }
    }

    /**
     * Enriches by email when present, otherwise by name + company.
     *
     * @return 0..N candidates
     * @throws ResponseStatusException 503 if not configured, 502 on any downstream failure
     */
    public List<RawCandidate> search(final String email, final String firstName,
                                     final String lastName, final String company) {
        final String apiKey = settingsService.get(API_KEY).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cognism is not configured"));
        final Map<String, Object> body = new LinkedHashMap<>();
        if (email != null && !email.isBlank()) {
            body.put("email", email.trim());
        } else {
            putIfPresent(body, "first_name", firstName);
            putIfPresent(body, "last_name", lastName);
            putIfPresent(body, "company", company);
        }
        try {
            final JsonNode response = restClient.post()
                .uri("/api/v1/enrich")
                .header(API_KEY_HEADER, apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, resp) -> {
                    throw new IllegalStateException("Cognism enrich returned " + resp.getStatusCode());
                })
                .body(JsonNode.class);
            return parse(response);
        } catch (final ResponseStatusException e) {
            throw e;
        } catch (final RuntimeException e) {
            LOG.warn("Cognism search failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Enrichment currently unavailable");
        }
    }

    private List<RawCandidate> parse(final JsonNode response) {
        if (response == null) {
            return List.of();
        }
        final JsonNode contacts = response.get("contacts");
        if (contacts == null || !contacts.isArray() || contacts.isEmpty()) {
            return List.of();
        }
        final List<RawCandidate> candidates = new ArrayList<>();
        int index = 0;
        for (final JsonNode contact : contacts) {
            final String email = text(contact, "email");
            final String phone = text(contact, "phone");
            final String company = text(contact, "company");
            final String position = text(contact, "jobTitle");
            final String linkedin = text(contact, "linkedinUrl");
            final Map<String, String> socialLinks = new LinkedHashMap<>();
            if (linkedin != null) {
                socialLinks.put("LINKEDIN", linkedin);
            }
            if (email == null && phone == null && company == null && position == null && socialLinks.isEmpty()) {
                continue;
            }
            final EnrichmentPayloadDto payload =
                new EnrichmentPayloadDto(email, position, phone, socialLinks, company, null, null);
            final String name = (nullSafe(text(contact, "firstName")) + " " + nullSafe(text(contact, "lastName"))).trim();
            candidates.add(new RawCandidate("cognism-" + index++, buildLabel(name, company, email), payload));
        }
        return candidates;
    }

    private static String buildLabel(final String name, final String company, final String email) {
        final String base = name == null || name.isBlank() ? (email == null ? "?" : email) : name;
        return company == null || company.isBlank() ? base : base + " @ " + company;
    }

    private static void putIfPresent(final Map<String, Object> map, final String key, final String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value.trim());
        }
    }

    private static String nullSafe(final String value) {
        return value == null ? "" : value;
    }

    private static String text(final JsonNode node, final String field) {
        if (node == null) {
            return null;
        }
        final JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        final String text = value.asText(null);
        return text == null || text.isBlank() ? null : text;
    }
}
