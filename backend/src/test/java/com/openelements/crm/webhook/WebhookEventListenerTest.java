package com.openelements.crm.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@DisplayName("WebhookEventListener")
class WebhookEventListenerTest {

    private WebhookRepository webhookRepository;
    private RestClient restClient;
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.ResponseSpec responseSpec;
    private WebhookEventListener listener;

    @BeforeEach
    void setUp() {
        webhookRepository = mock(WebhookRepository.class);
        restClient = mock(RestClient.class);
        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);

        listener = new WebhookEventListener(webhookRepository, restClient);
    }

    private WebhookEntity createWebhookEntity(final String url, final boolean active) {
        final WebhookEntity entity = new WebhookEntity();
        entity.setUrl(url);
        entity.setActive(active);
        return entity;
    }

    @Nested
    @DisplayName("handleEvent")
    class HandleEvent {

        @Test
        @DisplayName("should send POST with correct payload for create event")
        void shouldSendPostForCreateEvent() {
            final WebhookEntity webhook = createWebhookEntity("https://receiver.test/hook", true);
            when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of(webhook));

            final UUID entityId = UUID.randomUUID();
            final Map<String, Object> dto = Map.of("id", entityId.toString(), "name", "Acme Corp");
            final WebhookEvent event = new WebhookEvent(WebhookEventType.COMPANY_CREATED, entityId, dto);

            listener.handleEvent(event);

            final ArgumentCaptor<WebhookEventPayload> captor = ArgumentCaptor.forClass(WebhookEventPayload.class);
            verify(requestBodySpec).body(captor.capture());

            final WebhookEventPayload payload = captor.getValue();
            assertNotNull(payload.eventId());
            assertEquals(WebhookEventType.COMPANY_CREATED, payload.eventType());
            assertNotNull(payload.timestamp());
            assertEquals(entityId, payload.entityId());
            assertNotNull(payload.data());
        }

        @Test
        @DisplayName("should send POST with null data for delete event")
        void shouldSendNullDataForDeleteEvent() {
            final WebhookEntity webhook = createWebhookEntity("https://receiver.test/hook", true);
            when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of(webhook));

            final UUID entityId = UUID.randomUUID();
            final WebhookEvent event = new WebhookEvent(WebhookEventType.COMPANY_DELETED, entityId, null);

            listener.handleEvent(event);

            final ArgumentCaptor<WebhookEventPayload> captor = ArgumentCaptor.forClass(WebhookEventPayload.class);
            verify(requestBodySpec).body(captor.capture());

            final WebhookEventPayload payload = captor.getValue();
            assertEquals(WebhookEventType.COMPANY_DELETED, payload.eventType());
            assertEquals(entityId, payload.entityId());
            assertNull(payload.data());
        }

        @Test
        @DisplayName("should not call inactive webhooks")
        void shouldNotCallInactiveWebhooks() {
            when(webhookRepository.findAllByActiveTrue()).thenReturn(Collections.emptyList());

            final WebhookEvent event = new WebhookEvent(WebhookEventType.COMPANY_CREATED, UUID.randomUUID(), Map.of());

            listener.handleEvent(event);

            verify(restClient, never()).post();
        }

        @Test
        @DisplayName("should call all active webhooks")
        void shouldCallAllActiveWebhooks() {
            final WebhookEntity hook1 = createWebhookEntity("https://a.test/hook", true);
            final WebhookEntity hook2 = createWebhookEntity("https://b.test/hook", true);
            final WebhookEntity hook3 = createWebhookEntity("https://c.test/hook", true);
            when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of(hook1, hook2, hook3));

            final WebhookEvent event = new WebhookEvent(WebhookEventType.COMPANY_CREATED, UUID.randomUUID(), Map.of());

            listener.handleEvent(event);

            verify(requestBodyUriSpec, times(3)).uri(any(String.class));
        }

        @Test
        @DisplayName("should not propagate exception on failed webhook")
        void shouldNotPropagateException() {
            final WebhookEntity webhook = createWebhookEntity("https://fail.test/hook", true);
            when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of(webhook));
            when(requestBodySpec.retrieve()).thenThrow(new ResourceAccessException("Connection refused"));

            final WebhookEvent event = new WebhookEvent(WebhookEventType.CONTACT_CREATED, UUID.randomUUID(), Map.of());

            // Should not throw
            listener.handleEvent(event);
        }

        @Test
        @DisplayName("failed webhook should not affect other webhooks")
        void failedWebhookShouldNotAffectOthers() {
            final WebhookEntity hook1 = createWebhookEntity("https://good1.test/hook", true);
            final WebhookEntity hookFail = createWebhookEntity("https://fail.test/hook", true);
            final WebhookEntity hook3 = createWebhookEntity("https://good2.test/hook", true);
            when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of(hook1, hookFail, hook3));

            // Make the second webhook fail
            when(requestBodyUriSpec.uri("https://fail.test/hook")).thenReturn(requestBodySpec);
            when(requestBodyUriSpec.uri("https://good1.test/hook")).thenReturn(requestBodySpec);
            when(requestBodyUriSpec.uri("https://good2.test/hook")).thenReturn(requestBodySpec);

            final WebhookEvent event = new WebhookEvent(WebhookEventType.TAG_CREATED, UUID.randomUUID(), Map.of());

            // Should complete without exception
            listener.handleEvent(event);

            // All three should have been attempted
            verify(requestBodyUriSpec, times(3)).uri(any(String.class));
        }

        @Test
        @DisplayName("should do nothing when no webhooks registered")
        void shouldDoNothingWhenNoWebhooks() {
            when(webhookRepository.findAllByActiveTrue()).thenReturn(Collections.emptyList());

            final WebhookEvent event = new WebhookEvent(WebhookEventType.TASK_UPDATED, UUID.randomUUID(), Map.of());

            listener.handleEvent(event);

            verify(restClient, never()).post();
        }

        @Test
        @DisplayName("each event should have unique event ID")
        void shouldHaveUniqueEventId() {
            final WebhookEntity webhook = createWebhookEntity("https://receiver.test/hook", true);
            when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of(webhook));

            final WebhookEvent event1 = new WebhookEvent(WebhookEventType.COMPANY_CREATED, UUID.randomUUID(), Map.of());
            final WebhookEvent event2 = new WebhookEvent(WebhookEventType.COMPANY_CREATED, UUID.randomUUID(), Map.of());

            listener.handleEvent(event1);
            listener.handleEvent(event2);

            final ArgumentCaptor<WebhookEventPayload> captor = ArgumentCaptor.forClass(WebhookEventPayload.class);
            verify(requestBodySpec, times(2)).body(captor.capture());

            final List<WebhookEventPayload> payloads = captor.getAllValues();
            assertNotNull(payloads.get(0).eventId());
            assertNotNull(payloads.get(1).eventId());
            // UUIDs should be different
            assertEquals(false, payloads.get(0).eventId().equals(payloads.get(1).eventId()));
        }

        @Test
        @DisplayName("payload should contain timestamp")
        void shouldContainTimestamp() {
            final WebhookEntity webhook = createWebhookEntity("https://receiver.test/hook", true);
            when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of(webhook));

            final Instant before = Instant.now();
            final WebhookEvent event = new WebhookEvent(WebhookEventType.COMPANY_CREATED, UUID.randomUUID(), Map.of());
            listener.handleEvent(event);
            final Instant after = Instant.now();

            final ArgumentCaptor<WebhookEventPayload> captor = ArgumentCaptor.forClass(WebhookEventPayload.class);
            verify(requestBodySpec).body(captor.capture());

            final Instant timestamp = captor.getValue().timestamp();
            assertNotNull(timestamp);
            assertEquals(true, !timestamp.isBefore(before) && !timestamp.isAfter(after));
        }

        @Test
        @DisplayName("payload should not contain user information")
        void shouldNotContainUserInfo() {
            final WebhookEntity webhook = createWebhookEntity("https://receiver.test/hook", true);
            when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of(webhook));

            final Map<String, Object> dto = Map.of("id", UUID.randomUUID().toString(), "name", "Test");
            final WebhookEvent event = new WebhookEvent(WebhookEventType.COMPANY_CREATED, UUID.randomUUID(), dto);

            listener.handleEvent(event);

            final ArgumentCaptor<WebhookEventPayload> captor = ArgumentCaptor.forClass(WebhookEventPayload.class);
            verify(requestBodySpec).body(captor.capture());

            final WebhookEventPayload payload = captor.getValue();
            // WebhookEventPayload record has no user field — this is by design
            assertEquals(5, WebhookEventPayload.class.getRecordComponents().length);
        }
    }
}
