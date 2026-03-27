package com.openelements.crm.health;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for the health endpoint.
 *
 * @param status the health status of the application (e.g. "UP")
 */
@Schema(description = "Health status response")
public record HealthResponse(
        @Schema(description = "Health status indicator", example = "UP")
        String status
) {
}
