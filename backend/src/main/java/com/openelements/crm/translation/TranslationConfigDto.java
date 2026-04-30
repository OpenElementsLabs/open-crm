package com.openelements.crm.translation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO indicating whether the translation feature is configured on the backend.
 *
 * @param configured true if all required translation environment variables are set
 */
@Schema(description = "Translation feature configuration status")
record TranslationConfigDto(@Schema(description = "Whether translation is configured", example = "true") boolean configured) {
}
