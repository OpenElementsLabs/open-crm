package com.openelements.crm.enrich;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A single proposed field change, computed server-side against the contact's current (empty) value.
 *
 * <p>For the photo change the {@code field} is {@code "photo"} and {@code proposedValue} is
 * {@code null} — the client renders the avatar preview from {@link EnrichmentPayloadDto#photoBase64()}.
 * Social-link changes use the field {@code "socialLinks.<NETWORK>"} (e.g. {@code socialLinks.LINKEDIN}).
 *
 * @param field         the contact field being proposed for filling
 * @param currentValue  the contact's current value (always {@code null}/empty by the fill-empty rule)
 * @param proposedValue the value that would be written
 */
@Schema(description = "A single proposed fill-empty change")
public record EnrichmentChangeDto(
    @Schema(description = "The contact field", requiredMode = Schema.RequiredMode.REQUIRED) String field,
    @Schema(description = "The current (empty) value") String currentValue,
    @Schema(description = "The proposed value") String proposedValue
) {
}
