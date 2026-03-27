package com.openelements.crm.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating a new comment. The author is set automatically by the backend.
 *
 * @param text the comment text (required)
 */
@Schema(description = "Request body for creating a new comment")
public record CommentCreateDto(
        @NotBlank(message = "Text must not be blank")
        @Schema(description = "Comment text", example = "Had a great meeting today.", requiredMode = Schema.RequiredMode.REQUIRED)
        String text
) {
}
