package com.openelements.crm.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Reduced user representation for owner-selection combo boxes. Exposes no email; available to any
 * authenticated user (unlike the IT-ADMIN-only {@code GET /api/users}). The SYSTEM-USER is excluded
 * from the list that produces these.
 */
@Schema(description = "Reduced user option for owner selection")
public record UserOptionDto(
    @Schema(description = "User ID", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
    @Schema(description = "Display name", requiredMode = Schema.RequiredMode.REQUIRED) String name,
    @Schema(description = "Avatar URL, or null if none") String avatarUrl
) {
}
