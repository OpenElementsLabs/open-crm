package com.openelements.crm.brevo;

import java.util.Map;

/**
 * Represents a contact fetched from the Brevo API.
 *
 * @param id               the Brevo contact ID
 * @param email            the contact email address
 * @param attributes       the contact attributes map
 * @param emailBlacklisted whether the contact is blacklisted from email campaigns
 */
record BrevoContact(long id, String email, Map<String, Object> attributes, boolean emailBlacklisted) {
}
