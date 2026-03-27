package com.openelements.crm.company;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO representing a company.
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
 * @param deleted     whether the company is soft-deleted
 * @param createdAt   the creation timestamp
 * @param updatedAt   the last update timestamp
 */
@Schema(description = "Company response")
public record CompanyResponse(
        @Schema(description = "Company ID") UUID id,
        @Schema(description = "Company name") String name,
        @Schema(description = "Global email address") String email,
        @Schema(description = "Website URL") String website,
        @Schema(description = "Street") String street,
        @Schema(description = "House number") String houseNumber,
        @Schema(description = "Zip code") String zipCode,
        @Schema(description = "City") String city,
        @Schema(description = "Country") String country,
        @Schema(description = "Whether the company is soft-deleted") boolean deleted,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Last update timestamp") Instant updatedAt
) {

    /**
     * Creates a response DTO from a company entity.
     *
     * @param entity the company entity
     * @return the response DTO
     */
    public static CompanyResponse fromEntity(final CompanyEntity entity) {
        return new CompanyResponse(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getWebsite(),
                entity.getStreet(),
                entity.getHouseNumber(),
                entity.getZipCode(),
                entity.getCity(),
                entity.getCountry(),
                entity.isDeleted(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
