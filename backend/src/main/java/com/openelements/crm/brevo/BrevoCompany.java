package com.openelements.crm.brevo;

import java.util.List;

/**
 * Represents a company fetched from the Brevo API.
 *
 * @param id                the Brevo company ID
 * @param name              the company name
 * @param domain            the company domain/website
 * @param linkedContactsIds the IDs of contacts linked to this company
 */
record BrevoCompany(long id, String name, String domain, List<Long> linkedContactsIds) {
}
