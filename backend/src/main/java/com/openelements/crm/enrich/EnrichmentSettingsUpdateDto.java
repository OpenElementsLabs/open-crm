package com.openelements.crm.enrich;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for storing an enrichment service's API key.
 *
 * @param apiKey the API key to validate and store
 */
@Schema(description = "Update an enrichment service API key")
public record EnrichmentSettingsUpdateDto(
    @Schema(description = "The API key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String apiKey
) {
}
