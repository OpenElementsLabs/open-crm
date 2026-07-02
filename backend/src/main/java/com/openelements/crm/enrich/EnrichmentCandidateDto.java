package com.openelements.crm.enrich;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * One candidate returned by a search, with its server-computed preview.
 *
 * @param candidateId       opaque id used only for client-side selection
 * @param label             minimal identifying label ("Name @ Company")
 * @param changes           the fill-empty changes this candidate would apply
 * @param companyResolution how the candidate's company name resolves
 * @param nothingToEnrich   {@code true} when every provided field is already filled
 * @param payload           the enrichable values, echoed back on apply
 */
@Schema(description = "A single enrichment candidate with its computed preview")
public record EnrichmentCandidateDto(
    @Schema(description = "Opaque selection id", requiredMode = Schema.RequiredMode.REQUIRED) String candidateId,
    @Schema(description = "Minimal label 'Name @ Company'", requiredMode = Schema.RequiredMode.REQUIRED) String label,
    @Schema(description = "Fill-empty changes", requiredMode = Schema.RequiredMode.REQUIRED)
    List<EnrichmentChangeDto> changes,
    @Schema(description = "Company resolution", requiredMode = Schema.RequiredMode.REQUIRED)
    CompanyResolutionDto companyResolution,
    @Schema(description = "Whether nothing is left to enrich", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean nothingToEnrich,
    @Schema(description = "Echoed enrichable values", requiredMode = Schema.RequiredMode.REQUIRED)
    EnrichmentPayloadDto payload
) {
}
