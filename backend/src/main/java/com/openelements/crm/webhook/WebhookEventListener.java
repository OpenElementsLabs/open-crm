package com.openelements.crm.webhook;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

/**
 * Listens for {@link WebhookEvent} instances after transaction commit and fires
 * HTTP POST calls to all active webhooks asynchronously.
 */
@Component
public class WebhookEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookEventListener.class);

    private final WebhookRepository webhookRepository;
    private final RestClient restClient;

    public WebhookEventListener(final WebhookRepository webhookRepository, final RestClient webhookRestClient) {
        this.webhookRepository = Objects.requireNonNull(webhookRepository, "webhookRepository must not be null");
        this.restClient = Objects.requireNonNull(webhookRestClient, "webhookRestClient must not be null");
    }

    /**
     * Handles a webhook event by sending HTTP POST to all active webhooks in parallel.
     *
     * @param event the domain event to broadcast
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEvent(final WebhookEvent event) {
        final List<WebhookEntity> activeWebhooks = webhookRepository.findAllByActiveTrue();
        if (activeWebhooks.isEmpty()) {
            return;
        }

        final WebhookEventPayload payload = new WebhookEventPayload(
                UUID.randomUUID(),
                event.eventType(),
                Instant.now(),
                event.entityId(),
                event.data()
        );

        final CompletableFuture<?>[] futures = activeWebhooks.stream()
                .map(webhook -> CompletableFuture.runAsync(() -> sendWebhook(webhook, payload)))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();
    }

    private void sendWebhook(final WebhookEntity webhook, final WebhookEventPayload payload) {
        try {
            restClient.post()
                    .uri(webhook.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (final Exception e) {
            LOG.warn("Webhook call failed for URL {}: {}", webhook.getUrl(), e.getMessage());
        }
    }
}
