package com.openelements.crm.enrich;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * Unit tests for {@link GravatarClient}. Gravatar is keyless; these tests pin the request/response
 * shape the client is coded against, using a local mock server. The avatar is requested first, then
 * the public profile JSON.
 */
class GravatarClientTest {

    private MockWebServer server;
    private GravatarClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new GravatarClient(server.url("/").toString().replaceAll("/$", ""));
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void returnsAvatarAndBestEffortProfileFields() {
        server.enqueue(new MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "image/png").setBody("fake-avatar-bytes"));
        server.enqueue(new MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""
                {"entry":[{"displayName":"Max Müller","job_title":"CTO","company":"Open Elements GmbH",
                "accounts":[{"shortname":"github","url":"https://github.com/maxm"},
                {"shortname":"linkedin","url":"https://linkedin.com/in/maxm"}]}]}
                """));

        final Optional<RawCandidate> result = client.lookupByEmail("max@example.com");

        assertTrue(result.isPresent());
        final EnrichmentPayloadDto payload = result.get().payload();
        assertNotNull(payload.photoBase64());
        assertEquals("image/png", payload.photoContentType());
        assertEquals("CTO", payload.position());
        assertEquals("Open Elements GmbH", payload.companyName());
        assertEquals("https://github.com/maxm", payload.socialLinks().get("GITHUB"));
        assertEquals("https://linkedin.com/in/maxm", payload.socialLinks().get("LINKEDIN"));
        assertNull(payload.email(), "Gravatar must not fill email — email is the match key");
        assertEquals("Max Müller", result.get().label());
    }

    @Test
    void missingAvatarAndProfileYieldsEmpty() {
        server.enqueue(new MockResponse().setResponseCode(404));
        server.enqueue(new MockResponse().setResponseCode(404));

        assertTrue(client.lookupByEmail("nobody@example.com").isEmpty());
    }

    @Test
    void profileOnlyStillReturnsCandidate() {
        server.enqueue(new MockResponse().setResponseCode(404));
        server.enqueue(new MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"entry\":[{\"job_title\":\"Engineer\"}]}"));

        final Optional<RawCandidate> result = client.lookupByEmail("eng@example.com");

        assertTrue(result.isPresent());
        assertNull(result.get().payload().photoBase64());
        assertEquals("Engineer", result.get().payload().position());
    }

    @Test
    void unknownSocialAccountsAreIgnored() {
        server.enqueue(new MockResponse().setResponseCode(404));
        server.enqueue(new MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""
                {"entry":[{"job_title":"Dev","accounts":[{"shortname":"tumblr","url":"https://x.tumblr.com"}]}]}
                """));

        final Optional<RawCandidate> result = client.lookupByEmail("dev@example.com");

        assertTrue(result.isPresent());
        assertFalse(result.get().payload().socialLinks().containsKey("WEBSITE"));
        assertTrue(result.get().payload().socialLinks().isEmpty());
    }
}
