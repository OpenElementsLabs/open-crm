package com.openelements.crm.enrich;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openelements.spring.base.services.settings.SettingsDataService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@link DropcontactClient} against a mock server. These pin the request/response
 * contract the client parses; the real API is verified manually with a live key during acceptance.
 */
class DropcontactClientTest {

    private MockWebServer server;
    private SettingsDataService settings;
    private DropcontactClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        settings = Mockito.mock(SettingsDataService.class);
        client = new DropcontactClient(settings, server.url("/").toString().replaceAll("/$", ""), 1L);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void parsesInlineEnrichmentByEmail() throws Exception {
        Mockito.when(settings.get(DropcontactClient.API_KEY)).thenReturn(Optional.of("secret"));
        server.enqueue(new MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""
                {"data":[{"first_name":"Max","last_name":"Müller","company":"Open Elements GmbH",
                "job":"CTO","linkedin":"https://linkedin.com/in/maxm",
                "email":[{"email":"max@oe.com","qualification":"pro"}],"phone":"+49 123"}]}
                """));

        final List<RawCandidate> result = client.search("max@oe.com", null, null, null);

        assertEquals(1, result.size());
        final EnrichmentPayloadDto payload = result.get(0).payload();
        assertEquals("max@oe.com", payload.email());
        assertEquals("+49 123", payload.phoneNumber());
        assertEquals("Open Elements GmbH", payload.companyName());
        assertEquals("CTO", payload.position());
        assertEquals("https://linkedin.com/in/maxm", payload.socialLinks().get("LINKEDIN"));

        final RecordedRequest request = server.takeRequest();
        assertEquals("secret", request.getHeader("X-Access-Token"));
        assertTrue(request.getBody().readUtf8().contains("max@oe.com"));
    }

    @Test
    void pollsByRequestIdWhenDataNotInline() {
        Mockito.when(settings.get(DropcontactClient.API_KEY)).thenReturn(Optional.of("secret"));
        server.enqueue(new MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"success\":true,\"request_id\":\"r1\"}"));
        server.enqueue(new MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"data\":[{\"company\":\"Acme\",\"job\":\"Dev\"}]}"));

        final List<RawCandidate> result = client.search("x@y.com", null, null, null);

        assertEquals(1, result.size());
        assertEquals("Acme", result.get(0).payload().companyName());
        assertEquals("Dev", result.get(0).payload().position());
    }

    @Test
    void usesNameAndCompanyWhenNoEmail() throws Exception {
        Mockito.when(settings.get(DropcontactClient.API_KEY)).thenReturn(Optional.of("secret"));
        server.enqueue(new MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"data\":[{\"company\":\"Acme\"}]}"));

        client.search(null, "Jane", "Doe", "Acme");

        final String body = server.takeRequest().getBody().readUtf8();
        assertTrue(body.contains("Jane"));
        assertTrue(body.contains("Acme"));
    }

    @Test
    void unconfiguredThrowsServiceUnavailable() {
        Mockito.when(settings.get(DropcontactClient.API_KEY)).thenReturn(Optional.empty());

        final ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> client.search("x@y.com", null, null, null));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    void downstreamServerErrorBecomesGenericBadGateway() {
        Mockito.when(settings.get(DropcontactClient.API_KEY)).thenReturn(Optional.of("secret"));
        server.enqueue(new MockResponse().setResponseCode(500));

        final ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> client.search("x@y.com", null, null, null));
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
    }

    @Test
    void invalidKeyIsRejected() {
        server.enqueue(new MockResponse().setResponseCode(401));

        final ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> client.validateApiKey("bad"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void validKeyPassesValidation() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        client.validateApiKey("good");
    }
}
