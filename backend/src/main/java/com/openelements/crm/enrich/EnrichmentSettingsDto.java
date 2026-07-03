package com.openelements.crm.enrich;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Whether an enrichment service's API key is configured. Never exposes the key itself.
 *
 * @param configured {@code true} when a key is stored
 */
@Schema(description = "Enrichment service configuration status")
public record EnrichmentSettingsDto(
    @Schema(description = "Whether an API key is configured", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean configured
) {
}
