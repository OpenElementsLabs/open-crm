package com.openelements.crm.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating an existing comment.
 *
 * @param text   the updated comment text (required)
 * @param author the updated author name (required)
 */
@Schema(description = "Request body for updating an existing comment")
public record CommentUpdateRequest(
        @NotBlank(message = "Text must not be blank")
        @Schema(description = "Comment text", example = "Updated meeting notes.")
        String text,

        @NotBlank(message = "Author must not be blank")
        @Schema(description = "Author name", example = "Hendrik Ebbers")
        String author
) {
}
