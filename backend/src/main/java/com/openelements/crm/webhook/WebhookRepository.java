package com.openelements.crm.webhook;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for webhook entities.
 */
public interface WebhookRepository extends JpaRepository<WebhookEntity, UUID> {

    /**
     * Returns all webhooks that are currently active.
     *
     * @return list of active webhooks
     */
    List<WebhookEntity> findAllByActiveTrue();
}
