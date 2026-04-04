package com.openelements.crm.webhook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("WebhookEventListener")
class WebhookEventListenerTest {

    private WebhookRepository webhookRepository;
    private WebhookSender webhookSender;
    private WebhookEventListener listener;

    @BeforeEach
    void setUp() {
        webhookRepository = mock(WebhookRepository.class);
        webhookSender = mock(WebhookSender.class);
        listener = new WebhookEventListener(webhookRepository, webhookSender);
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
        @DisplayName("should delegate to WebhookSender for each active webhook")
        void shouldDelegateToSender() {
            final WebhookEntity hook1 = createWebhookEntity("https://a.test/hook", true);
            final WebhookEntity hook2 = createWebhookEntity("https://b.test/hook", true);
            when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of(hook1, hook2));

            final WebhookEvent event = new WebhookEvent(WebhookEventType.COMPANY_CREATED, UUID.randomUUID(), Map.of());
            listener.handleEvent(event);

            verify(webhookSender, times(2)).sendAndTrack(any(WebhookEntity.class), any(WebhookEventPayload.class));
        }

        @Test
        @DisplayName("should pass correct payload to sender")
        void shouldPassCorrectPayload() {
            final WebhookEntity webhook = createWebhookEntity("https://receiver.test/hook", true);
            when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of(webhook));

            final UUID entityId = UUID.randomUUID();
            final Map<String, Object> dto = Map.of("id", entityId.toString(), "name", "Acme Corp");
            final WebhookEvent event = new WebhookEvent(WebhookEventType.COMPANY_CREATED, entityId, dto);

            listener.handleEvent(event);

            final ArgumentCaptor<WebhookEventPayload> captor = ArgumentCaptor.forClass(WebhookEventPayload.class);
            verify(webhookSender).sendAndTrack(eq(webhook), captor.capture());

            final WebhookEventPayload payload = captor.getValue();
            org.junit.jupiter.api.Assertions.assertNotNull(payload.eventId());
            org.junit.jupiter.api.Assertions.assertEquals(WebhookEventType.COMPANY_CREATED, payload.eventType());
            org.junit.jupiter.api.Assertions.assertEquals(entityId, payload.entityId());
            org.junit.jupiter.api.Assertions.assertNotNull(payload.data());
        }

        @Test
        @DisplayName("should not call sender when no active webhooks")
        void shouldNotCallSenderWhenNoWebhooks() {
            when(webhookRepository.findAllByActiveTrue()).thenReturn(Collections.emptyList());

            final WebhookEvent event = new WebhookEvent(WebhookEventType.COMPANY_CREATED, UUID.randomUUID(), Map.of());
            listener.handleEvent(event);

            verify(webhookSender, never()).sendAndTrack(any(), any());
        }

        @Test
        @DisplayName("should call sender for all three active webhooks")
        void shouldCallSenderForAllWebhooks() {
            final WebhookEntity hook1 = createWebhookEntity("https://a.test", true);
            final WebhookEntity hook2 = createWebhookEntity("https://b.test", true);
            final WebhookEntity hook3 = createWebhookEntity("https://c.test", true);
            when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of(hook1, hook2, hook3));

            final WebhookEvent event = new WebhookEvent(WebhookEventType.TAG_CREATED, UUID.randomUUID(), Map.of());
            listener.handleEvent(event);

            verify(webhookSender, times(3)).sendAndTrack(any(WebhookEntity.class), any(WebhookEventPayload.class));
        }

        @Test
        @DisplayName("each event should produce unique eventId in payload")
        void shouldProduceUniqueEventIds() {
            final WebhookEntity webhook = createWebhookEntity("https://receiver.test/hook", true);
            when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of(webhook));

            listener.handleEvent(new WebhookEvent(WebhookEventType.COMPANY_CREATED, UUID.randomUUID(), Map.of()));
            listener.handleEvent(new WebhookEvent(WebhookEventType.COMPANY_CREATED, UUID.randomUUID(), Map.of()));

            final ArgumentCaptor<WebhookEventPayload> captor = ArgumentCaptor.forClass(WebhookEventPayload.class);
            verify(webhookSender, times(2)).sendAndTrack(any(), captor.capture());

            final List<WebhookEventPayload> payloads = captor.getAllValues();
            org.junit.jupiter.api.Assertions.assertNotEquals(payloads.get(0).eventId(), payloads.get(1).eventId());
        }
    }
}
