package com.openelements.crm.task;

import com.openelements.crm.tag.TagEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Task")
public record TaskDto(
        @Schema(description = "Task ID", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(description = "Action description", requiredMode = Schema.RequiredMode.REQUIRED) String action,
        @Schema(description = "Due date", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate dueDate,
        @Schema(description = "Task status", requiredMode = Schema.RequiredMode.REQUIRED) TaskStatus status,
        @Schema(description = "Company ID") UUID companyId,
        @Schema(description = "Company name") String companyName,
        @Schema(description = "Contact ID") UUID contactId,
        @Schema(description = "Contact name") String contactName,
        @Schema(description = "Assigned tag IDs") List<UUID> tagIds,
        @Schema(description = "Number of comments", requiredMode = Schema.RequiredMode.REQUIRED) long commentCount,
        @Schema(description = "Creation timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(description = "Last update timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt
) {

    public static TaskDto fromEntity(final TaskEntity entity, final long commentCount) {
        final UUID companyId = entity.getCompany() != null ? entity.getCompany().getId() : null;
        final String companyName = entity.getCompany() != null ? entity.getCompany().getName() : null;
        final UUID contactId = entity.getContact() != null ? entity.getContact().getId() : null;
        final String contactName = entity.getContact() != null
                ? entity.getContact().getFirstName() + " " + entity.getContact().getLastName()
                : null;
        final List<UUID> tagIds = entity.getTags().stream()
                .map(TagEntity::getId)
                .toList();
        return new TaskDto(
                entity.getId(),
                entity.getAction(),
                entity.getDueDate(),
                entity.getStatus(),
                companyId,
                companyName,
                contactId,
                contactName,
                tagIds,
                commentCount,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
