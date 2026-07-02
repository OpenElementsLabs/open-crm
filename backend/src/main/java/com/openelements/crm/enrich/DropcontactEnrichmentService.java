package com.openelements.crm.enrich;

import com.openelements.crm.contact.ContactDto;
import com.openelements.crm.contact.ContactService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Concrete Dropcontact enrichment service. Matches by email when the contact has one, otherwise by
 * name + company.
 */
@Service
public class DropcontactEnrichmentService {

    private final ContactService contactService;
    private final DropcontactClient dropcontactClient;
    private final ContactEnrichmentApplier applier;

    public DropcontactEnrichmentService(final ContactService contactService,
                                        final DropcontactClient dropcontactClient,
                                        final ContactEnrichmentApplier applier) {
        this.contactService = Objects.requireNonNull(contactService, "contactService must not be null");
        this.dropcontactClient = Objects.requireNonNull(dropcontactClient, "dropcontactClient must not be null");
        this.applier = Objects.requireNonNull(applier, "applier must not be null");
    }

    /**
     * Searches Dropcontact, preferring email over name+company.
     *
     * @param contactId the contact
     * @return the enrichment result
     * @throws ResponseStatusException 404 if the contact does not exist, 503 if unconfigured,
     *                                 502 on downstream failure
     */
    public EnrichmentResultDto search(final UUID contactId) {
        final ContactDto contact = contactService.findById(contactId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
        final List<RawCandidate> candidates;
        if (contact.email() != null && !contact.email().isBlank()) {
            candidates = dropcontactClient.search(contact.email(), null, null, null);
        } else {
            candidates = dropcontactClient.search(null, contact.firstName(), contact.lastName(), contact.companyName());
        }
        return applier.buildResult(contactId, candidates);
    }

    /**
     * Applies an accepted Dropcontact candidate.
     *
     * @param contactId the contact
     * @param request   the echoed payload and create-company flag
     * @return the updated contact
     */
    public ContactDto apply(final UUID contactId, final EnrichmentApplyDto request) {
        return applier.apply(contactId, request.payload(), request.createCompany());
    }
}
