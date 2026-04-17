package com.openelements.crm.task;

import com.openelements.spring.base.data.WithId;
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
) implements WithId {

}
