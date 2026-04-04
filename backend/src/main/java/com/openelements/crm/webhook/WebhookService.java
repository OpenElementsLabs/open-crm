package com.openelements.crm.webhook;

import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service handling webhook CRUD operations.
 */
@Service
@Transactional
public class WebhookService {

    private final WebhookRepository webhookRepository;

    public WebhookService(final WebhookRepository webhookRepository) {
        this.webhookRepository = Objects.requireNonNull(webhookRepository, "webhookRepository must not be null");
    }

    /**
     * Creates a new webhook.
     *
     * @param request the create request
     * @return the created webhook DTO
     */
    public WebhookDto create(final WebhookCreateDto request) {
        Objects.requireNonNull(request, "request must not be null");
        final WebhookEntity entity = new WebhookEntity();
        entity.setUrl(request.url());
        final WebhookEntity saved = webhookRepository.saveAndFlush(entity);
        return WebhookDto.fromEntity(saved);
    }

    /**
     * Returns a webhook by its ID.
     *
     * @param id the webhook ID
     * @return the webhook DTO
     * @throws ResponseStatusException with 404 if not found
     */
    @Transactional(readOnly = true)
    public WebhookDto getById(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return WebhookDto.fromEntity(findOrThrow(id));
    }

    /**
     * Updates an existing webhook.
     *
     * @param id      the webhook ID
     * @param request the update request
     * @return the updated webhook DTO
     * @throws ResponseStatusException with 404 if not found
     */
    public WebhookDto update(final UUID id, final WebhookUpdateDto request) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        final WebhookEntity entity = findOrThrow(id);
        entity.setUrl(request.url());
        entity.setActive(request.active());
        final WebhookEntity saved = webhookRepository.saveAndFlush(entity);
        return WebhookDto.fromEntity(saved);
    }

    /**
     * Deletes a webhook.
     *
     * @param id the webhook ID
     * @throws ResponseStatusException with 404 if not found
     */
    public void delete(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final WebhookEntity entity = findOrThrow(id);
        webhookRepository.delete(entity);
    }

    /**
     * Lists webhooks with pagination.
     *
     * @param pageable pagination parameters
     * @return a page of webhook DTOs
     */
    @Transactional(readOnly = true)
    public Page<WebhookDto> list(final Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable must not be null");
        return webhookRepository.findAll(pageable).map(WebhookDto::fromEntity);
    }

    private WebhookEntity findOrThrow(final UUID id) {
        return webhookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Webhook not found: " + id));
    }
}
