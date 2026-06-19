package com.openelements.crm.brevo;

import com.openelements.crm.company.CompanyDto;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.company.CompanyService;
import com.openelements.crm.contact.ContactDto;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.contact.ContactService;
import com.openelements.crm.contact.Language;
import com.openelements.crm.contact.SocialLinkEntity;
import com.openelements.crm.contact.SocialNetworkType;
import com.openelements.spring.base.events.OnObjectCreate;
import com.openelements.spring.base.events.OnObjectUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service that synchronizes companies and contacts from Brevo into the CRM database.
 */
@Service
public class BrevoSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(BrevoSyncService.class);
    private static final int COMPANY_PROGRESS_INTERVAL = 25;
    private static final int CONTACT_PROGRESS_INTERVAL = 25;

    private final BrevoApiClient brevoApiClient;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final CompanyService companyService;
    private final ContactService contactService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

    /**
     * Creates a new BrevoSyncService.
     *
     * @param brevoApiClient      the Brevo API client
     * @param companyRepository   the company repository (used for Brevo-specific finders)
     * @param contactRepository   the contact repository (used for Brevo-specific finders)
     * @param companyService      used to fetch CompanyDto after each save so the same events
     *                            an ordinary {@code save()} would emit can be published
     * @param contactService      same role as {@code companyService}, for contacts
     * @param eventPublisher      publishes OnObjectCreate / OnObjectUpdate so audit, search
     *                            and webhook listeners pick the sync up automatically
     * @param transactionTemplate the transaction template for per-entity transactions
     */
    public BrevoSyncService(final BrevoApiClient brevoApiClient,
                            final CompanyRepository companyRepository,
                            final ContactRepository contactRepository,
                            final CompanyService companyService,
                            final ContactService contactService,
                            final ApplicationEventPublisher eventPublisher,
                            final TransactionTemplate transactionTemplate) {
        this.brevoApiClient = Objects.requireNonNull(brevoApiClient, "brevoApiClient must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
        this.companyService = Objects.requireNonNull(companyService, "companyService must not be null");
        this.contactService = Objects.requireNonNull(contactService, "contactService must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate must not be null");
    }

    /**
     * Runs a full synchronization of companies and contacts from Brevo.
     *
     * @return a summary of the sync results
     * @throws ResponseStatusException with status 409 if a sync is already in progress
     */
    public BrevoSyncResultDto syncAll() {
        if (!syncInProgress.compareAndSet(false, true)) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(409),
                "A Brevo sync is already in progress");
        }
        try {
            return doSync();
        } finally {
            syncInProgress.set(false);
        }
    }

    private BrevoSyncResultDto doSync() {
        final List<String> errors = new ArrayList<>();
        int companiesImported = 0;
        int companiesUpdated = 0;
        int companiesFailed = 0;
        int contactsImported = 0;
        int contactsUpdated = 0;
        int contactsFailed = 0;
        final long overallStartNanos = System.nanoTime();

        // Phase 1: Companies
        LOG.info("Starting Brevo company sync");
        final List<BrevoCompany> brevoCompanies = brevoApiClient.fetchAllCompanies();
        final Set<String> seenCompanyIds = new HashSet<>();
        final Map<Long, String> contactToCompanyBrevoId = new HashMap<>();

        final long companyPhaseStartNanos = System.nanoTime();
        LOG.info("Processing {} Brevo companies", brevoCompanies.size());
        int companyIndex = 0;
        for (final BrevoCompany brevoCompany : brevoCompanies) {
            companyIndex++;
            seenCompanyIds.add(brevoCompany.id());
            LOG.debug("Fetched Brevo company: id={}, name={}", brevoCompany.id(), brevoCompany.name());
            try {
                final boolean isNew = syncCompany(brevoCompany);
                if (isNew) {
                    companiesImported++;
                    LOG.debug("Created company '{}' (Brevo ID {})", brevoCompany.name(), brevoCompany.id());
                } else {
                    companiesUpdated++;
                    LOG.debug("Updated company '{}' (Brevo ID {})", brevoCompany.name(), brevoCompany.id());
                }
                for (final Long contactId : brevoCompany.linkedContactsIds()) {
                    contactToCompanyBrevoId.put(contactId, brevoCompany.id());
                }
            } catch (final Exception e) {
                companiesFailed++;
                final String msg = "Failed to sync company '" + brevoCompany.name()
                    + "' (Brevo ID " + brevoCompany.id() + "): " + e.getMessage();
                LOG.error(msg, e);
                errors.add(msg);
            }
            if (companyIndex % COMPANY_PROGRESS_INTERVAL == 0 || companyIndex == brevoCompanies.size()) {
                LOG.info("Company sync progress: {}/{} processed ({} created, {} updated, {} failed)",
                    companyIndex, brevoCompanies.size(),
                    companiesImported, companiesUpdated, companiesFailed);
            }
        }
        LOG.info("Company sync complete: {} imported, {} updated, {} failed (took {} ms)",
            companiesImported, companiesUpdated, companiesFailed,
            (System.nanoTime() - companyPhaseStartNanos) / 1_000_000);

        // Phase 2: Contacts
        LOG.info("Starting Brevo contact sync");
        final List<BrevoContact> brevoContacts = brevoApiClient.fetchAllContacts();
        final Set<String> seenContactIds = new HashSet<>();

        final long contactPhaseStartNanos = System.nanoTime();
        LOG.info("Processing {} Brevo contacts", brevoContacts.size());
        int contactIndex = 0;
        for (final BrevoContact brevoContact : brevoContacts) {
            contactIndex++;
            seenContactIds.add(String.valueOf(brevoContact.id()));
            try {
                final String firstName = getStringAttribute(brevoContact.attributes(), "VORNAME");
                final String lastName = getStringAttribute(brevoContact.attributes(), "NACHNAME");

                if (isBlank(firstName) && isBlank(lastName)) {
                    final String msg = "Skipping contact (Brevo ID " + brevoContact.id()
                        + ", email=" + brevoContact.email()
                        + "): both VORNAME and NACHNAME are missing";
                    LOG.warn(msg);
                    errors.add(msg);
                    contactsFailed++;
                    continue;
                }

                final boolean isNew = syncContact(brevoContact, contactToCompanyBrevoId);
                if (isNew) {
                    contactsImported++;
                } else {
                    contactsUpdated++;
                }
            } catch (final Exception e) {
                contactsFailed++;
                final String msg = "Failed to sync contact (Brevo ID " + brevoContact.id()
                    + ", email=" + brevoContact.email() + "): " + e.getMessage();
                LOG.error(msg, e);
                errors.add(msg);
            }
            if (contactIndex % CONTACT_PROGRESS_INTERVAL == 0 || contactIndex == brevoContacts.size()) {
                LOG.info("Contact sync progress: {}/{} processed ({} created, {} updated, {} failed)",
                    contactIndex, brevoContacts.size(),
                    contactsImported, contactsUpdated, contactsFailed);
            }
        }
        LOG.info("Contact sync complete: {} imported, {} updated, {} failed (took {} ms)",
            contactsImported, contactsUpdated, contactsFailed,
            (System.nanoTime() - contactPhaseStartNanos) / 1_000_000);

        // Phase 3: Unlink entries no longer in Brevo
        LOG.info("Starting Brevo unlink phase");
        final long unlinkPhaseStartNanos = System.nanoTime();
        int companiesUnlinked = 0;
        int contactsUnlinked = 0;

        final List<CompanyEntity> linkedCompanies = companyRepository.findAllByBrevoCompanyIdIsNotNull();
        LOG.info("Scanning {} CRM companies for unlinking", linkedCompanies.size());
        for (final CompanyEntity company : linkedCompanies) {
            if (!seenCompanyIds.contains(company.getBrevoCompanyId())) {
                transactionTemplate.executeWithoutResult(status -> {
                    company.setBrevoCompanyId(null);
                    companyRepository.save(company);
                    publishCompanyEvent(company.getId(), false);
                });
                companiesUnlinked++;
                LOG.info("Unlinked company '{}' (former Brevo ID removed)", company.getName());
            }
        }

        final List<ContactEntity> linkedContacts = contactRepository.findAllByBrevoIdIsNotNull();
        LOG.info("Scanning {} CRM contacts for unlinking", linkedContacts.size());
        for (final ContactEntity contact : linkedContacts) {
            if (!seenContactIds.contains(contact.getBrevoId())) {
                transactionTemplate.executeWithoutResult(status -> {
                    contact.setBrevoId(null);
                    contact.setReceivesNewsletter(false);
                    contactRepository.save(contact);
                    publishContactEvent(contact.getId(), false);
                });
                contactsUnlinked++;
                LOG.info("Unlinked contact '{} {}' (former Brevo ID removed)",
                    contact.getFirstName(), contact.getLastName());
            }
        }

        LOG.info("Unlink phase complete: {} companies, {} contacts unlinked (took {} ms)",
            companiesUnlinked, contactsUnlinked,
            (System.nanoTime() - unlinkPhaseStartNanos) / 1_000_000);

        LOG.info("Brevo sync finished in {} ms ({} company errors, {} contact errors)",
            (System.nanoTime() - overallStartNanos) / 1_000_000,
            companiesFailed, contactsFailed);

        return new BrevoSyncResultDto(
            companiesImported, companiesUpdated, companiesFailed, companiesUnlinked,
            contactsImported, contactsUpdated, contactsFailed, contactsUnlinked,
            errors);
    }

    /**
     * Syncs a single Brevo company into the CRM database.
     *
     * @return true if a new company was created, false if an existing one was updated
     */
    private boolean syncCompany(final BrevoCompany brevoCompany) {
        final Boolean isNew = transactionTemplate.execute(status -> {
            Optional<CompanyEntity> existing = companyRepository.findByBrevoCompanyId(brevoCompany.id());
            if (existing.isEmpty() && brevoCompany.name() != null) {
                existing = companyRepository.findByNameIgnoreCase(brevoCompany.name());
            }

            final CompanyEntity entity;
            final boolean created;
            if (existing.isPresent()) {
                entity = existing.get();
                created = false;
            } else {
                entity = new CompanyEntity();
                created = true;
            }

            if (brevoCompany.name() != null) {
                entity.setName(brevoCompany.name());
            }
            entity.setWebsite(brevoCompany.domain());
            entity.setBrevoCompanyId(brevoCompany.id());
            companyRepository.saveAndFlush(entity);
            publishCompanyEvent(entity.getId(), created);
            return created;
        });
        return Boolean.TRUE.equals(isNew);
    }

    /**
     * Syncs a single Brevo contact into the CRM database.
     *
     * @return true if a new contact was created, false if an existing one was updated
     */
    private boolean syncContact(final BrevoContact brevoContact,
                                final Map<Long, String> contactToCompanyBrevoId) {
        final Boolean isNew = transactionTemplate.execute(status -> {
            Optional<ContactEntity> existing = contactRepository.findByBrevoId(String.valueOf(brevoContact.id()));
            if (existing.isEmpty() && brevoContact.email() != null) {
                existing = contactRepository.findByEmailIgnoreCase(brevoContact.email());
            }

            final ContactEntity entity;
            final boolean created;
            if (existing.isPresent()) {
                entity = existing.get();
                created = false;
            } else {
                entity = new ContactEntity();
                created = true;
            }

            final Map<String, Object> attrs = brevoContact.attributes();
            final String firstName = getStringAttribute(attrs, "VORNAME");
            final String lastName = getStringAttribute(attrs, "NACHNAME");
            final String email = getStringAttribute(attrs, "E-MAIL");
            final String sms = getStringAttribute(attrs, "SMS");
            final String jobTitle = getStringAttribute(attrs, "JOB_TITLE");
            final String linkedIn = getStringAttribute(attrs, "LINKEDIN");
            final String firmaManuell = getStringAttribute(attrs, "FIRMA_MANUELL");

            // Always set Brevo-managed fields
            entity.setFirstName(firstName != null ? firstName : "");
            entity.setLastName(lastName != null ? lastName : "");
            entity.setEmail(email != null ? email : brevoContact.email());
            entity.setBrevoId(String.valueOf(brevoContact.id()));
            entity.setLanguage(mapLanguage(attrs.get("SPRACHE")));
            entity.setReceivesNewsletter(computeNewsletterStatus(attrs, brevoContact.emailBlacklisted()));

            // Only set user-editable fields on first import
            if (created) {
                entity.setPhoneNumber(sms);
                entity.setPosition(jobTitle);
                if (linkedIn != null) {
                    final SocialLinkEntity linkEntity = new SocialLinkEntity();
                    linkEntity.setNetworkType(SocialNetworkType.LINKEDIN);
                    linkEntity.setValue(linkedIn);
                    linkEntity.setUrl(linkedIn);
                    entity.getSocialLinks().add(linkEntity);
                }

                final CompanyEntity company = resolveCompany(
                    brevoContact.id(), contactToCompanyBrevoId, firmaManuell);
                entity.setCompany(company);
            } else {
                entity.getSocialLinks().removeIf(link -> link.getNetworkType() == SocialNetworkType.LINKEDIN);
                if (linkedIn != null) {
                    final SocialLinkEntity linkEntity = new SocialLinkEntity();
                    linkEntity.setNetworkType(SocialNetworkType.LINKEDIN);
                    linkEntity.setValue(linkedIn);
                    linkEntity.setUrl(linkedIn);
                    entity.getSocialLinks().add(linkEntity);
                }
            }

            contactRepository.saveAndFlush(entity);
            publishContactEvent(entity.getId(), created);
            return created;
        });
        return Boolean.TRUE.equals(isNew);
    }

    private CompanyEntity resolveCompany(final long brevoContactId,
                                         final Map<Long, String> contactToCompanyBrevoId,
                                         final String firmaManuell) {
        // First: check if linked via Brevo company
        final String companyBrevoId = contactToCompanyBrevoId.get(brevoContactId);
        if (companyBrevoId != null) {
            final Optional<CompanyEntity> company = companyRepository.findByBrevoCompanyId(companyBrevoId);
            if (company.isPresent()) {
                LOG.debug("Contact {} resolved to company '{}' via Brevo company ID {}",
                    brevoContactId, company.get().getName(), companyBrevoId);
                return company.get();
            }
        }

        // Second: check FIRMA_MANUELL attribute
        if (!isBlank(firmaManuell)) {
            final Optional<CompanyEntity> existing = companyRepository.findByNameIgnoreCase(firmaManuell);
            if (existing.isPresent()) {
                LOG.debug("Contact {} resolved to existing company '{}' via FIRMA_MANUELL",
                    brevoContactId, firmaManuell);
                return existing.get();
            }
            // Create new company from FIRMA_MANUELL
            final CompanyEntity newCompany = new CompanyEntity();
            newCompany.setName(firmaManuell);
            LOG.debug("Contact {} created new company '{}' from FIRMA_MANUELL",
                brevoContactId, firmaManuell);
            final CompanyEntity saved = companyRepository.saveAndFlush(newCompany);
            publishCompanyEvent(saved.getId(), true);
            return saved;
        }

        LOG.debug("Contact {} has no company association", brevoContactId);
        return null;
    }

    private Language mapLanguage(final Object sprache) {
        if (sprache == null) {
            return null;
        }
        final int value;
        if (sprache instanceof Number number) {
            value = number.intValue();
        } else {
            try {
                value = (int) Double.parseDouble(sprache.toString());
            } catch (final NumberFormatException e) {
                return null;
            }
        }
        return switch (value) {
            case 1 -> Language.DE;
            case 2 -> Language.EN;
            default -> null;
        };
    }

    private String getStringAttribute(final Map<String, Object> attrs, final String key) {
        final Object val = attrs.get(key);
        if (val == null) {
            return null;
        }
        final String str = val.toString().trim();
        return str.isEmpty() ? null : str;
    }

    private boolean computeNewsletterStatus(final Map<String, Object> attrs, final boolean emailBlacklisted) {
        if (emailBlacklisted) {
            return false;
        }
        final Object doubleOptIn = attrs.get("DOUBLE_OPT-IN");
        if (doubleOptIn == null) {
            return false;
        }
        if (doubleOptIn instanceof Number num) {
            return num.intValue() == 1;
        }
        try {
            return Integer.parseInt(doubleOptIn.toString().trim()) == 1;
        } catch (final NumberFormatException e) {
            return false;
        }
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    /**
     * Publishes the same OnObjectCreate / OnObjectUpdate event a regular
     * {@code companyService.save(dto)} would publish, so audit, search-index
     * and webhook listeners pick up the Brevo sync automatically.
     */
    private void publishCompanyEvent(final UUID companyId, final boolean created) {
        final CompanyDto dto = companyService.findById(companyId).orElseThrow(
            () -> new IllegalStateException("Company " + companyId + " disappeared mid-sync"));
        if (created) {
            eventPublisher.publishEvent(new OnObjectCreate<>(dto));
        } else {
            eventPublisher.publishEvent(new OnObjectUpdate<>(dto));
        }
    }

    private void publishContactEvent(final UUID contactId, final boolean created) {
        final ContactDto dto = contactService.findById(contactId).orElseThrow(
            () -> new IllegalStateException("Contact " + contactId + " disappeared mid-sync"));
        if (created) {
            eventPublisher.publishEvent(new OnObjectCreate<>(dto));
        } else {
            eventPublisher.publishEvent(new OnObjectUpdate<>(dto));
        }
    }
}
