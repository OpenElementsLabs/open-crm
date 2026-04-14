package com.openelements.crm.comment;

import com.openelements.spring.base.data.WithId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO representing a comment.
 *
 * @param id        the comment ID
 * @param text      the comment text
 * @param author    the author name
 * @param companyId the company ID (null if not attached to a company)
 * @param contactId the contact ID (null if not attached to a contact)
 * @param taskId    the task ID (null if not attached to a task)
 * @param createdAt the creation timestamp
 * @param updatedAt the last update timestamp
 */
@Schema(description = "Comment response")
public record CommentDto(
        @Schema(description = "Comment ID", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(description = "Comment text", requiredMode = Schema.RequiredMode.REQUIRED) String text,
        @Schema(description = "Author name", requiredMode = Schema.RequiredMode.REQUIRED) String author,
        @Schema(description = "Company ID (null if not attached to a company)") UUID companyId,
        @Schema(description = "Contact ID (null if not attached to a contact)") UUID contactId,
        @Schema(description = "Task ID (null if not attached to a task)") UUID taskId,
        @Schema(description = "Creation timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(description = "Last update timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt
) implements WithId {

    /**
     * Creates a response DTO from a comment entity.
     *
     * @param entity the comment entity
     * @return the response DTO
     */
    public static CommentDto fromEntity(final CommentEntity entity) {
        final UUID companyId = entity.getCompany() != null ? entity.getCompany().getId() : null;
        final UUID contactId = entity.getContact() != null ? entity.getContact().getId() : null;
        final UUID taskId = entity.getTask() != null ? entity.getTask().getId() : null;
        return new CommentDto(
                entity.getId(),
                entity.getText(),
                entity.getAuthor(),
                companyId,
                contactId,
                taskId,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
