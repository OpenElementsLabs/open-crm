package com.openelements.crm.brevo;

import com.openelements.crm.comment.CommentRepository;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.contact.Language;
import com.openelements.crm.settings.SettingsService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("BrevoSyncService")
class BrevoSyncServiceTest {

    @Autowired
    private BrevoSyncService brevoSyncService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private BrevoApiClient brevoApiClient;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        contactRepository.deleteAll();
        companyRepository.deleteAll();
        reset(brevoApiClient);
        settingsService.set("brevo.api-key", "test-key");
    }

    private BrevoCompany makeBrevoCompany(final String id, final String name, final String domain,
                                           final List<Long> linkedContactsIds) {
        return new BrevoCompany(id, name, domain, linkedContactsIds);
    }

    private BrevoContact makeBrevoContact(final long id, final String email,
                                           final Map<String, Object> attributes) {
        return new BrevoContact(id, email, attributes);
    }

    /**
     * Loads the company associated with a contact within a transaction to avoid
     * LazyInitializationException.
     */
    private CompanyEntity getCompanyForContact(final ContactEntity contact) {
        return transactionTemplate.execute(status -> {
            final ContactEntity managed = contactRepository.findById(contact.getId()).orElseThrow();
            final CompanyEntity company = managed.getCompany();
            if (company != null) {
                // Force initialization of the proxy
                company.getName();
            }
            return company;
        });
    }

    @Nested
    @DisplayName("Company Import")
    class CompanyImport {

        @Test
        @DisplayName("imports new company")
        void importsNewCompany() {
            when(brevoApiClient.fetchAllCompanies()).thenReturn(
                    List.of(makeBrevoCompany("aaa111", "Acme", "acme.com", List.of())));
            when(brevoApiClient.fetchAllContacts()).thenReturn(List.of());

            final BrevoSyncResultDto result = brevoSyncService.syncAll();

            assertEquals(1, result.companiesImported());
            assertEquals(0, result.companiesUpdated());
            final List<CompanyEntity> companies = companyRepository.findAll();
            assertEquals(1, companies.size());
            assertEquals("Acme", companies.get(0).getName());
            assertEquals("acme.com", companies.get(0).getWebsite());
            assertEquals("aaa111", companies.get(0).getBrevoCompanyId());
        }

        @Test
        @DisplayName("updates company matched by brevoCompanyId")
        void updatesCompanyMatchedByBrevoId() {
            final CompanyEntity existing = new CompanyEntity();
            existing.setName("Old Name");
            existing.setBrevoCompanyId("aaa111");
            companyRepository.saveAndFlush(existing);

            when(brevoApiClient.fetchAllCompanies()).thenReturn(
                    List.of(makeBrevoCompany("aaa111", "New Name", "new.com", List.of())));
            when(brevoApiClient.fetchAllContacts()).thenReturn(List.of());

            final BrevoSyncResultDto result = brevoSyncService.syncAll();

            assertEquals(0, result.companiesImported());
            assertEquals(1, result.companiesUpdated());
            final CompanyEntity updated = companyRepository.findByBrevoCompanyId("aaa111").orElseThrow();
            assertEquals("New Name", updated.getName());
            assertEquals("new.com", updated.getWebsite());
        }

        @Test
        @DisplayName("first import matches by name")
        void firstImportMatchesByName() {
            final CompanyEntity existing = new CompanyEntity();
            existing.setName("Acme Corp");
            companyRepository.saveAndFlush(existing);

            when(brevoApiClient.fetchAllCompanies()).thenReturn(
                    List.of(makeBrevoCompany("aaa111", "Acme Corp", "acme.com", List.of())));
            when(brevoApiClient.fetchAllContacts()).thenReturn(List.of());

            brevoSyncService.syncAll();

            assertEquals(1, companyRepository.count());
            final CompanyEntity matched = companyRepository.findByBrevoCompanyId("aaa111").orElseThrow();
            assertEquals("Acme Corp", matched.getName());
            assertEquals("aaa111", matched.getBrevoCompanyId());
        }

        @Test
        @DisplayName("name matching is case-insensitive")
        void nameMatchingIsCaseInsensitive() {
            final CompanyEntity existing = new CompanyEntity();
            existing.setName("Acme Corp");
            companyRepository.saveAndFlush(existing);

            when(brevoApiClient.fetchAllCompanies()).thenReturn(
                    List.of(makeBrevoCompany("aaa111", "ACME CORP", "acme.com", List.of())));
            when(brevoApiClient.fetchAllContacts()).thenReturn(List.of());

            brevoSyncService.syncAll();

            assertEquals(1, companyRepository.count());
            final CompanyEntity matched = companyRepository.findByBrevoCompanyId("aaa111").orElseThrow();
            assertNotNull(matched.getBrevoCompanyId());
        }

        @Test
        @DisplayName("imports company without domain")
        void importsCompanyWithoutDomain() {
            when(brevoApiClient.fetchAllCompanies()).thenReturn(
                    List.of(makeBrevoCompany("aaa111", "NoDomain Inc", null, List.of())));
            when(brevoApiClient.fetchAllContacts()).thenReturn(List.of());

            brevoSyncService.syncAll();

            final CompanyEntity company = companyRepository.findByBrevoCompanyId("aaa111").orElseThrow();
            assertNull(company.getWebsite());
        }

        @Test
        @DisplayName("CRM-only companies untouched")
        void crmOnlyCompaniesUntouched() {
            final CompanyEntity local = new CompanyEntity();
            local.setName("Local GmbH");
            companyRepository.saveAndFlush(local);

            when(brevoApiClient.fetchAllCompanies()).thenReturn(
                    List.of(makeBrevoCompany("aaa111", "Brevo Corp", "brevo.com", List.of())));
            when(brevoApiClient.fetchAllContacts()).thenReturn(List.of());

            brevoSyncService.syncAll();

            assertEquals(2, companyRepository.count());
            final CompanyEntity unchanged = companyRepository.findByNameIgnoreCase("Local GmbH").orElseThrow();
            assertNull(unchanged.getBrevoCompanyId());
        }
    }

    @Nested
    @DisplayName("Contact Import")
    class ContactImport {

        @Test
        @DisplayName("imports new contact with all fields")
        void importsNewContactWithAllFields() {
            when(brevoApiClient.fetchAllCompanies()).thenReturn(
                    List.of(makeBrevoCompany("aaa111", "Acme", "acme.com", List.of(200L))));
            final Map<String, Object> attrs = new HashMap<>();
            attrs.put("VORNAME", "John");
            attrs.put("NACHNAME", "Doe");
            attrs.put("E-MAIL", "john@test.com");
            attrs.put("SMS", "+49123");
            attrs.put("JOB_TITLE", "CTO");
            attrs.put("LINKEDIN", "https://li");
            attrs.put("SPRACHE", 1.0);
            when(brevoApiClient.fetchAllContacts()).thenReturn(
                    List.of(makeBrevoContact(200L, "john@test.com", attrs)));

            final BrevoSyncResultDto result = brevoSyncService.syncAll();

            assertEquals(1, result.contactsImported());
            final ContactEntity contact = contactRepository.findByBrevoId("200").orElseThrow();
            assertEquals("John", contact.getFirstName());
            assertEquals("Doe", contact.getLastName());
            assertEquals("john@test.com", contact.getEmail());
            assertEquals("+49123", contact.getPhoneNumber());
            assertEquals("CTO", contact.getPosition());
            assertEquals("https://li", contact.getLinkedInUrl());
            assertEquals(Language.DE, contact.getLanguage());
            assertNotNull(contact.getBrevoId());
            final CompanyEntity company = getCompanyForContact(contact);
            assertNotNull(company);
            assertEquals("aaa111", company.getBrevoCompanyId());
        }

        @Test
        @DisplayName("updates contact matched by brevoId")
        void updatesContactMatchedByBrevoId() {
            final ContactEntity existing = new ContactEntity();
            existing.setFirstName("Old");
            existing.setLastName("Name");
            existing.setBrevoId("200");
            contactRepository.saveAndFlush(existing);

            when(brevoApiClient.fetchAllCompanies()).thenReturn(List.of());
            final Map<String, Object> attrs = new HashMap<>();
            attrs.put("VORNAME", "New");
            attrs.put("NACHNAME", "Name");
            when(brevoApiClient.fetchAllContacts()).thenReturn(
                    List.of(makeBrevoContact(200L, "new@test.com", attrs)));

            final BrevoSyncResultDto result = brevoSyncService.syncAll();

            assertEquals(0, result.contactsImported());
            assertEquals(1, result.contactsUpdated());
            final ContactEntity updated = contactRepository.findByBrevoId("200").orElseThrow();
            assertEquals("New", updated.getFirstName());
        }

        @Test
        @DisplayName("first import matches by email")
        void firstImportMatchesByEmail() {
            final ContactEntity existing = new ContactEntity();
            existing.setFirstName("John");
            existing.setLastName("Doe");
            existing.setEmail("john@test.com");
            contactRepository.saveAndFlush(existing);

            when(brevoApiClient.fetchAllCompanies()).thenReturn(List.of());
            final Map<String, Object> attrs = new HashMap<>();
            attrs.put("VORNAME", "John");
            attrs.put("NACHNAME", "Doe");
            when(brevoApiClient.fetchAllContacts()).thenReturn(
                    List.of(makeBrevoContact(200L, "john@test.com", attrs)));

            brevoSyncService.syncAll();

            assertEquals(1, contactRepository.count());
            final ContactEntity matched = contactRepository.findByBrevoId("200").orElseThrow();
            assertEquals("200", matched.getBrevoId());
        }

        @Test
        @DisplayName("email matching is case-insensitive")
        void emailMatchingIsCaseInsensitive() {
            final ContactEntity existing = new ContactEntity();
            existing.setFirstName("John");
            existing.setLastName("Doe");
            existing.setEmail("john@test.com");
            contactRepository.saveAndFlush(existing);

            when(brevoApiClient.fetchAllCompanies()).thenReturn(List.of());
            final Map<String, Object> attrs = new HashMap<>();
            attrs.put("VORNAME", "John");
            attrs.put("NACHNAME", "Doe");
            when(brevoApiClient.fetchAllContacts()).thenReturn(
                    List.of(makeBrevoContact(200L, "John@TEST.com", attrs)));

            brevoSyncService.syncAll();

            assertEquals(1, contactRepository.count());
            final ContactEntity matched = contactRepository.findByBrevoId("200").orElseThrow();
            assertNotNull(matched.getBrevoId());
        }

        @Test
        @DisplayName("SPRACHE=2 maps to EN")
        void sprache2MapsToEn() {
            when(brevoApiClient.fetchAllCompanies()).thenReturn(List.of());
            final Map<String, Object> attrs = new HashMap<>();
            attrs.put("VORNAME", "Jane");
            attrs.put("NACHNAME", "Doe");
            attrs.put("SPRACHE", 2.0);
            when(brevoApiClient.fetchAllContacts()).thenReturn(
                    List.of(makeBrevoContact(200L, "jane@test.com", attrs)));

            brevoSyncService.syncAll();

            final ContactEntity contact = contactRepository.findByBrevoId("200").orElseThrow();
            assertEquals(Language.EN, contact.getLanguage());
        }

        @Test
        @DisplayName("SPRACHE=3 maps to null")
        void sprache3MapsToNull() {
            when(brevoApiClient.fetchAllCompanies()).thenReturn(List.of());
            final Map<String, Object> attrs = new HashMap<>();
            attrs.put("VORNAME", "Jane");
            attrs.put("NACHNAME", "Doe");
            attrs.put("SPRACHE", 3.0);
            when(brevoApiClient.fetchAllContacts()).thenReturn(
                    List.of(makeBrevoContact(200L, "jane@test.com", attrs)));

            brevoSyncService.syncAll();

            final ContactEntity contact = contactRepository.findByBrevoId("200").orElseThrow();
            assertNull(contact.getLanguage());
        }

        @Test
        @DisplayName("SPRACHE not set maps to null")
        void spracheNotSetMapsToNull() {
            when(brevoApiClient.fetchAllCompanies()).thenReturn(List.of());
            final Map<String, Object> attrs = new HashMap<>();
            attrs.put("VORNAME", "Jane");
            attrs.put("NACHNAME", "Doe");
            when(brevoApiClient.fetchAllContacts()).thenReturn(
                    List.of(makeBrevoContact(200L, "jane@test.com", attrs)));

            brevoSyncService.syncAll();

            final ContactEntity contact = contactRepository.findByBrevoId("200").orElseThrow();
            assertNull(contact.getLanguage());
        }

        @Test
        @DisplayName("sets brevoId on import")
        void setsBrevoIdOnImport() {
            when(brevoApiClient.fetchAllCompanies()).thenReturn(List.of());
            final Map<String, Object> attrs = new HashMap<>();
            attrs.put("VORNAME", "Jane");
            attrs.put("NACHNAME", "Doe");
            when(brevoApiClient.fetchAllContacts()).thenReturn(
                    List.of(makeBrevoContact(200L, "jane@test.com", attrs)));

            brevoSyncService.syncAll();

            final ContactEntity contact = contactRepository.findByBrevoId("200").orElseThrow();
            assertNotNull(contact.getBrevoId());
        }

        @Test
        @DisplayName("CRM-only contacts untouched")
        void crmOnlyContactsUntouched() {
            final ContactEntity local = new ContactEntity();
            local.setFirstName("Local");
            local.setLastName("Person");
            local.setEmail("local@local.com");
            contactRepository.saveAndFlush(local);

            when(brevoApiClient.fetchAllCompanies()).thenReturn(List.of());
            final Map<String, Object> attrs = new HashMap<>();
            attrs.put("VORNAME", "Brevo");
            attrs.put("NACHNAME", "User");
            when(brevoApiClient.fetchAllContacts()).thenReturn(
                    List.of(makeBrevoContact(200L, "brevo@test.com", attrs)));

            brevoSyncService.syncAll();

            assertEquals(2, contactRepository.count());
            final ContactEntity unchanged = contactRepository.findByEmailIgnoreCase("local@local.com").orElseThrow();
            assertNull(unchanged.getBrevoId());
            assertNull(unchanged.getBrevoId());
        }
    }

    @Nested
    @DisplayName("Company-Contact Association")
    class CompanyContactAssociation {

        @Test
        @DisplayName("links contact to CRM company via linkedContactsIds")
        void linksContactToCompanyViaLinkedContactsIds() {
            when(brevoApiClient.fetchAllCompanies()).thenReturn(
                    List.of(makeBrevoCompany("aaa111", "Acme", "acme.com", List.of(200L))));
            final Map<String, Object> attrs = new HashMap<>();
            attrs.put("VORNAME", "John");
            attrs.put("NACHNAME", "Doe");
            when(brevoApiClient.fetchAllContacts()).thenReturn(
                    List.of(makeBrevoContact(200L, "john@test.com", attrs)));

            brevoSyncService.syncAll();

            final ContactEntity contact = contactRepository.findByBrevoId("200").orElseThrow();
            final CompanyEntity linkedCompany = getCompanyForContact(contact);
            assertNotNull(linkedCompany);
            assertEquals("aaa111", linkedCompany.getBrevoCompanyId());
        }

        @Test
        @DisplayName("FIRMA_MANUELL creates new company when no CRM link")
        void firmaManuellCreatesNewCompany() {
            when(brevoApiClient.fetchAllCompanies()).thenReturn(List.of());
            final Map<String, Object> attrs = new HashMap<>();
            attrs.put("VORNAME", "John");
            attrs.put("NACHNAME", "Doe");
            attrs.put("FIRMA_MANUELL", "Startup XYZ");
            when(brevoApiClient.fetchAllContacts()).thenReturn(
                    List.of(makeBrevoContact(200L, "john@test.com", attrs)));

            brevoSyncService.syncAll();

            final ContactEntity contact = contactRepository.findByBrevoId("200").orElseThrow();
            final CompanyEntity createdCompany = getCompanyForContact(contact);
            assertNotNull(createdCompany);
            assertEquals("Startup XYZ", createdCompany.getName());
        }

        @Test
        @DisplayName("CRM link takes priority over FIRMA_MANUELL")
        void crmLinkTakesPriorityOverFirmaManuell() {
            when(brevoApiClient.fetchAllCompanies()).thenReturn(
                    List.of(makeBrevoCompany("aaa111", "CRM Company", "crm.com", List.of(200L))));
            final Map<String, Object> attrs = new HashMap<>();
            attrs.put("VORNAME", "John");
            attrs.put("NACHNAME", "Doe");
            attrs.put("FIRMA_MANUELL", "Other");
            when(brevoApiClient.fetchAllContacts()).thenReturn(
                    List.of(makeBrevoContact(200L, "john@test.com", attrs)));

            brevoSyncService.syncAll();

            final ContactEntity contact = contactRepository.findByBrevoId("200").orElseThrow();
            final CompanyEntity priorityCompany = getCompanyForContact(contact);
            assertNotNull(priorityCompany);
            assertEquals("CRM Company", priorityCompany.getName());
            assertEquals("aaa111", priorityCompany.getBrevoCompanyId());
        }

        @Test
        @DisplayName("FIRMA_MANUELL matches existing company by name")
        void firmaManuellMatchesExistingCompanyByName() {
            final CompanyEntity existing = new CompanyEntity();
            existing.setName("Existing Corp");
            companyRepository.saveAndFlush(existing);

            when(brevoApiClient.fetchAllCompanies()).thenReturn(List.of());
            final Map<String, Object> attrs = new HashMap<>();
            attrs.put("VORNAME", "John");
            attrs.put("NACHNAME", "Doe");
            attrs.put("FIRMA_MANUELL", "Existing Corp");
            when(brevoApiClient.fetchAllContacts()).thenReturn(
                    List.of(makeBrevoContact(200L, "john@test.com", attrs)));

            brevoSyncService.syncAll();

            final ContactEntity contact = contactRepository.findByBrevoId("200").orElseThrow();
            final CompanyEntity matchedCompany = getCompanyForContact(contact);
            assertNotNull(matchedCompany);
            assertEquals("Existing Corp", matchedCompany.getName());
            assertEquals(1, companyRepository.count());
        }

        @Test
        @DisplayName("empty FIRMA_MANUELL does not create company")
        void emptyFirmaManuellDoesNotCreateCompany() {
            when(brevoApiClient.fetchAllCompanies()).thenReturn(List.of());
            final Map<String, Object> attrs = new HashMap<>();
            attrs.put("VORNAME", "John");
            attrs.put("NACHNAME", "Doe");
            attrs.put("FIRMA_MANUELL", "");
            when(brevoApiClient.fetchAllContacts()).thenReturn(
                    List.of(makeBrevoContact(200L, "john@test.com", attrs)));

            brevoSyncService.syncAll();

            final ContactEntity contact = contactRepository.findByBrevoId("200").orElseThrow();
            assertNull(contact.getCompany());
        }

        @Test
        @DisplayName("no company when neither CRM link nor FIRMA_MANUELL")
        void noCompanyWhenNeitherLinkNorFirma() {
            when(brevoApiClient.fetchAllCompanies()).thenReturn(List.of());
            final Map<String, Object> attrs = new HashMap<>();
            attrs.put("VORNAME", "John");
            attrs.put("NACHNAME", "Doe");
            when(brevoApiClient.fetchAllContacts()).thenReturn(
                    List.of(makeBrevoContact(200L, "john@test.com", attrs)));

            brevoSyncService.syncAll();

            final ContactEntity contact = contactRepository.findByBrevoId("200").orElseThrow();
            assertNull(contact.getCompany());
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("skips contact missing both VORNAME and NACHNAME")
        void skipsContactMissingBothNames() {
            when(brevoApiClient.fetchAllCompanies()).thenReturn(List.of());
            final Map<String, Object> attrs = new HashMap<>();
            when(brevoApiClient.fetchAllContacts()).thenReturn(
                    List.of(makeBrevoContact(200L, "noname@test.com", attrs)));

            final BrevoSyncResultDto result = brevoSyncService.syncAll();

            assertEquals(1, result.contactsFailed());
            assertEquals(0, result.contactsImported());
            assertEquals(0, contactRepository.count());
            assertFalse(result.errors().isEmpty());
        }

        @Test
        @DisplayName("partial failure preserves successful imports")
        void partialFailurePreservesSuccessfulImports() {
            when(brevoApiClient.fetchAllCompanies()).thenReturn(List.of());

            final Map<String, Object> attrs1 = new HashMap<>();
            attrs1.put("VORNAME", "First");
            attrs1.put("NACHNAME", "Person");

            final Map<String, Object> attrs2 = new HashMap<>();
            // no VORNAME or NACHNAME — will be skipped

            final Map<String, Object> attrs3 = new HashMap<>();
            attrs3.put("VORNAME", "Third");
            attrs3.put("NACHNAME", "Person");

            when(brevoApiClient.fetchAllContacts()).thenReturn(List.of(
                    makeBrevoContact(201L, "first@test.com", attrs1),
                    makeBrevoContact(202L, "noname@test.com", attrs2),
                    makeBrevoContact(203L, "third@test.com", attrs3)));

            final BrevoSyncResultDto result = brevoSyncService.syncAll();

            assertEquals(2, result.contactsImported());
            assertEquals(1, result.contactsFailed());
            assertEquals(2, contactRepository.count());
        }

        @Test
        @DisplayName("concurrent sync returns 409")
        void concurrentSyncReturns409() throws Exception {
            final CountDownLatch fetchStarted = new CountDownLatch(1);
            final CountDownLatch proceedWithFetch = new CountDownLatch(1);

            when(brevoApiClient.fetchAllCompanies()).thenAnswer(invocation -> {
                fetchStarted.countDown();
                proceedWithFetch.await();
                return List.of();
            });
            when(brevoApiClient.fetchAllContacts()).thenReturn(List.of());

            final AtomicReference<BrevoSyncResultDto> firstResult = new AtomicReference<>();
            final Thread syncThread = new Thread(() -> firstResult.set(brevoSyncService.syncAll()));
            syncThread.start();

            fetchStarted.await();

            // Second sync should fail with 409
            try {
                brevoSyncService.syncAll();
                throw new AssertionError("Expected ResponseStatusException with 409");
            } catch (final ResponseStatusException e) {
                assertEquals(409, e.getStatusCode().value());
            } finally {
                proceedWithFetch.countDown();
                syncThread.join();
            }

            assertNotNull(firstResult.get());
        }
    }

    @Nested
    @DisplayName("Result Counts")
    class ResultCounts {

        @Test
        @DisplayName("result counts are correct for mixed import")
        void resultCountsAreCorrectForMixedImport() {
            // Pre-create 1 company (will be updated)
            final CompanyEntity existingCompany = new CompanyEntity();
            existingCompany.setName("Existing Co");
            existingCompany.setBrevoCompanyId("aaa111");
            companyRepository.saveAndFlush(existingCompany);

            // Pre-create 1 contact (will be updated)
            final ContactEntity existingContact = new ContactEntity();
            existingContact.setFirstName("Existing");
            existingContact.setLastName("Contact");
            existingContact.setBrevoId("300");
            contactRepository.saveAndFlush(existingContact);

            // Mock: 3 companies (1 existing update + 2 new)
            when(brevoApiClient.fetchAllCompanies()).thenReturn(List.of(
                    makeBrevoCompany("aaa111", "Existing Co Updated", "existing.com", List.of()),
                    makeBrevoCompany("bbb222", "New Co 1", "new1.com", List.of()),
                    makeBrevoCompany("ccc333", "New Co 2", "new2.com", List.of())));

            // Mock: 4 contacts (1 existing update + 2 new + 1 failed)
            final Map<String, Object> existingAttrs = new HashMap<>();
            existingAttrs.put("VORNAME", "Updated");
            existingAttrs.put("NACHNAME", "Contact");

            final Map<String, Object> newAttrs1 = new HashMap<>();
            newAttrs1.put("VORNAME", "New1");
            newAttrs1.put("NACHNAME", "Person");

            final Map<String, Object> newAttrs2 = new HashMap<>();
            newAttrs2.put("VORNAME", "New2");
            newAttrs2.put("NACHNAME", "Person");

            final Map<String, Object> failedAttrs = new HashMap<>();
            // no names — will fail

            when(brevoApiClient.fetchAllContacts()).thenReturn(List.of(
                    makeBrevoContact(300L, "existing@test.com", existingAttrs),
                    makeBrevoContact(301L, "new1@test.com", newAttrs1),
                    makeBrevoContact(302L, "new2@test.com", newAttrs2),
                    makeBrevoContact(303L, "fail@test.com", failedAttrs)));

            final BrevoSyncResultDto result = brevoSyncService.syncAll();

            assertEquals(2, result.companiesImported());
            assertEquals(1, result.companiesUpdated());
            assertEquals(0, result.companiesFailed());
            assertEquals(2, result.contactsImported());
            assertEquals(1, result.contactsUpdated());
            assertEquals(1, result.contactsFailed());
            assertEquals(1, result.errors().size());
        }
    }
}
