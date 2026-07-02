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
 * Concrete Gravatar enrichment service. Gravatar matches only on email, so a contact without an
 * email yields {@code NO_MATCH} (the frontend also hides the entry in that case).
 */
@Service
public class GravatarEnrichmentService {

    private final ContactService contactService;
    private final GravatarClient gravatarClient;
    private final ContactEnrichmentApplier applier;

    public GravatarEnrichmentService(final ContactService contactService,
                                     final GravatarClient gravatarClient,
                                     final ContactEnrichmentApplier applier) {
        this.contactService = Objects.requireNonNull(contactService, "contactService must not be null");
        this.gravatarClient = Objects.requireNonNull(gravatarClient, "gravatarClient must not be null");
        this.applier = Objects.requireNonNull(applier, "applier must not be null");
    }

    /**
     * Searches Gravatar for the contact's email.
     *
     * @param contactId the contact
     * @return the enrichment result ({@code NO_MATCH} when the contact has no email or no avatar/profile)
     * @throws ResponseStatusException 404 if the contact does not exist
     */
    public EnrichmentResultDto search(final UUID contactId) {
        final ContactDto contact = contactService.findById(contactId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
        if (contact.email() == null || contact.email().isBlank()) {
            return EnrichmentResultDto.NO_MATCH;
        }
        final List<RawCandidate> candidates = gravatarClient.lookupByEmail(contact.email())
            .map(List::of).orElseGet(List::of);
        return applier.buildResult(contactId, candidates);
    }

    /**
     * Applies an accepted Gravatar candidate.
     *
     * @param contactId the contact
     * @param request   the echoed payload and create-company flag
     * @return the updated contact
     */
    public ContactDto apply(final UUID contactId, final EnrichmentApplyDto request) {
        return applier.apply(contactId, request.payload(), request.createCompany());
    }
}
