package com.openelements.crm.brevo;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.contact.Language;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service that synchronizes companies and contacts from Brevo into the CRM database.
 */
@Service
public class BrevoSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(BrevoSyncService.class);

    private final BrevoApiClient brevoApiClient;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final TransactionTemplate transactionTemplate;
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

    /**
     * Creates a new BrevoSyncService.
     *
     * @param brevoApiClient      the Brevo API client
     * @param companyRepository   the company repository
     * @param contactRepository   the contact repository
     * @param transactionTemplate the transaction template for per-entity transactions
     */
    public BrevoSyncService(final BrevoApiClient brevoApiClient,
                            final CompanyRepository companyRepository,
                            final ContactRepository contactRepository,
                            final TransactionTemplate transactionTemplate) {
        this.brevoApiClient = Objects.requireNonNull(brevoApiClient, "brevoApiClient must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
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

        // Phase 1: Companies
        LOG.info("Starting Brevo company sync");
        final List<BrevoCompany> brevoCompanies = brevoApiClient.fetchAllCompanies();
        final Map<Long, String> contactToCompanyBrevoId = new HashMap<>();

        for (final BrevoCompany brevoCompany : brevoCompanies) {
            LOG.debug("Fetched Brevo company: id={}, name={}", brevoCompany.id(), brevoCompany.name());
            try {
                final boolean isNew = syncCompany(brevoCompany);
                if (isNew) {
                    companiesImported++;
                    LOG.info("Created company '{}' (Brevo ID {})", brevoCompany.name(), brevoCompany.id());
                } else {
                    companiesUpdated++;
                    LOG.info("Updated company '{}' (Brevo ID {})", brevoCompany.name(), brevoCompany.id());
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
        }
        LOG.info("Company sync complete: {} imported, {} updated, {} failed",
                companiesImported, companiesUpdated, companiesFailed);

        // Phase 2: Contacts
        LOG.info("Starting Brevo contact sync");
        final List<BrevoContact> brevoContacts = brevoApiClient.fetchAllContacts();

        for (final BrevoContact brevoContact : brevoContacts) {
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
        }
        LOG.info("Contact sync complete: {} imported, {} updated, {} failed",
                contactsImported, contactsUpdated, contactsFailed);

        return new BrevoSyncResultDto(
                companiesImported, companiesUpdated, companiesFailed,
                contactsImported, contactsUpdated, contactsFailed,
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
            Optional<ContactEntity> existing = contactRepository.findByBrevoId(brevoContact.id());
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

            entity.setFirstName(firstName != null ? firstName : "");
            entity.setLastName(lastName != null ? lastName : "");
            entity.setEmail(email != null ? email : brevoContact.email());
            entity.setPhoneNumber(sms);
            entity.setPosition(jobTitle);
            entity.setLinkedInUrl(linkedIn);
            entity.setBrevoId(brevoContact.id());
            entity.setSyncedToBrevo(true);

            // Language mapping
            entity.setLanguage(mapLanguage(attrs.get("SPRACHE")));

            // Company resolution
            final CompanyEntity company = resolveCompany(
                    brevoContact.id(), contactToCompanyBrevoId, firmaManuell);
            entity.setCompany(company);

            contactRepository.saveAndFlush(entity);
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
            return companyRepository.saveAndFlush(newCompany);
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

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
