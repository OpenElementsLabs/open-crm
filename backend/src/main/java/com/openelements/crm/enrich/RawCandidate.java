package com.openelements.crm.enrich;

/**
 * A raw candidate produced by a concrete enrichment client, before the contact-domain
 * {@link ContactEnrichmentApplier} turns it into an authoritative {@link EnrichmentCandidateDto}
 * (computing the fill-empty changes, company resolution, and {@code nothingToEnrich}).
 *
 * @param candidateId opaque id for client-side selection
 * @param label       minimal identifying label ("Name @ Company")
 * @param payload     the enrichable values the client discovered
 */
public record RawCandidate(String candidateId, String label, EnrichmentPayloadDto payload) {
}
