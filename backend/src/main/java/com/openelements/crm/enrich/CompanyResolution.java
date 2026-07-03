package com.openelements.crm.enrich;

/**
 * How a candidate's company name resolves against the existing companies.
 *
 * <ul>
 *   <li>{@link #MATCHED} — an existing company matches the name (case-insensitive); linked as part of
 *       the all-or-nothing apply set.</li>
 *   <li>{@link #NEW} — no existing company matches; creation is offered behind an explicit checkbox.</li>
 *   <li>{@link #NONE} — the candidate provided no usable company name.</li>
 * </ul>
 */
public enum CompanyResolution {
    MATCHED,
    NEW,
    NONE
}
