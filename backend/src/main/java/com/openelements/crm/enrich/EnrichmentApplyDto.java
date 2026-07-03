package com.openelements.crm.enrich;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for applying an enrichment candidate.
 *
 * @param payload       the enrichable values echoed back from the search preview
 * @param createCompany whether to create-and-link a new company when the company resolution is
 *                      {@code NEW} (the one exception to the all-or-nothing rule)
 */
@Schema(description = "Apply an enrichment candidate")
public record EnrichmentApplyDto(
    @Schema(description = "Echoed enrichable values", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull EnrichmentPayloadDto payload,
    @Schema(description = "Create and link a new company (only relevant when resolution is NEW)")
    boolean createCompany
) {
}
