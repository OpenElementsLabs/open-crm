package com.openelements.crm.enrich;

import com.openelements.crm.company.CompanyDto;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.company.CompanyService;
import com.openelements.crm.contact.ContactDto;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.contact.ContactService;
import com.openelements.crm.contact.SocialLinkDto;
import com.openelements.crm.contact.SocialLinkEntity;
import com.openelements.crm.contact.SocialNetworkType;
import com.openelements.spring.base.data.image.ImageData;
import com.openelements.spring.base.events.OnObjectUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The single contact-domain helper shared by the three concrete enrichment services. It computes the
 * fill-empty diff for a candidate and applies an accepted candidate to a contact.
 *
 * <p>This is deliberately <em>not</em> a provider abstraction: it never talks to an external service
 * and knows nothing about Gravatar/Dropcontact/Cognism. Each concrete client discovers values and
 * hands them here as a {@link RawCandidate}; this class enforces the two invariants that make the
 * feature safe — <strong>only empty fields are ever filled</strong> and existing values are
 * <strong>never</strong> overwritten.
 */
@Component
public class ContactEnrichmentApplier {

    private static final Logger LOG = LoggerFactory.getLogger(ContactEnrichmentApplier.class);

    /** The GDPR Art. 14 reminder shown to the admin after a successful apply. */
    public static final String GDPR_NOTICE =
        "Basierend auf DSGVO-Recht muss die betroffene Person ggf. darüber informiert werden, dass "
            + "Daten aus einer externen Quelle erhoben wurden (Art. 14 DSGVO).";

    private final ContactRepository contactRepository;
    private final ContactService contactService;
    private final CompanyRepository companyRepository;
    private final CompanyService companyService;
    private final ApplicationEventPublisher eventPublisher;

    public ContactEnrichmentApplier(final ContactRepository contactRepository,
                                    final ContactService contactService,
                                    final CompanyRepository companyRepository,
                                    final CompanyService companyService,
                                    final ApplicationEventPublisher eventPublisher) {
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
        this.contactService = Objects.requireNonNull(contactService, "contactService must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.companyService = Objects.requireNonNull(companyService, "companyService must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    /**
     * Turns raw candidates into an authoritative result, computing each candidate's fill-empty
     * changes and company resolution against the contact's current state.
     *
     * @param contactId     the contact being enriched
     * @param rawCandidates the candidates discovered by a concrete client (may be empty)
     * @return {@link EnrichmentResultDto#NO_MATCH} when empty, otherwise a {@code MATCH} result
     * @throws ResponseStatusException 404 if the contact does not exist
     */
    @Transactional(readOnly = true)
    public EnrichmentResultDto buildResult(final UUID contactId, final List<RawCandidate> rawCandidates) {
        Objects.requireNonNull(contactId, "contactId must not be null");
        if (rawCandidates == null || rawCandidates.isEmpty()) {
            return EnrichmentResultDto.NO_MATCH;
        }
        final ContactDto contact = contactService.findById(contactId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
        final List<EnrichmentCandidateDto> candidates = rawCandidates.stream()
            .map(rc -> toCandidate(contact, rc))
            .toList();
        return new EnrichmentResultDto(EnrichmentStatus.MATCH, candidates);
    }

    private EnrichmentCandidateDto toCandidate(final ContactDto contact, final RawCandidate raw) {
        final EnrichmentPayloadDto p = raw.payload();
        final List<EnrichmentChangeDto> changes = new ArrayList<>();
        if (isBlank(contact.email()) && notBlank(p.email())) {
            changes.add(new EnrichmentChangeDto("email", null, p.email().trim()));
        }
        if (isBlank(contact.position()) && notBlank(p.position())) {
            changes.add(new EnrichmentChangeDto("position", null, p.position().trim()));
        }
        if (isBlank(contact.phoneNumber()) && notBlank(p.phoneNumber())) {
            changes.add(new EnrichmentChangeDto("phoneNumber", null, p.phoneNumber().trim()));
        }
        final Set<String> presentNetworks = Optional.ofNullable(contact.socialLinks()).orElse(List.of())
            .stream().map(SocialLinkDto::networkType).collect(Collectors.toSet());
        forEachSocialLink(p.socialLinks(), presentNetworks, (type, resolved) ->
            changes.add(new EnrichmentChangeDto("socialLinks." + type.name(), null, resolved.url())));
        if (!contact.hasPhoto() && notBlank(p.photoBase64())) {
            // proposedValue stays null; the client renders the avatar from payload.photoBase64
            changes.add(new EnrichmentChangeDto("photo", null, null));
        }
        final CompanyResolutionDto resolution =
            resolveCompany(contact.companyId() != null, p.companyName());
        final boolean nothingToEnrich = changes.isEmpty() && resolution.kind() == CompanyResolution.NONE;
        return new EnrichmentCandidateDto(raw.candidateId(), raw.label(), List.copyOf(changes),
            resolution, nothingToEnrich, p);
    }

    private CompanyResolutionDto resolveCompany(final boolean contactHasCompany, final String companyName) {
        if (contactHasCompany || isBlank(companyName)) {
            return CompanyResolutionDto.NONE;
        }
        final String name = companyName.trim();
        return companyRepository.findByNameIgnoreCase(name)
            .map(c -> new CompanyResolutionDto(CompanyResolution.MATCHED, c.getId(), c.getName()))
            .orElseGet(() -> new CompanyResolutionDto(CompanyResolution.NEW, null, name));
    }

    /**
     * Applies an accepted candidate to the contact, re-enforcing the fill-empty rule against the
     * <em>current</em> state (guards against races between search and apply), then publishes an
     * {@link OnObjectUpdate} so audit, search, and webhooks react.
     *
     * @param contactId     the contact to enrich
     * @param payload       the echoed enrichable values
     * @param createCompany whether to create-and-link a new company (only used when resolution is NEW)
     * @return the updated contact
     * @throws ResponseStatusException 404 if the contact does not exist, 400 if the photo cannot be decoded
     */
    @Transactional
    public ContactDto apply(final UUID contactId, final EnrichmentPayloadDto payload, final boolean createCompany) {
        Objects.requireNonNull(contactId, "contactId must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        final ContactEntity entity = contactRepository.findByIdOrThrow(contactId);

        if (isBlank(entity.getEmail()) && notBlank(payload.email())) {
            entity.setEmail(payload.email().trim());
        }
        if (isBlank(entity.getPosition()) && notBlank(payload.position())) {
            entity.setPosition(payload.position().trim());
        }
        if (isBlank(entity.getPhoneNumber()) && notBlank(payload.phoneNumber())) {
            entity.setPhoneNumber(payload.phoneNumber().trim());
        }

        final Set<String> presentNetworks = entity.getSocialLinks().stream()
            .map(link -> link.getNetworkType().name()).collect(Collectors.toCollection(java.util.HashSet::new));
        forEachSocialLink(payload.socialLinks(), presentNetworks, (type, resolved) -> {
            final SocialLinkEntity link = new SocialLinkEntity();
            link.setNetworkType(type);
            link.setValue(resolved.value());
            link.setUrl(resolved.url());
            entity.getSocialLinks().add(link);
            presentNetworks.add(type.name());
        });

        applyCompany(entity, payload.companyName(), createCompany);
        applyPhoto(entity, payload);

        contactRepository.saveAndFlush(entity);
        final ContactDto dto = contactService.findById(contactId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
        eventPublisher.publishEvent(new OnObjectUpdate<>(dto));
        return dto;
    }

    private void applyCompany(final ContactEntity entity, final String companyName, final boolean createCompany) {
        if (entity.getCompany() != null || isBlank(companyName)) {
            return;
        }
        final String name = companyName.trim();
        final Optional<CompanyEntity> existing = companyRepository.findByNameIgnoreCase(name);
        if (existing.isPresent()) {
            entity.setCompany(existing.get());
        } else if (createCompany) {
            final CompanyDto created = companyService.save(new CompanyDto(
                null, name, null, null, null, null, null, null, null, null,
                null, null, null, null, null, false, false, 0L, 0L, List.of(), null, null));
            entity.setCompany(companyRepository.findByIdOrThrow(created.id()));
        }
    }

    private void applyPhoto(final ContactEntity entity, final EnrichmentPayloadDto payload) {
        if (entity.getPhoto() != null || isBlank(payload.photoBase64())) {
            return;
        }
        final byte[] raw;
        try {
            raw = Base64.getDecoder().decode(payload.photoBase64());
        } catch (final IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid photo data");
        }
        final String contentType = isBlank(payload.photoContentType()) ? "image/jpeg" : payload.photoContentType();
        try {
            entity.setPhoto(ImageData.of(raw, contentType).asJpeg().data());
            entity.setPhotoContentType("image/jpeg");
        } catch (final ResponseStatusException e) {
            throw e;
        } catch (final RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not decode avatar image", e);
        }
    }

    /**
     * Invokes {@code consumer} for each social link whose network is not already present on the
     * contact and whose value resolves to a valid link. Unknown networks and unresolvable values are
     * skipped (logged), never fatal — a single bad social link must not abort the whole enrichment.
     */
    private void forEachSocialLink(final Map<String, String> socialLinks,
                                   final Set<String> presentNetworks,
                                   final ResolvedLinkConsumer consumer) {
        if (socialLinks == null) {
            return;
        }
        socialLinks.forEach((network, value) -> {
            if (isBlank(value) || presentNetworks.contains(network)) {
                return;
            }
            final SocialNetworkType type;
            try {
                type = SocialNetworkType.valueOf(network);
            } catch (final IllegalArgumentException e) {
                LOG.debug("Skipping unknown social network '{}' from enrichment", network);
                return;
            }
            try {
                consumer.accept(type, type.resolve(value));
            } catch (final RuntimeException e) {
                LOG.debug("Skipping unresolvable {} link from enrichment: {}", network, e.getMessage());
            }
        });
    }

    @FunctionalInterface
    private interface ResolvedLinkConsumer {
        void accept(SocialNetworkType type, SocialNetworkType.ResolvedLink resolved);
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private static boolean notBlank(final String value) {
        return value != null && !value.isBlank();
    }
}
