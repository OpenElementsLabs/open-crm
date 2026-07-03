package com.openelements.crm.enrich;

import com.openelements.crm.contact.ContactDto;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Result of a successful apply: the updated contact plus a GDPR reminder for the admin.
 *
 * @param contact    the updated contact
 * @param gdprNotice a reminder about the Art. 14 information obligation
 */
@Schema(description = "Result of applying an enrichment")
public record EnrichmentApplyResultDto(
    @Schema(description = "Updated contact", requiredMode = Schema.RequiredMode.REQUIRED) ContactDto contact,
    @Schema(description = "GDPR Art. 14 reminder", requiredMode = Schema.RequiredMode.REQUIRED) String gdprNotice
) {
}
