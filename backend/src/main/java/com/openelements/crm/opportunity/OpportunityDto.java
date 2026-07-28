package com.openelements.crm.opportunity;

import com.openelements.spring.base.data.NameSupplier;
import com.openelements.spring.base.data.WithId;
import com.openelements.spring.base.services.user.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read representation of a sales opportunity. {@code companyName} and {@code mainContactName} are
 * denormalized for display; {@code owner} is a nested {@link UserDto} (same convention as
 * {@code CommentDto.author}).
 */
@Schema(description = "Sales opportunity (deal)")
public record OpportunityDto(
    @Schema(description = "Opportunity ID", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
    @Schema(description = "Title", requiredMode = Schema.RequiredMode.REQUIRED) String title,
    @Schema(description = "Free-text pipeline stage (Kanban-ready); any value accepted") String stage,
    @Schema(description = "Status", requiredMode = Schema.RequiredMode.REQUIRED) OpportunityStatus status,
    @Schema(description = "Free-text product / offering") String product,
    @Schema(description = "Estimated value in EUR (>= 0), or null for no forecast") BigDecimal estimatedValue,
    @Schema(description = "Company ID", requiredMode = Schema.RequiredMode.REQUIRED) UUID companyId,
    @Schema(description = "Company name (denormalized)") String companyName,
    @Schema(description = "Main contact ID", requiredMode = Schema.RequiredMode.REQUIRED) UUID mainContactId,
    @Schema(description = "Main contact display name (denormalized)") String mainContactName,
    @Schema(description = "Additional contact IDs") List<UUID> additionalContactIds,
    @Schema(description = "Responsible owner", requiredMode = Schema.RequiredMode.REQUIRED) UserDto owner,
    @Schema(description = "Assigned tag IDs") List<UUID> tagIds,
    @Schema(description = "Number of comments", requiredMode = Schema.RequiredMode.REQUIRED) long commentCount,
    @Schema(description = "Creation timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
    @Schema(description = "Last update timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt
) implements WithId {

    /**
     * Display name used by the spring-services audit log and updates feed ({@code @NameSupplier}).
     *
     * @return the opportunity title, or an empty string if absent
     */
    @NameSupplier
    public String displayName() {
        return title == null ? "" : title;
    }
}
