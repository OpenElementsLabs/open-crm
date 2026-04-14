package com.openelements.crm.company;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * DTO for creating a new company.
 *
 * @param name        the company name (required)
 * @param email       the global email address
 * @param website     the website URL
 * @param street      the street of the address
 * @param houseNumber the house number of the address
 * @param zipCode     the zip code of the address
 * @param city        the city of the address
 * @param country     the country of the address
 * @param description the free-text description (optional)
 */
@Schema(description = "Request body for creating a new company")
public record CompanyDataDto(
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
    String country,

    @Size(max = 255)
    @Schema(description = "Phone number", example = "+49 30 12345678")
    String phoneNumber,

    @Schema(description = "Free-text description")
    String description,

    @Size(max = 255)
    @Schema(description = "Bank name", example = "Deutsche Bank")
    String bankName,

    @Size(max = 11)
    @Schema(description = "Bank Identifier Code (BIC)", example = "DEUTDEFF")
    String bic,

    @Size(max = 34)
    @Schema(description = "International Bank Account Number (IBAN)", example = "DE89370400440532013000")
    String iban,

    @Size(max = 20)
    @Schema(description = "VAT identification number", example = "DE123456789")
    String vatId,

    @Schema(description = "Tag IDs to assign")
    List<UUID> tagIds
) {
}
