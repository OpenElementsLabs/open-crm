package com.openelements.crm.translation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for the {@code POST /api/translate} endpoint.
 *
 * @param text           the source text to translate
 * @param targetLanguage the target language code, either {@code de} or {@code en}
 */
@Schema(description = "Translation request")
record TranslateRequestDto(
        @NotBlank(message = "Text must not be blank")
        @Size(max = 10_000, message = "Text must not exceed 10000 characters")
        @Schema(description = "Source text to translate",
                example = "Hallo Welt",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String text,

        @NotBlank(message = "Target language must not be blank")
        @Pattern(regexp = "de|en", message = "Target language must be either 'de' or 'en'")
        @Schema(description = "Target language code (de or en)",
                example = "en",
                allowableValues = {"de", "en"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String targetLanguage
) {
}
