package com.openelements.crm.enrich;

import com.openelements.spring.base.security.roles.RequiresItAdmin;
import com.openelements.spring.base.services.settings.SettingsDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * Cognism settings (IT-admin only) and enrichment actions (app-or-it-admin). Cognism ships
 * configured-but-inactive — no key is present by default, so the menu entry stays hidden until an
 * IT administrator stores a key.
 *
 * <p>As with Dropcontact, the settings <em>status</em> ({@code GET}) is readable by any admin for
 * menu gating; only key management ({@code PUT}/{@code DELETE}) is IT-admin only.
 */
@RestController
@Tag(name = "Contact Enrichment", description = "Cognism settings and enrichment")
@SecurityRequirement(name = "oidc")
public class CognismEnrichmentController {

    private final SettingsDataService settingsService;
    private final CognismClient cognismClient;
    private final CognismEnrichmentService service;

    public CognismEnrichmentController(final SettingsDataService settingsService,
                                       final CognismClient cognismClient,
                                       final CognismEnrichmentService service) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService must not be null");
        this.cognismClient = Objects.requireNonNull(cognismClient, "cognismClient must not be null");
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    /**
     * Reports whether a Cognism key is configured. Readable by any admin (menu gating); never returns
     * the key itself.
     *
     * @return the configuration status
     */
    @GetMapping(value = "/api/cognism/settings", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('APP-ADMIN') or hasRole('IT-ADMIN')")
    @Operation(summary = "Get Cognism settings status")
    public EnrichmentSettingsDto getSettings() {
        return new EnrichmentSettingsDto(cognismClient.isConfigured());
    }

    /**
     * Validates and stores the Cognism API key.
     *
     * @param request the new API key
     * @return the configuration status
     */
    @PutMapping(value = "/api/cognism/settings", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @RequiresItAdmin
    @Operation(summary = "Update Cognism API key")
    @ApiResponse(responseCode = "200", description = "API key updated")
    @ApiResponse(responseCode = "400", description = "Invalid API key")
    public EnrichmentSettingsDto updateSettings(@Valid @RequestBody final EnrichmentSettingsUpdateDto request) {
        cognismClient.validateApiKey(request.apiKey());
        settingsService.set(CognismClient.API_KEY, request.apiKey());
        return new EnrichmentSettingsDto(true);
    }

    /**
     * Removes the stored Cognism API key.
     */
    @DeleteMapping("/api/cognism/settings")
    @RequiresItAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove Cognism API key")
    @ApiResponse(responseCode = "204", description = "API key removed")
    public void deleteSettings() {
        settingsService.delete(CognismClient.API_KEY);
    }

    /**
     * Searches Cognism for the contact and returns the server-computed preview.
     *
     * @param id the contact id
     * @return the enrichment result
     */
    @PostMapping(value = "/api/contacts/{id}/enrich/cognism/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('APP-ADMIN') or hasRole('IT-ADMIN')")
    @Operation(summary = "Search Cognism for a contact")
    @ApiResponse(responseCode = "200", description = "Search completed")
    @ApiResponse(responseCode = "503", description = "Cognism not configured")
    public EnrichmentResultDto search(@PathVariable("id") final UUID id) {
        return service.search(id);
    }

    /**
     * Applies an accepted Cognism candidate to the contact.
     *
     * @param id      the contact id
     * @param request the echoed payload and create-company flag
     * @return the updated contact and a GDPR reminder
     */
    @PostMapping(value = "/api/contacts/{id}/enrich/cognism/apply", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('APP-ADMIN') or hasRole('IT-ADMIN')")
    @Operation(summary = "Apply a Cognism candidate")
    @ApiResponse(responseCode = "200", description = "Enrichment applied")
    public EnrichmentApplyResultDto apply(@PathVariable("id") final UUID id,
                                          @Valid @RequestBody final EnrichmentApplyDto request) {
        return new EnrichmentApplyResultDto(service.apply(id, request), ContactEnrichmentApplier.GDPR_NOTICE);
    }
}
