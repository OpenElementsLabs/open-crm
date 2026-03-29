package com.openelements.crm.user;

/**
 * Represents information about the currently authenticated user.
 *
 * @param name  the display name
 * @param email the email address
 */
public record UserInfo(String name, String email) {}
