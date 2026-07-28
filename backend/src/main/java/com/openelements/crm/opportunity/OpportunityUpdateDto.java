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
 * Request body for a full update of an opportunity. Unlike {@link OpportunityCreateDto}, both
 * {@code status} and {@code ownerId} are required. Validation otherwise mirrors create.
 */
@Schema(description = "Request body for updating an opportunity")
public record OpportunityUpdateDto(
    @NotBlank(message = "Title must not be blank")
    @Size(max = 255)
    @Schema(description = "Title", requiredMode = Schema.RequiredMode.REQUIRED)
    String title,

    @Size(max = 255)
    @Schema(description = "Free-text pipeline stage; any value accepted")
    String stage,

    @NotNull(message = "status must not be null")
    @Schema(description = "Status", requiredMode = Schema.RequiredMode.REQUIRED)
    OpportunityStatus status,

    @Size(max = 255)
    @Schema(description = "Free-text product / offering")
    String product,

    @PositiveOrZero(message = "Estimated value must not be negative")
    @Digits(integer = 10, fraction = 2)
    @Schema(description = "Estimated value in EUR (>= 0)")
    BigDecimal estimatedValue,

    @NotNull(message = "companyId must not be null")
    @Schema(description = "Company ID", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID companyId,

    @NotNull(message = "mainContactId must not be null")
    @Schema(description = "Main contact ID", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID mainContactId,

    @Schema(description = "Additional contact IDs")
    List<UUID> additionalContactIds,

    @NotNull(message = "ownerId must not be null")
    @Schema(description = "Owner user ID", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID ownerId,

    @Schema(description = "Tag IDs to assign")
    List<UUID> tagIds
) {
}
