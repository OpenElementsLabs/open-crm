package com.openelements.crm.company;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a company.
 *
 * @param id          the company ID
 * @param name        the company name
 * @param email       the global email address
 * @param website     the website URL
 * @param street      the street of the address
 * @param houseNumber the house number of the address
 * @param zipCode     the zip code of the address
 * @param city        the city of the address
 * @param country     the country of the address
 * @param deleted      whether the company is soft-deleted
 * @param hasLogo      whether the company has an uploaded logo
 * @param brevo        whether the company was imported from Brevo
 * @param contactCount the number of associated contacts
 * @param commentCount the number of comments on this company
 * @param createdAt    the creation timestamp
 * @param updatedAt    the last update timestamp
 */
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
        @Schema(description = "Whether the company is soft-deleted", requiredMode = Schema.RequiredMode.REQUIRED) boolean deleted,
        @Schema(description = "Whether the company has an uploaded logo", requiredMode = Schema.RequiredMode.REQUIRED) boolean hasLogo,
        @Schema(description = "Whether the company was imported from Brevo", requiredMode = Schema.RequiredMode.REQUIRED) boolean brevo,
        @Schema(description = "Number of associated contacts", requiredMode = Schema.RequiredMode.REQUIRED) long contactCount,
        @Schema(description = "Number of comments", requiredMode = Schema.RequiredMode.REQUIRED) long commentCount,
        @Schema(description = "Creation timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(description = "Last update timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt
) {

    /**
     * Creates a DTO from a company entity with counts.
     *
     * @param entity       the company entity
     * @param contactCount the number of associated contacts
     * @param commentCount the number of comments
     * @return the DTO
     */
    public static CompanyDto fromEntity(final CompanyEntity entity,
                                        final long contactCount,
                                        final long commentCount) {
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
                entity.isDeleted(),
                entity.getLogo() != null,
                entity.getBrevoCompanyId() != null,
                contactCount,
                commentCount,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
