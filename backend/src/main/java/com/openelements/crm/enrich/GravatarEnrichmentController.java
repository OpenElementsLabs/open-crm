package com.openelements.crm.enrich;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * Enrichment actions backed by Gravatar. Gravatar is keyless and always available, so there are no
 * settings endpoints. Both actions require an application or IT administrator.
 */
@RestController
@RequestMapping("/api/contacts/{id}/enrich/gravatar")
@Tag(name = "Contact Enrichment", description = "Enrich a contact from Gravatar")
@SecurityRequirement(name = "oidc")
public class GravatarEnrichmentController {

    private final GravatarEnrichmentService service;

    public GravatarEnrichmentController(final GravatarEnrichmentService service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    /**
     * Searches Gravatar for the contact and returns the server-computed preview.
     *
     * @param id the contact id
     * @return the enrichment result
     */
    @PostMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('APP-ADMIN') or hasRole('IT-ADMIN')")
    @Operation(summary = "Search Gravatar for a contact")
    @ApiResponse(responseCode = "200", description = "Search completed")
    public EnrichmentResultDto search(@PathVariable("id") final UUID id) {
        return service.search(id);
    }

    /**
     * Applies an accepted Gravatar candidate to the contact.
     *
     * @param id      the contact id
     * @param request the echoed payload and create-company flag
     * @return the updated contact and a GDPR reminder
     */
    @PostMapping(value = "/apply", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('APP-ADMIN') or hasRole('IT-ADMIN')")
    @Operation(summary = "Apply a Gravatar candidate")
    @ApiResponse(responseCode = "200", description = "Enrichment applied")
    public EnrichmentApplyResultDto apply(@PathVariable("id") final UUID id,
                                          @Valid @RequestBody final EnrichmentApplyDto request) {
        return new EnrichmentApplyResultDto(service.apply(id, request), ContactEnrichmentApplier.GDPR_NOTICE);
    }
}
