package com.openelements.crm.brevo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO indicating whether the Brevo API key is configured.
 *
 * @param apiKeyConfigured true if a Brevo API key is stored
 */
@Schema(description = "Brevo API settings status")
record BrevoSettingsDto(boolean apiKeyConfigured) {
}
