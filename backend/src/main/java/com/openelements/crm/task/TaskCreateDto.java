package com.openelements.crm.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for creating a new task.
 *
 * @param action    the action description (required)
 * @param dueDate   the due date (required)
 * @param status    the task status (optional, defaults to OPEN)
 * @param companyId the company ID (XOR with contactId)
 * @param contactId the contact ID (XOR with companyId)
 * @param tagIds    tag IDs to assign
 */
@Schema(description = "Request body for creating a new task")
public record TaskCreateDto(
        @NotBlank(message = "Action must not be blank")
        @Schema(description = "Action description", requiredMode = Schema.RequiredMode.REQUIRED)
        String action,

        @NotNull(message = "Due date must not be null")
        @Schema(description = "Due date", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate dueDate,

        @Schema(description = "Task status (defaults to OPEN if omitted)")
        TaskStatus status,

        @Schema(description = "Company ID (XOR with contactId)")
        UUID companyId,

        @Schema(description = "Contact ID (XOR with companyId)")
        UUID contactId,

        @Schema(description = "Tag IDs to assign")
        List<UUID> tagIds
) {
}
