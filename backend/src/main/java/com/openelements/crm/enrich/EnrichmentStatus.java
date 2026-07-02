package com.openelements.crm.enrich;

/**
 * The outcome of an enrichment search.
 */
public enum EnrichmentStatus {
    /** At least one candidate was found. */
    MATCH,
    /** No candidate was found. */
    NO_MATCH
}
