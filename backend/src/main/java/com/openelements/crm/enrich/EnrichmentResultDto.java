package com.openelements.crm.enrich;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The result of an enrichment search: a status and the candidate list (0..N).
 *
 * @param status     {@code MATCH} if any candidate was found, otherwise {@code NO_MATCH}
 * @param candidates the candidates with their server-computed previews (empty when {@code NO_MATCH})
 */
@Schema(description = "Result of an enrichment search")
public record EnrichmentResultDto(
    @Schema(description = "MATCH or NO_MATCH", requiredMode = Schema.RequiredMode.REQUIRED)
    EnrichmentStatus status,
    @Schema(description = "Candidates with computed previews", requiredMode = Schema.RequiredMode.REQUIRED)
    List<EnrichmentCandidateDto> candidates
) {

    /** Shared instance for the no-match case. */
    public static final EnrichmentResultDto NO_MATCH =
        new EnrichmentResultDto(EnrichmentStatus.NO_MATCH, List.of());
}
