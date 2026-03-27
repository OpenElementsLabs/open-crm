package com.openelements.crm.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO representing a comment.
 *
 * @param id        the comment ID
 * @param text      the comment text
 * @param author    the author name
 * @param companyId the company ID (null if attached to a contact)
 * @param contactId the contact ID (null if attached to a company)
 * @param createdAt the creation timestamp
 * @param updatedAt the last update timestamp
 */
@Schema(description = "Comment response")
public record CommentResponse(
        @Schema(description = "Comment ID") UUID id,
        @Schema(description = "Comment text") String text,
        @Schema(description = "Author name") String author,
        @Schema(description = "Company ID (null if attached to a contact)") UUID companyId,
        @Schema(description = "Contact ID (null if attached to a company)") UUID contactId,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Last update timestamp") Instant updatedAt
) {

    /**
     * Creates a response DTO from a comment entity.
     *
     * @param entity the comment entity
     * @return the response DTO
     */
    public static CommentResponse fromEntity(final CommentEntity entity) {
        final UUID companyId = entity.getCompany() != null ? entity.getCompany().getId() : null;
        final UUID contactId = entity.getContact() != null ? entity.getContact().getId() : null;
        return new CommentResponse(
                entity.getId(),
                entity.getText(),
                entity.getAuthor(),
                companyId,
                contactId,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
