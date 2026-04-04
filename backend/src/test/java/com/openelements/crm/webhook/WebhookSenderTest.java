package com.openelements.crm.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@DisplayName("WebhookSender")
class WebhookSenderTest {

    private RestClient restClient;
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    private RestClient.ResponseSpec responseSpec;
    private WebhookRepository webhookRepository;
    private WebhookSender sender;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(RestClient.RequestBodySpec.class);
        requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
        webhookRepository = mock(WebhookRepository.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(WebhookEventPayload.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        sender = new WebhookSender(restClient, webhookRepository);
    }

    private WebhookEntity createWebhookEntity(final String url) {
        final WebhookEntity entity = new WebhookEntity();
        entity.setUrl(url);
        entity.setActive(true);
        return entity;
    }

    private WebhookEventPayload createPayload(final WebhookEventType eventType) {
        return new WebhookEventPayload(UUID.randomUUID(), eventType, Instant.now(), UUID.randomUUID(), null);
    }

    @Nested
    @DisplayName("successful calls")
    class SuccessfulCalls {

        @Test
        @DisplayName("should store HTTP status 200")
        void shouldStoreStatus200() {
            when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());
            final WebhookEntity webhook = createWebhookEntity("https://example.com/hook");

            sender.sendAndTrack(webhook, createPayload(WebhookEventType.COMPANY_CREATED));

            assertEquals(200, webhook.getLastStatus());
            assertNotNull(webhook.getLastCalledAt());
            verify(webhookRepository).save(webhook);
        }

        @Test
        @DisplayName("should store HTTP status 201")
        void shouldStoreStatus201() {
            when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.status(201).build());
            final WebhookEntity webhook = createWebhookEntity("https://example.com/hook");

            sender.sendAndTrack(webhook, createPayload(WebhookEventType.CONTACT_CREATED));

            assertEquals(201, webhook.getLastStatus());
        }
    }

    @Nested
    @DisplayName("error cases")
    class ErrorCases {

        @Test
        @DisplayName("should store 400 for client error")
        void shouldStore400ForClientError() {
            when(requestBodySpec.retrieve()).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
            final WebhookEntity webhook = createWebhookEntity("https://example.com/hook");

            sender.sendAndTrack(webhook, createPayload(WebhookEventType.PING));

            assertEquals(400, webhook.getLastStatus());
            assertNotNull(webhook.getLastCalledAt());
            verify(webhookRepository).save(webhook);
        }

        @Test
        @DisplayName("should store 500 for server error")
        void shouldStore500ForServerError() {
            when(requestBodySpec.retrieve()).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
            final WebhookEntity webhook = createWebhookEntity("https://example.com/hook");

            sender.sendAndTrack(webhook, createPayload(WebhookEventType.PING));

            assertEquals(500, webhook.getLastStatus());
        }

        @Test
        @DisplayName("should store 0 for connection error")
        void shouldStore0ForConnectionError() {
            when(requestBodySpec.retrieve()).thenThrow(new ResourceAccessException("Connection refused"));
            final WebhookEntity webhook = createWebhookEntity("https://does-not-exist.invalid/hook");

            sender.sendAndTrack(webhook, createPayload(WebhookEventType.PING));

            assertEquals(0, webhook.getLastStatus());
            assertNotNull(webhook.getLastCalledAt());
            verify(webhookRepository).save(webhook);
        }

        @Test
        @DisplayName("should store -1 for timeout")
        void shouldStoreMinusOneForTimeout() {
            when(requestBodySpec.retrieve()).thenThrow(
                    new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out")));
            final WebhookEntity webhook = createWebhookEntity("https://slow.test/hook");

            sender.sendAndTrack(webhook, createPayload(WebhookEventType.PING));

            assertEquals(-1, webhook.getLastStatus());
        }

        @Test
        @DisplayName("should not throw exception on failure")
        void shouldNotThrowOnFailure() {
            when(requestBodySpec.retrieve()).thenThrow(new ResourceAccessException("Connection refused"));
            final WebhookEntity webhook = createWebhookEntity("https://fail.test/hook");

            // Should not throw
            sender.sendAndTrack(webhook, createPayload(WebhookEventType.COMPANY_CREATED));

            verify(webhookRepository).save(webhook);
        }
    }

    @Nested
    @DisplayName("status overwrite")
    class StatusOverwrite {

        @Test
        @DisplayName("should overwrite previous status")
        void shouldOverwritePreviousStatus() {
            final WebhookEntity webhook = createWebhookEntity("https://example.com/hook");
            webhook.setLastStatus(200);
            webhook.setLastCalledAt(Instant.now().minusSeconds(60));

            when(requestBodySpec.retrieve()).thenThrow(
                    new ResourceAccessException("Timeout", new SocketTimeoutException("Read timed out")));

            sender.sendAndTrack(webhook, createPayload(WebhookEventType.COMPANY_UPDATED));

            assertEquals(-1, webhook.getLastStatus());
            verify(webhookRepository).save(webhook);
        }
    }

    @Nested
    @DisplayName("PING payload")
    class PingPayload {

        @Test
        @DisplayName("should send PING payload with correct structure")
        void shouldSendPingPayload() {
            when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());
            final WebhookEntity webhook = createWebhookEntity("https://example.com/hook");

            final WebhookEventPayload payload = new WebhookEventPayload(
                    UUID.randomUUID(), WebhookEventType.PING, Instant.now(), null, null);

            sender.sendAndTrack(webhook, payload);

            final ArgumentCaptor<WebhookEventPayload> captor = ArgumentCaptor.forClass(WebhookEventPayload.class);
            verify(requestBodySpec).body(captor.capture());

            final WebhookEventPayload sent = captor.getValue();
            assertEquals(WebhookEventType.PING, sent.eventType());
            assertNull(sent.entityId());
            assertNull(sent.data());
            assertNotNull(sent.eventId());
            assertNotNull(sent.timestamp());
        }
    }
}
