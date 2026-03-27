package com.openelements.crm.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for updating an existing comment. The author is not modifiable.
 *
 * @param text the updated comment text (required)
 */
@Schema(description = "Request body for updating an existing comment")
public record CommentUpdateDto(
        @NotBlank(message = "Text must not be blank")
        @Schema(description = "Comment text", example = "Updated meeting notes.", requiredMode = Schema.RequiredMode.REQUIRED)
        String text
) {
}
