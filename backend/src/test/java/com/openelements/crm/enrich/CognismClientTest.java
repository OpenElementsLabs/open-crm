package com.openelements.crm.enrich;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.openelements.spring.base.services.settings.SettingsDataService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@link CognismClient} against a mock server. Cognism ships configured-but-inactive,
 * so it is only ever exercised against mocked responses. These pin the contract the client parses.
 */
class CognismClientTest {

    private MockWebServer server;
    private SettingsDataService settings;
    private CognismClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        settings = Mockito.mock(SettingsDataService.class);
        client = new CognismClient(settings, server.url("/").toString().replaceAll("/$", ""));
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void parsesMultipleCandidates() {
        Mockito.when(settings.get(CognismClient.API_KEY)).thenReturn(Optional.of("secret"));
        server.enqueue(new MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""
                {"contacts":[
                  {"firstName":"Max","lastName":"Müller","jobTitle":"CTO","company":"Acme",
                   "phone":"+49 1","linkedinUrl":"https://linkedin.com/in/maxm"},
                  {"firstName":"Max","lastName":"Müller","jobTitle":"CEO","company":"Beta"}
                ]}
                """));

        final List<RawCandidate> result = client.search(null, "Max", "Müller", null);

        assertEquals(2, result.size());
        assertEquals("CTO", result.get(0).payload().position());
        assertEquals("https://linkedin.com/in/maxm", result.get(0).payload().socialLinks().get("LINKEDIN"));
        assertEquals("Max Müller @ Beta", result.get(1).label());
    }

    @Test
    void unconfiguredThrowsServiceUnavailable() {
        Mockito.when(settings.get(CognismClient.API_KEY)).thenReturn(Optional.empty());

        final ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> client.search("x@y.com", null, null, null));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    void downstreamFailureBecomesGenericBadGateway() {
        Mockito.when(settings.get(CognismClient.API_KEY)).thenReturn(Optional.of("secret"));
        server.enqueue(new MockResponse().setResponseCode(503));

        final ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> client.search("x@y.com", null, null, null));
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
    }

    @Test
    void invalidKeyIsRejected() {
        server.enqueue(new MockResponse().setResponseCode(403));

        final ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> client.validateApiKey("bad"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
