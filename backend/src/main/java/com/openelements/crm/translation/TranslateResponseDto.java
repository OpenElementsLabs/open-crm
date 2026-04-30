package com.openelements.crm.translation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO containing the translated text returned by {@code POST /api/translate}.
 *
 * @param translatedText the translated text
 */
@Schema(description = "Translation response")
record TranslateResponseDto(
        @Schema(description = "Translated text", example = "Hello World")
        String translatedText
) {
}
