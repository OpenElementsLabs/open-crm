package com.openelements.crm.company;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for updating an existing company.
 *
 * @param name        the company name (required)
 * @param email       the global email address
 * @param website     the website URL
 * @param street      the street of the address
 * @param houseNumber the house number of the address
 * @param zipCode     the zip code of the address
 * @param city        the city of the address
 * @param country     the country of the address
 */
@Schema(description = "Request body for updating an existing company")
public record CompanyUpdateDto(
        @NotBlank(message = "Name must not be blank")
        @Size(max = 255)
        @Schema(description = "Company name", example = "Open Elements GmbH", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Size(max = 255)
        @Schema(description = "Global email address", example = "info@open-elements.com")
        String email,

        @Size(max = 500)
        @Schema(description = "Website URL", example = "https://open-elements.com")
        String website,

        @Size(max = 255)
        @Schema(description = "Street", example = "Musterstraße")
        String street,

        @Size(max = 20)
        @Schema(description = "House number", example = "42")
        String houseNumber,

        @Size(max = 20)
        @Schema(description = "Zip code", example = "12345")
        String zipCode,

        @Size(max = 255)
        @Schema(description = "City", example = "Berlin")
        String city,

        @Size(max = 100)
        @Schema(description = "Country", example = "Germany")
        String country
) {
}
