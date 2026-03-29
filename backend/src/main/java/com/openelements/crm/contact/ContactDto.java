package com.openelements.crm.contact;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
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
 * @param companyName    the company name (resolved)
 * @param companyDeleted whether the associated company is soft-deleted
 * @param commentCount   the number of comments on this contact
 * @param hasPhoto       whether the contact has an uploaded photo
 * @param birthday       the birthday (optional)
 * @param syncedToBrevo  whether synced to Brevo
 * @param language      the preferred language
 * @param createdAt     the creation timestamp
 * @param updatedAt     the last update timestamp
 */
@Schema(description = "Contact response")
public record ContactDto(
        @Schema(description = "Contact ID", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(description = "First name", requiredMode = Schema.RequiredMode.REQUIRED) String firstName,
        @Schema(description = "Last name", requiredMode = Schema.RequiredMode.REQUIRED) String lastName,
        @Schema(description = "Email address") String email,
        @Schema(description = "Job position") String position,
        @Schema(description = "Gender") Gender gender,
        @Schema(description = "LinkedIn profile URL") String linkedInUrl,
        @Schema(description = "Phone number") String phoneNumber,
        @Schema(description = "Company ID") UUID companyId,
        @Schema(description = "Company name") String companyName,
        @Schema(description = "Whether the associated company is archived", requiredMode = Schema.RequiredMode.REQUIRED) boolean companyDeleted,
        @Schema(description = "Number of comments", requiredMode = Schema.RequiredMode.REQUIRED) long commentCount,
        @Schema(description = "Whether the contact has an uploaded photo", requiredMode = Schema.RequiredMode.REQUIRED) boolean hasPhoto,
        @Schema(description = "Birthday") LocalDate birthday,
        @Schema(description = "Whether synced to Brevo", requiredMode = Schema.RequiredMode.REQUIRED) boolean syncedToBrevo,
        @Schema(description = "Preferred language (null if unknown)") Language language,
        @Schema(description = "Creation timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(description = "Last update timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt
) {

    /**
     * Creates a response DTO from a contact entity with comment count.
     *
     * @param entity       the contact entity
     * @param commentCount the number of comments
     * @return the response DTO
     */
    public static ContactDto fromEntity(final ContactEntity entity, final long commentCount) {
        final UUID companyId = entity.getCompany() != null ? entity.getCompany().getId() : null;
        final String companyName = entity.getCompany() != null ? entity.getCompany().getName() : null;
        final boolean companyDeleted = entity.getCompany() != null && entity.getCompany().isDeleted();
        return new ContactDto(
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
                companyDeleted,
                commentCount,
                entity.getPhoto() != null,
                entity.getBirthday(),
                entity.isSyncedToBrevo(),
                entity.getLanguage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
