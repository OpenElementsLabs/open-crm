package com.openelements.crm.contact;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO representing a contact.
 *
 * @param id            the contact ID
 * @param firstName     the first name
 * @param lastName      the last name
 * @param email         the email address
 * @param position      the job position
 * @param gender        the gender
 * @param linkedInUrl   the LinkedIn profile URL
 * @param phoneNumber   the phone number
 * @param companyId     the company ID
 * @param companyName   the company name (resolved)
 * @param syncedToBrevo whether synced to Brevo
 * @param doubleOptIn   whether double opt-in is confirmed
 * @param language      the preferred language
 * @param createdAt     the creation timestamp
 * @param updatedAt     the last update timestamp
 */
@Schema(description = "Contact response")
public record ContactResponse(
        @Schema(description = "Contact ID") UUID id,
        @Schema(description = "First name") String firstName,
        @Schema(description = "Last name") String lastName,
        @Schema(description = "Email address") String email,
        @Schema(description = "Job position") String position,
        @Schema(description = "Gender") Gender gender,
        @Schema(description = "LinkedIn profile URL") String linkedInUrl,
        @Schema(description = "Phone number") String phoneNumber,
        @Schema(description = "Company ID") UUID companyId,
        @Schema(description = "Company name") String companyName,
        @Schema(description = "Whether synced to Brevo") boolean syncedToBrevo,
        @Schema(description = "Whether double opt-in is confirmed") boolean doubleOptIn,
        @Schema(description = "Preferred language") Language language,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Last update timestamp") Instant updatedAt
) {

    /**
     * Creates a response DTO from a contact entity.
     *
     * @param entity the contact entity
     * @return the response DTO
     */
    public static ContactResponse fromEntity(final ContactEntity entity) {
        final UUID companyId = entity.getCompany() != null ? entity.getCompany().getId() : null;
        final String companyName = entity.getCompany() != null ? entity.getCompany().getName() : null;
        return new ContactResponse(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPosition(),
                entity.getGender(),
                entity.getLinkedInUrl(),
                entity.getPhoneNumber(),
                companyId,
                companyName,
                entity.isSyncedToBrevo(),
                entity.isDoubleOptIn(),
                entity.getLanguage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
