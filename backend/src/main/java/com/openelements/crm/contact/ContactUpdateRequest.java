package com.openelements.crm.contact;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request DTO for updating an existing contact. Does not include Brevo-managed fields.
 *
 * @param firstName   the first name (required)
 * @param lastName    the last name (required)
 * @param email       the email address
 * @param position    the job position
 * @param gender      the gender (nullable — null means unknown)
 * @param linkedInUrl the LinkedIn profile URL
 * @param phoneNumber the phone number
 * @param companyId   the company ID (optional)
 * @param language    the preferred language (required)
 */
@Schema(description = "Request body for updating an existing contact")
public record ContactUpdateRequest(
        @NotBlank(message = "First name must not be blank")
        @Size(max = 255)
        @Schema(description = "First name", example = "Hendrik")
        String firstName,

        @NotBlank(message = "Last name must not be blank")
        @Size(max = 255)
        @Schema(description = "Last name", example = "Ebbers")
        String lastName,

        @Size(max = 255)
        @Schema(description = "Email address", example = "hendrik@open-elements.com")
        String email,

        @Size(max = 255)
        @Schema(description = "Job position", example = "CEO")
        String position,

        @Schema(description = "Gender (null if unknown)", example = "MALE")
        Gender gender,

        @Size(max = 500)
        @Schema(description = "LinkedIn profile URL", example = "https://linkedin.com/in/hendrik-ebbers")
        String linkedInUrl,

        @Size(max = 50)
        @Schema(description = "Phone number", example = "+49 123 456789")
        String phoneNumber,

        @Schema(description = "Company ID (optional)")
        UUID companyId,

        @NotNull(message = "Language must not be null")
        @Schema(description = "Preferred language", example = "DE")
        Language language
) {
}
