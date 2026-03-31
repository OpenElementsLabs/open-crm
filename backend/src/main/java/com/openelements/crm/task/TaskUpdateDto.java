package com.openelements.crm.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for updating an existing task. Does not allow changing the owner.
 *
 * @param action  the action description (required)
 * @param dueDate the due date (required)
 * @param status  the task status (required)
 * @param tagIds  tag IDs to assign (null = no change, empty = remove all)
 */
@Schema(description = "Request body for updating an existing task")
public record TaskUpdateDto(
        @NotBlank(message = "Action must not be blank")
        @Schema(description = "Action description", requiredMode = Schema.RequiredMode.REQUIRED)
        String action,

        @NotNull(message = "Due date must not be null")
        @Schema(description = "Due date", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate dueDate,

        @NotNull(message = "Status must not be null")
        @Schema(description = "Task status", requiredMode = Schema.RequiredMode.REQUIRED)
        TaskStatus status,

        @Schema(description = "Tag IDs to assign (null = no change, empty = remove all)")
        List<UUID> tagIds
) {
}
