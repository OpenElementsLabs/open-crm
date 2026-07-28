package com.openelements.crm.opportunity;

/**
 * Fixed status of a sales {@link OpportunityEntity}. The value set is stable and drives future
 * reporting; it is independent of the free-text {@code stage} field. New opportunities default to
 * {@link #OPEN}.
 */
public enum OpportunityStatus {

    /** The opportunity is still being pursued. */
    OPEN,

    /** The opportunity was won. */
    WON,

    /** The opportunity was lost. */
    LOST
}
