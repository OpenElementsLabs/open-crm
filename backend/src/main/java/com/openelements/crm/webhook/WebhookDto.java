package com.openelements.crm.webhook;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a webhook registration.
 */
@Schema(description = "Webhook")
public record WebhookDto(
        @Schema(description = "Webhook ID", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(description = "Target URL", requiredMode = Schema.RequiredMode.REQUIRED) String url,
        @Schema(description = "Whether the webhook is active", requiredMode = Schema.RequiredMode.REQUIRED) boolean active,
        @Schema(description = "Creation timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(description = "Last update timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt
) {

    public static WebhookDto fromEntity(final WebhookEntity entity) {
        return new WebhookDto(
                entity.getId(),
                entity.getUrl(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
