package com.openelements.crm.enrich;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * The resolution of a candidate's company name against the CRM's existing companies.
 *
 * @param kind        {@code MATCHED} (existing company), {@code NEW} (offer creation), or {@code NONE}
 * @param companyId   the matched company's id when {@code kind == MATCHED}, otherwise {@code null}
 * @param companyName the resolved / proposed company name, or {@code null} when {@code kind == NONE}
 */
@Schema(description = "Resolution of a candidate company name against existing companies")
public record CompanyResolutionDto(
    @Schema(description = "MATCHED, NEW, or NONE", requiredMode = Schema.RequiredMode.REQUIRED)
    CompanyResolution kind,
    @Schema(description = "Matched company id (only when MATCHED)") UUID companyId,
    @Schema(description = "Resolved / proposed company name") String companyName
) {

    /** Shared instance for the "no company data" case. */
    public static final CompanyResolutionDto NONE = new CompanyResolutionDto(CompanyResolution.NONE, null, null);
}
