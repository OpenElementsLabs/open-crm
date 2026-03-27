package com.openelements.crm.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating a new comment.
 *
 * @param text   the comment text (required)
 * @param author the author name (required, freetext for now)
 */
@Schema(description = "Request body for creating a new comment")
public record CommentCreateRequest(
        @NotBlank(message = "Text must not be blank")
        @Schema(description = "Comment text", example = "Had a great meeting today.")
        String text,

        @NotBlank(message = "Author must not be blank")
        @Schema(description = "Author name", example = "Hendrik Ebbers")
        String author
) {
}
