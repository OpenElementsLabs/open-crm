package com.openelements.crm.webhook;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for webhook management.
 */
@RestController
@RequestMapping("/api/webhooks")
@Tag(name = "Webhooks", description = "Webhook management operations")
@SecurityRequirement(name = "oidc")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(final WebhookService webhookService) {
        this.webhookService = Objects.requireNonNull(webhookService, "webhookService must not be null");
    }

    /**
     * Lists webhooks with pagination.
     *
     * @param pageable pagination parameters
     * @return a page of webhook responses
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List webhooks", description = "Returns a paginated list of registered webhooks")
    public Page<WebhookDto> list(
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "createdAt") final Pageable pageable) {
        return webhookService.list(pageable);
    }

    /**
     * Returns a webhook by its ID.
     *
     * @param id the webhook ID
     * @return the webhook response
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get webhook by ID")
    @ApiResponse(responseCode = "200", description = "Webhook found")
    @ApiResponse(responseCode = "404", description = "Webhook not found")
    public WebhookDto getById(@Parameter(description = "The webhook ID") @PathVariable final UUID id) {
        return webhookService.getById(id);
    }

    /**
     * Creates a new webhook.
     *
     * @param request the create request
     * @return the created webhook response
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new webhook")
    @ApiResponse(responseCode = "201", description = "Webhook created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public WebhookDto create(@Valid @RequestBody final WebhookCreateDto request) {
        return webhookService.create(request);
    }

    /**
     * Updates an existing webhook.
     *
     * @param id      the webhook ID
     * @param request the update request
     * @return the updated webhook response
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a webhook")
    @ApiResponse(responseCode = "200", description = "Webhook updated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Webhook not found")
    public WebhookDto update(@Parameter(description = "The webhook ID") @PathVariable final UUID id,
                             @Valid @RequestBody final WebhookUpdateDto request) {
        return webhookService.update(id, request);
    }

    /**
     * Triggers an asynchronous PING call to the specified webhook.
     *
     * @param id the webhook ID
     */
    @PostMapping("/{id}/ping")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Ping a webhook", description = "Sends an asynchronous PING to the webhook URL. Result visible via GET.")
    @ApiResponse(responseCode = "202", description = "PING triggered")
    @ApiResponse(responseCode = "404", description = "Webhook not found")
    public void ping(@Parameter(description = "The webhook ID") @PathVariable final UUID id) {
        webhookService.ping(id);
    }

    /**
     * Deletes a webhook.
     *
     * @param id the webhook ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a webhook")
    @ApiResponse(responseCode = "204", description = "Webhook deleted")
    @ApiResponse(responseCode = "404", description = "Webhook not found")
    public void delete(@Parameter(description = "The webhook ID") @PathVariable final UUID id) {
        webhookService.delete(id);
    }
}
