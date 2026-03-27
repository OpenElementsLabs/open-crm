package com.openelements.crm.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller providing a simple liveness health check endpoint.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Application health check")
public class HealthController {

    /**
     * Returns the health status of the application.
     *
     * @return a {@link HealthDto} with status "UP"
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Check application health",
            description = "Returns the current health status of the backend application"
    )
    @ApiResponse(responseCode = "200", description = "Application is running")
    public HealthDto health() {
        return new HealthDto("UP");
    }
}
