package com.openelements.crm.contact;

import com.openelements.crm.tag.TagEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Contact response")
public record ContactDto(
        @Schema(description = "Contact ID", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(description = "Title (e.g. Dr., Prof.)") String title,
        @Schema(description = "First name", requiredMode = Schema.RequiredMode.REQUIRED) String firstName,
        @Schema(description = "Last name", requiredMode = Schema.RequiredMode.REQUIRED) String lastName,
        @Schema(description = "Email address") String email,
        @Schema(description = "Job position") String position,
        @Schema(description = "Gender") Gender gender,
        @Schema(description = "LinkedIn profile URL") String linkedInUrl,
        @Schema(description = "Phone number") String phoneNumber,
        @Schema(description = "Free-text description") String description,
        @Schema(description = "Company ID") UUID companyId,
        @Schema(description = "Company name") String companyName,
        @Schema(description = "Whether the associated company is archived", requiredMode = Schema.RequiredMode.REQUIRED) boolean companyDeleted,
        @Schema(description = "Number of comments", requiredMode = Schema.RequiredMode.REQUIRED) long commentCount,
        @Schema(description = "Whether the contact has an uploaded photo", requiredMode = Schema.RequiredMode.REQUIRED) boolean hasPhoto,
        @Schema(description = "Birthday") LocalDate birthday,
        @Schema(description = "Whether the contact was imported from Brevo", requiredMode = Schema.RequiredMode.REQUIRED) boolean brevo,
        @Schema(description = "Preferred language (null if unknown)") Language language,
        @Schema(description = "Assigned tag IDs") List<UUID> tagIds,
        @Schema(description = "Creation timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(description = "Last update timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt
) {

    public static ContactDto fromEntity(final ContactEntity entity, final long commentCount) {
        final UUID companyId = entity.getCompany() != null ? entity.getCompany().getId() : null;
        final String companyName = entity.getCompany() != null ? entity.getCompany().getName() : null;
        final boolean companyDeleted = entity.getCompany() != null && entity.getCompany().isDeleted();
        final List<UUID> tagIds = entity.getTags().stream()
                .map(TagEntity::getId)
                .toList();
        return new ContactDto(
                entity.getId(),
                entity.getTitle(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPosition(),
                entity.getGender(),
                entity.getLinkedInUrl(),
                entity.getPhoneNumber(),
                entity.getDescription(),
                companyId,
                companyName,
                companyDeleted,
                commentCount,
                entity.getPhoto() != null,
                entity.getBirthday(),
                entity.getBrevoId() != null,
                entity.getLanguage(),
                tagIds,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
