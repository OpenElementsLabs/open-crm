package com.openelements.crm.enrich;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Keyless client for Gravatar. The avatar is guaranteed public via the avatar URL; structured
 * profile fields (job title, company, verified accounts) are best-effort via the public profile JSON
 * and simply omitted when absent.
 *
 * <p>The avatar is requested with {@code d=404} so a missing avatar returns 404 rather than a generic
 * mystery-person placeholder — this keeps placeholder images out of the preview entirely.
 */
@Component
public class GravatarClient {

    private static final Logger LOG = LoggerFactory.getLogger(GravatarClient.class);
    private static final int AVATAR_SIZE = 512;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    /** Maps Gravatar account shortnames to the CRM's social network types. */
    private static final Map<String, String> ACCOUNT_NETWORKS = Map.of(
        "github", "GITHUB",
        "linkedin", "LINKEDIN",
        "twitter", "X",
        "x", "X",
        "mastodon", "MASTODON",
        "youtube", "YOUTUBE",
        "bluesky", "BLUESKY");

    private final RestClient restClient;

    public GravatarClient(@Value("${enrichment.gravatar.base-url:https://gravatar.com}") final String baseUrl) {
        final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        final JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
            .baseUrl(Objects.requireNonNull(baseUrl, "baseUrl must not be null"))
            .requestFactory(requestFactory)
            .build();
    }

    /**
     * Looks up a Gravatar candidate for the given email. Gravatar keys entirely on the email, so it
     * returns at most one candidate.
     *
     * @param email the contact email (the match key)
     * @return the single candidate, or empty if neither an avatar nor any profile field was found
     */
    public Optional<RawCandidate> lookupByEmail(final String email) {
        Objects.requireNonNull(email, "email must not be null");
        final String hash = md5Hex(email);

        final AvatarResult avatar = fetchAvatar(hash);
        final JsonNode profile = fetchProfileEntry(hash);

        String position = null;
        String companyName = null;
        String displayName = null;
        final Map<String, String> socialLinks = new LinkedHashMap<>();
        if (profile != null) {
            displayName = text(profile, "displayName");
            position = text(profile, "job_title");
            companyName = text(profile, "company");
            final JsonNode accounts = profile.get("accounts");
            if (accounts != null && accounts.isArray()) {
                for (final JsonNode account : accounts) {
                    final String shortname = text(account, "shortname");
                    final String url = text(account, "url");
                    if (shortname != null && url != null) {
                        final String network = ACCOUNT_NETWORKS.get(shortname.toLowerCase(Locale.ROOT));
                        if (network != null) {
                            socialLinks.putIfAbsent(network, url);
                        }
                    }
                }
            }
        }

        final boolean empty = avatar == null && position == null && companyName == null && socialLinks.isEmpty();
        if (empty) {
            return Optional.empty();
        }

        final EnrichmentPayloadDto payload = new EnrichmentPayloadDto(
            null, position, null, socialLinks, companyName,
            avatar == null ? null : avatar.base64(),
            avatar == null ? null : avatar.contentType());
        final String label = displayName != null && !displayName.isBlank() ? displayName : email;
        return Optional.of(new RawCandidate(hash, label, payload));
    }

    private AvatarResult fetchAvatar(final String hash) {
        try {
            final ResponseEntity<byte[]> response = restClient.get()
                .uri("/avatar/{hash}?d=404&s={size}", hash, AVATAR_SIZE)
                .retrieve()
                .onStatus(status -> status.value() == 404, (request, resp) -> { /* no avatar */ })
                .onStatus(HttpStatusCode::isError, (request, resp) -> {
                    throw new IllegalStateException("Gravatar avatar returned " + resp.getStatusCode());
                })
                .toEntity(byte[].class);
            if (response.getStatusCode().value() == 404 || response.getBody() == null || response.getBody().length == 0) {
                return null;
            }
            final MediaType contentType = response.getHeaders().getContentType();
            return new AvatarResult(java.util.Base64.getEncoder().encodeToString(response.getBody()),
                contentType == null ? "image/jpeg" : contentType.toString());
        } catch (final RuntimeException e) {
            LOG.warn("Gravatar avatar lookup failed: {}", e.getMessage());
            return null;
        }
    }

    private JsonNode fetchProfileEntry(final String hash) {
        try {
            final JsonNode body = restClient.get()
                .uri("/{hash}.json", hash)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, resp) -> {
                    throw new IllegalStateException("Gravatar profile returned " + resp.getStatusCode());
                })
                .body(JsonNode.class);
            if (body == null) {
                return null;
            }
            final JsonNode entry = body.get("entry");
            if (entry != null && entry.isArray() && !entry.isEmpty()) {
                return entry.get(0);
            }
            return null;
        } catch (final RuntimeException e) {
            LOG.debug("Gravatar profile lookup failed (best-effort): {}", e.getMessage());
            return null;
        }
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

    private static String md5Hex(final String email) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            final byte[] digest = md.digest(email.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            final StringBuilder sb = new StringBuilder(digest.length * 2);
            for (final byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (final NoSuchAlgorithmException e) {
            // MD5 is required by every JVM; this cannot happen.
            throw new IllegalStateException("MD5 not available", e);
        }
    }

    private record AvatarResult(String base64, String contentType) {
    }
}
