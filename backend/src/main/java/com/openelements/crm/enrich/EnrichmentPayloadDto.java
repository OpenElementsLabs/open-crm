package com.openelements.crm.enrich;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * The enrichable values a single external candidate offers for a contact.
 *
 * <p>This is a plain data carrier, not a provider abstraction. It is produced by a concrete
 * enrichment client, echoed to the client inside {@link EnrichmentCandidateDto}, and sent back
 * unchanged on {@code apply} so the operation stays stateless (no server-side session/cache). The
 * apply path re-enforces the fill-empty rule against the current contact, so accepting the echoed
 * values is safe for an admin-only action.
 *
 * @param email            proposed email address, or {@code null}
 * @param position         proposed job position, or {@code null}
 * @param phoneNumber      proposed phone number, or {@code null}
 * @param socialLinks      proposed social links keyed by {@code SocialNetworkType} name; value is a
 *                         URL or handle understood by {@code SocialNetworkType.resolve}
 * @param companyName      proposed company name (resolved against existing companies), or {@code null}
 * @param photoBase64      Base64-encoded avatar bytes (Gravatar only), or {@code null}
 * @param photoContentType MIME type of {@code photoBase64}, or {@code null}
 */
@Schema(description = "Enrichable values offered by an external candidate")
public record EnrichmentPayloadDto(
    @Schema(description = "Proposed email address") String email,
    @Schema(description = "Proposed job position") String position,
    @Schema(description = "Proposed phone number") String phoneNumber,
    @Schema(description = "Proposed social links keyed by network type name") Map<String, String> socialLinks,
    @Schema(description = "Proposed company name") String companyName,
    @Schema(description = "Base64-encoded avatar bytes (Gravatar only)") String photoBase64,
    @Schema(description = "MIME type of the avatar bytes") String photoContentType
) {
}
