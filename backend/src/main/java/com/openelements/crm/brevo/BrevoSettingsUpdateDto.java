package com.openelements.crm.brevo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating the Brevo API key.
 *
 * @param apiKey the Brevo API key
 */
@Schema(description = "Brevo API key update")
record BrevoSettingsUpdateDto(@NotBlank String apiKey) {
}
