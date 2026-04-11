package com.openelements.crm.company;

import com.openelements.crm.tag.TagEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Company")
public record CompanyDto(
        @Schema(description = "Company ID", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(description = "Company name", requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(description = "Global email address") String email,
        @Schema(description = "Website URL") String website,
        @Schema(description = "Street") String street,
        @Schema(description = "House number") String houseNumber,
        @Schema(description = "Zip code") String zipCode,
        @Schema(description = "City") String city,
        @Schema(description = "Country") String country,
        @Schema(description = "Phone number") String phoneNumber,
        @Schema(description = "Free-text description") String description,
        @Schema(description = "Bank name") String bankName,
        @Schema(description = "Bank Identifier Code (BIC)") String bic,
        @Schema(description = "International Bank Account Number (IBAN)") String iban,
        @Schema(description = "VAT identification number") String vatId,
        @Schema(description = "Whether the company has an uploaded logo", requiredMode = Schema.RequiredMode.REQUIRED) boolean hasLogo,
        @Schema(description = "Whether the company was imported from Brevo", requiredMode = Schema.RequiredMode.REQUIRED) boolean brevo,
        @Schema(description = "Number of associated contacts", requiredMode = Schema.RequiredMode.REQUIRED) long contactCount,
        @Schema(description = "Number of comments", requiredMode = Schema.RequiredMode.REQUIRED) long commentCount,
        @Schema(description = "Assigned tag IDs") List<UUID> tagIds,
        @Schema(description = "Creation timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(description = "Last update timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt
) {

    public static CompanyDto fromEntity(final CompanyEntity entity,
                                        final long contactCount,
                                        final long commentCount) {
        final List<UUID> tagIds = entity.getTags().stream()
                .map(TagEntity::getId)
                .toList();
        return new CompanyDto(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getWebsite(),
                entity.getStreet(),
                entity.getHouseNumber(),
                entity.getZipCode(),
                entity.getCity(),
                entity.getCountry(),
                entity.getPhoneNumber(),
                entity.getDescription(),
                entity.getBankName(),
                entity.getBic(),
                entity.getIban(),
                entity.getVatId(),
                entity.getLogo() != null,
                entity.getBrevoCompanyId() != null,
                contactCount,
                commentCount,
                tagIds,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
