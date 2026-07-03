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
 * Dropcontact settings (IT-admin only) and enrichment actions (app-or-it-admin).
 *
 * <p>The settings <em>status</em> ({@code GET}) is readable by any admin who can trigger enrichment,
 * because the frontend uses it to gate the menu entry; only <em>managing</em> the key
 * ({@code PUT}/{@code DELETE}) is restricted to IT administrators.
 */
@RestController
@Tag(name = "Contact Enrichment", description = "Dropcontact settings and enrichment")
@SecurityRequirement(name = "oidc")
public class DropcontactEnrichmentController {

    private final SettingsDataService settingsService;
    private final DropcontactClient dropcontactClient;
    private final DropcontactEnrichmentService service;

    public DropcontactEnrichmentController(final SettingsDataService settingsService,
                                           final DropcontactClient dropcontactClient,
                                           final DropcontactEnrichmentService service) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService must not be null");
        this.dropcontactClient = Objects.requireNonNull(dropcontactClient, "dropcontactClient must not be null");
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    /**
     * Reports whether a Dropcontact key is configured. Readable by any admin (menu gating); never
     * returns the key itself.
     *
     * @return the configuration status
     */
    @GetMapping(value = "/api/dropcontact/settings", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('APP-ADMIN') or hasRole('IT-ADMIN')")
    @Operation(summary = "Get Dropcontact settings status")
    public EnrichmentSettingsDto getSettings() {
        return new EnrichmentSettingsDto(dropcontactClient.isConfigured());
    }

    /**
     * Validates and stores the Dropcontact API key.
     *
     * @param request the new API key
     * @return the configuration status
     */
    @PutMapping(value = "/api/dropcontact/settings", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @RequiresItAdmin
    @Operation(summary = "Update Dropcontact API key")
    @ApiResponse(responseCode = "200", description = "API key updated")
    @ApiResponse(responseCode = "400", description = "Invalid API key")
    public EnrichmentSettingsDto updateSettings(@Valid @RequestBody final EnrichmentSettingsUpdateDto request) {
        dropcontactClient.validateApiKey(request.apiKey());
        settingsService.set(DropcontactClient.API_KEY, request.apiKey());
        return new EnrichmentSettingsDto(true);
    }

    /**
     * Removes the stored Dropcontact API key.
     */
    @DeleteMapping("/api/dropcontact/settings")
    @RequiresItAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove Dropcontact API key")
    @ApiResponse(responseCode = "204", description = "API key removed")
    public void deleteSettings() {
        settingsService.delete(DropcontactClient.API_KEY);
    }

    /**
     * Searches Dropcontact for the contact and returns the server-computed preview.
     *
     * @param id the contact id
     * @return the enrichment result
     */
    @PostMapping(value = "/api/contacts/{id}/enrich/dropcontact/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('APP-ADMIN') or hasRole('IT-ADMIN')")
    @Operation(summary = "Search Dropcontact for a contact")
    @ApiResponse(responseCode = "200", description = "Search completed")
    @ApiResponse(responseCode = "503", description = "Dropcontact not configured")
    public EnrichmentResultDto search(@PathVariable("id") final UUID id) {
        return service.search(id);
    }

    /**
     * Applies an accepted Dropcontact candidate to the contact.
     *
     * @param id      the contact id
     * @param request the echoed payload and create-company flag
     * @return the updated contact and a GDPR reminder
     */
    @PostMapping(value = "/api/contacts/{id}/enrich/dropcontact/apply", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('APP-ADMIN') or hasRole('IT-ADMIN')")
    @Operation(summary = "Apply a Dropcontact candidate")
    @ApiResponse(responseCode = "200", description = "Enrichment applied")
    public EnrichmentApplyResultDto apply(@PathVariable("id") final UUID id,
                                          @Valid @RequestBody final EnrichmentApplyDto request) {
        return new EnrichmentApplyResultDto(service.apply(id, request), ContactEnrichmentApplier.GDPR_NOTICE);
    }
}
