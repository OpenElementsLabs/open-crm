package com.openelements.crm.opportunity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request body for creating an opportunity. {@code status} defaults to {@code OPEN} when omitted;
 * {@code ownerId} defaults to the current user when omitted. {@code stage} and {@code product}
 * accept any string. Unknown referenced IDs and a main contact listed as an additional contact are
 * rejected at the service level with {@code 400 Bad Request}.
 */
@Schema(description = "Request body for creating an opportunity")
public record OpportunityCreateDto(
    @NotBlank(message = "Title must not be blank")
    @Size(max = 255)
    @Schema(description = "Title", example = "Muster-Bank – CRA Support & Care", requiredMode = Schema.RequiredMode.REQUIRED)
    String title,

    @Size(max = 255)
    @Schema(description = "Free-text pipeline stage; any value accepted", example = "Angebot")
    String stage,

    @Schema(description = "Status; defaults to OPEN when omitted", example = "OPEN")
    OpportunityStatus status,

    @Size(max = 255)
    @Schema(description = "Free-text product / offering", example = "Support & Care")
    String product,

    @PositiveOrZero(message = "Estimated value must not be negative")
    @Digits(integer = 10, fraction = 2)
    @Schema(description = "Estimated value in EUR (>= 0)", example = "25000.00")
    BigDecimal estimatedValue,

    @NotNull(message = "companyId must not be null")
    @Schema(description = "Company ID", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID companyId,

    @NotNull(message = "mainContactId must not be null")
    @Schema(description = "Main contact ID", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID mainContactId,

    @Schema(description = "Additional contact IDs")
    List<UUID> additionalContactIds,

    @Schema(description = "Owner user ID; defaults to the current user when omitted")
    UUID ownerId,

    @Schema(description = "Tag IDs to assign")
    List<UUID> tagIds
) {
}
