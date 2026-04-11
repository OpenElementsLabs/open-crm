package com.openelements.crm.company;

import com.openelements.crm.ImageData;
import com.openelements.crm.TestSecurityUtil;
import com.openelements.crm.comment.CommentCreateDto;
import com.openelements.crm.comment.CommentRepository;
import com.openelements.crm.comment.CommentService;
import com.openelements.crm.contact.ContactCreateDto;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.contact.ContactService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("CompanyService")
class CompanyServiceTest {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        contactRepository.deleteAll();
        companyRepository.deleteAll();
        TestSecurityUtil.setSecurityContext();
    }

    @AfterEach
    void tearDown() {
        TestSecurityUtil.clearSecurityContext();
    }

    private CompanyDto createCompany(final String name) {
        return companyService.create(new CompanyCreateDto(name, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
    }

    private void createContact(final String firstName, final String lastName, final UUID companyId) {
        contactService.create(new ContactCreateDto(null, firstName, lastName, null, null, null, null, null, companyId, null, null, null, null));
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create and return DTO with zero counts")
        void shouldCreateAndReturnDtoWithZeroCounts() {
            final CompanyDto result = createCompany("Test Corp");

            assertNotNull(result.id());
            assertEquals("Test Corp", result.name());
            assertEquals(0, result.contactCount());
            assertEquals(0, result.commentCount());
        }

        @Test
        @DisplayName("should persist to database")
        void shouldPersistToDatabase() {
            final CompanyDto created = createCompany("Persisted Corp");

            final CompanyDto fetched = companyService.getById(created.id());
            assertEquals(created.id(), fetched.id());
            assertEquals("Persisted Corp", fetched.name());
        }

        @Test
        @DisplayName("should create company with description")
        void shouldCreateWithDescription() {
            final CompanyDto result = companyService.create(
                    new CompanyCreateDto("Desc Corp", null, null, null, null, null, null, null, null, "A great company", null, null, null, null, null));

            assertEquals("A great company", result.description());
        }

        @Test
        @DisplayName("should create company without description")
        void shouldCreateWithoutDescription() {
            final CompanyDto result = createCompany("No Desc Corp");

            assertNull(result.description());
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("should return with correct counts")
        void shouldReturnWithCorrectCounts() {
            final CompanyDto company = createCompany("Counted Corp");
            createContact("Alice", "A", company.id());
            createContact("Bob", "B", company.id());
            commentService.addToCompany(company.id(), new CommentCreateDto("A comment"));

            final CompanyDto result = companyService.getById(company.id());

            assertEquals(2, result.contactCount());
            assertEquals(1, result.commentCount());
        }

        @Test
        @DisplayName("should throw 404 for nonexistent ID")
        void shouldThrow404ForNonexistentId() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class, () -> companyService.getById(fakeId));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update all fields")
        void shouldUpdateAllFields() {
            final CompanyDto company = createCompany("Old Name");

            final CompanyDto updated = companyService.update(company.id(),
                    new CompanyUpdateDto("New Name", "new@test.com", "https://new.com",
                            "New Street", "99", "54321", "Munich", "Austria", "+49 123", null, null, null, null, null, null));

            assertEquals("New Name", updated.name());
            assertEquals("new@test.com", updated.email());
            assertEquals("https://new.com", updated.website());
            assertEquals("New Street", updated.street());
            assertEquals("99", updated.houseNumber());
            assertEquals("54321", updated.zipCode());
            assertEquals("Munich", updated.city());
            assertEquals("Austria", updated.country());
        }

        @Test
        @DisplayName("should update description")
        void shouldUpdateDescription() {
            final CompanyDto company = createCompany("Desc Co");

            final CompanyDto updated = companyService.update(company.id(),
                    new CompanyUpdateDto("Desc Co", null, null, null, null, null, null, null, null, "New description", null, null, null, null, null));

            assertEquals("New description", updated.description());
        }

        @Test
        @DisplayName("should clear description when set to null")
        void shouldClearDescription() {
            final CompanyDto company = companyService.create(
                    new CompanyCreateDto("Desc Co", null, null, null, null, null, null, null, null, "Initial desc", null, null, null, null, null));

            final CompanyDto updated = companyService.update(company.id(),
                    new CompanyUpdateDto("Desc Co", null, null, null, null, null, null, null, null, null, null, null, null, null, null));

            assertNull(updated.description());
        }

        @Test
        @DisplayName("should throw 404 for nonexistent ID")
        void shouldThrow404ForNonexistentId() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> companyService.update(fakeId, new CompanyUpdateDto("Name", null, null, null, null, null, null, null, null, null, null, null, null, null, null)));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should hard-delete company without contacts")
        void shouldHardDeleteCompanyWithoutContacts() {
            final CompanyDto company = createCompany("To Delete");

            companyService.delete(company.id(), false);

            final var ex = assertThrows(ResponseStatusException.class, () -> companyService.getById(company.id()));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("should delete company and its contacts when deleteContacts is true")
        void shouldDeleteCompanyAndContacts() {
            final CompanyDto company = createCompany("Has Contacts");
            createContact("John", "Doe", company.id());

            companyService.delete(company.id(), true);

            final var ex = assertThrows(ResponseStatusException.class, () -> companyService.getById(company.id()));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals(0, contactRepository.count());
        }

        @Test
        @DisplayName("should delete company only and unlink contacts when deleteContacts is false")
        void shouldDeleteCompanyOnlyAndUnlinkContacts() {
            final CompanyDto company = createCompany("Company Only");
            createContact("Jane", "Doe", company.id());

            companyService.delete(company.id(), false);

            final var ex = assertThrows(ResponseStatusException.class, () -> companyService.getById(company.id()));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals(1, contactRepository.count());
        }

        @Test
        @DisplayName("should throw 404 for nonexistent ID")
        void shouldThrow404ForNonexistentId() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class, () -> companyService.delete(fakeId, false));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("finance validation")
    class FinanceValidation {

        @Test
        @DisplayName("should accept valid IBAN")
        void shouldAcceptValidIban() {
            final CompanyDto result = companyService.create(
                    new CompanyCreateDto("IBAN Corp", null, null, null, null, null, null, null, null, null, null, null, "DE89370400440532013000", null, null));

            assertEquals("DE89370400440532013000", result.iban());
        }

        @Test
        @DisplayName("should strip whitespace from IBAN")
        void shouldStripWhitespaceFromIban() {
            final CompanyDto result = companyService.create(
                    new CompanyCreateDto("IBAN Space Corp", null, null, null, null, null, null, null, null, null, null, null, "DE89 3704 0044 0532 0130 00", null, null));

            assertEquals("DE89370400440532013000", result.iban());
        }

        @Test
        @DisplayName("should reject IBAN too short")
        void shouldRejectIbanTooShort() {
            final var ex = assertThrows(ResponseStatusException.class, () -> companyService.create(
                    new CompanyCreateDto("Short IBAN Corp", null, null, null, null, null, null, null, null, null, null, null, "DE89", null, null)));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("should reject IBAN with invalid country code")
        void shouldRejectIbanWithInvalidCountryCode() {
            final var ex = assertThrows(ResponseStatusException.class, () -> companyService.create(
                    new CompanyCreateDto("Bad Country IBAN Corp", null, null, null, null, null, null, null, null, null, null, null, "1289370400440532013000", null, null)));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("should reject IBAN exceeding max length")
        void shouldRejectIbanExceedingMaxLength() {
            final var ex = assertThrows(ResponseStatusException.class, () -> companyService.create(
                    new CompanyCreateDto("Long IBAN Corp", null, null, null, null, null, null, null, null, null, null, null, "DE893704004405320130001234567890123", null, null)));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("should accept empty IBAN")
        void shouldAcceptEmptyIban() {
            final CompanyDto result = companyService.create(
                    new CompanyCreateDto("No IBAN Corp", null, null, null, null, null, null, null, null, null, null, null, null, null, null));

            assertNull(result.iban());
        }

        @Test
        @DisplayName("should normalize blank IBAN to null")
        void shouldNormalizeBlankIbanToNull() {
            final CompanyDto result = companyService.create(
                    new CompanyCreateDto("Blank IBAN Corp", null, null, null, null, null, null, null, null, null, null, null, "  ", null, null));

            assertNull(result.iban());
        }

        @Test
        @DisplayName("should accept valid 8-character BIC")
        void shouldAcceptValid8CharBic() {
            final CompanyDto result = companyService.create(
                    new CompanyCreateDto("BIC8 Corp", null, null, null, null, null, null, null, null, null, null, "DEUTDEFF", null, null, null));

            assertEquals("DEUTDEFF", result.bic());
        }

        @Test
        @DisplayName("should accept valid 11-character BIC")
        void shouldAcceptValid11CharBic() {
            final CompanyDto result = companyService.create(
                    new CompanyCreateDto("BIC11 Corp", null, null, null, null, null, null, null, null, null, null, "DEUTDEFF500", null, null, null));

            assertEquals("DEUTDEFF500", result.bic());
        }

        @Test
        @DisplayName("should reject BIC with wrong length")
        void shouldRejectBicWithWrongLength() {
            final var ex = assertThrows(ResponseStatusException.class, () -> companyService.create(
                    new CompanyCreateDto("Bad BIC Corp", null, null, null, null, null, null, null, null, null, null, "DEUTDE", null, null, null)));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("should reject BIC with non-alphanumeric characters")
        void shouldRejectBicWithNonAlphanumericCharacters() {
            final var ex = assertThrows(ResponseStatusException.class, () -> companyService.create(
                    new CompanyCreateDto("Special BIC Corp", null, null, null, null, null, null, null, null, null, null, "DEUT-DEFF", null, null, null)));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("should accept empty BIC")
        void shouldAcceptEmptyBic() {
            final CompanyDto result = companyService.create(
                    new CompanyCreateDto("No BIC Corp", null, null, null, null, null, null, null, null, null, null, null, null, null, null));

            assertNull(result.bic());
        }

        @Test
        @DisplayName("should store VAT ID as-is")
        void shouldStoreVatIdAsIs() {
            final CompanyDto result = companyService.create(
                    new CompanyCreateDto("VAT Corp", null, null, null, null, null, null, null, null, null, null, null, null, "DE123456789", null));

            assertEquals("DE123456789", result.vatId());
        }

        @Test
        @DisplayName("should accept any format VAT ID")
        void shouldAcceptAnyFormatVatId() {
            final CompanyDto result = companyService.create(
                    new CompanyCreateDto("VAT AT Corp", null, null, null, null, null, null, null, null, null, null, null, null, "ATU12345678", null));

            assertEquals("ATU12345678", result.vatId());
        }

        @Test
        @DisplayName("should create company with all financial fields")
        void shouldCreateCompanyWithAllFinancialFields() {
            final CompanyDto result = companyService.create(
                    new CompanyCreateDto("Full Finance Corp", null, null, null, null, null, null, null, null, null, "Deutsche Bank", "DEUTDEFF", "DE89370400440532013000", "DE123456789", null));

            assertEquals("Deutsche Bank", result.bankName());
            assertEquals("DEUTDEFF", result.bic());
            assertEquals("DE89370400440532013000", result.iban());
            assertEquals("DE123456789", result.vatId());
        }

        @Test
        @DisplayName("should create company without financial fields")
        void shouldCreateCompanyWithoutFinancialFields() {
            final CompanyDto result = createCompany("No Finance Corp");

            assertNull(result.bankName());
            assertNull(result.bic());
            assertNull(result.iban());
            assertNull(result.vatId());
        }

        @Test
        @DisplayName("should update financial fields")
        void shouldUpdateFinancialFields() {
            final CompanyDto company = createCompany("Update Finance Corp");

            final CompanyDto updated = companyService.update(company.id(),
                    new CompanyUpdateDto("Update Finance Corp", null, null, null, null, null, null, null, null, null, null, null, "DE89370400440532013000", null, null));

            assertEquals("DE89370400440532013000", updated.iban());
        }

        @Test
        @DisplayName("should clear financial fields")
        void shouldClearFinancialFields() {
            final CompanyDto company = companyService.create(
                    new CompanyCreateDto("Clear Finance Corp", null, null, null, null, null, null, null, null, null, "Deutsche Bank", "DEUTDEFF", "DE89370400440532013000", "DE123456789", null));

            final CompanyDto updated = companyService.update(company.id(),
                    new CompanyUpdateDto("Clear Finance Corp", null, null, null, null, null, null, null, null, null, null, null, null, null, null));

            assertNull(updated.bankName());
            assertNull(updated.bic());
            assertNull(updated.iban());
            assertNull(updated.vatId());
        }
    }

    @Nested
    @DisplayName("list")
    class List {

        @Test
        @DisplayName("should list all companies")
        void shouldListAllCompanies() {
            createCompany("Active 1");
            createCompany("Active 2");

            final var page = companyService.list(null, null, null, PageRequest.of(0, 20));

            assertEquals(2, page.getTotalElements());
        }

        @Test
        @DisplayName("should filter by name (partial, case-insensitive)")
        void shouldFilterByName() {
            createCompany("Open Elements");
            createCompany("Acme Corp");

            final var page = companyService.list("open", null, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("Open Elements", page.getContent().get(0).name());
        }

        @Test
        @DisplayName("filters by brevo true")
        void filtersByBrevoTrue() {
            createCompany("Normal Corp");
            final CompanyEntity brevoEntity = new CompanyEntity();
            brevoEntity.setName("Brevo Corp");
            brevoEntity.setBrevoCompanyId("brevo-123");
            companyRepository.saveAndFlush(brevoEntity);

            final var page = companyService.list(null, true, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("Brevo Corp", page.getContent().get(0).name());
        }

        @Test
        @DisplayName("filters by brevo false")
        void filtersByBrevoFalse() {
            createCompany("Normal Corp");
            final CompanyEntity brevoEntity = new CompanyEntity();
            brevoEntity.setName("Brevo Corp");
            brevoEntity.setBrevoCompanyId("brevo-123");
            companyRepository.saveAndFlush(brevoEntity);

            final var page = companyService.list(null, false, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("Normal Corp", page.getContent().get(0).name());
        }

    }

    @Nested
    @DisplayName("logo")
    class Logo {

        @Test
        @DisplayName("should store logo data")
        void shouldStoreLogoData() {
            final CompanyDto company = createCompany("Logo Corp");
            final byte[] data = new byte[]{1, 2, 3, 4, 5};

            companyService.uploadLogo(company.id(), data, "image/png");

            final ImageData logo = companyService.getLogo(company.id());
            assertArrayEquals(data, logo.data());
            assertEquals("image/png", logo.contentType());
        }

        @Test
        @DisplayName("should reject invalid content type")
        void shouldRejectInvalidContentType() {
            final CompanyDto company = createCompany("Invalid Logo Corp");

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> companyService.uploadLogo(company.id(), new byte[]{1}, "image/gif"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("should throw 404 for nonexistent company")
        void shouldThrow404ForNonexistentCompany() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> companyService.uploadLogo(fakeId, new byte[]{1}, "image/png"));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("should throw 404 when no logo exists")
        void shouldThrow404WhenNoLogoExists() {
            final CompanyDto company = createCompany("No Logo Corp");

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> companyService.getLogo(company.id()));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("should remove logo data")
        void shouldRemoveLogoData() {
            final CompanyDto company = createCompany("Delete Logo Corp");
            companyService.uploadLogo(company.id(), new byte[]{1, 2, 3}, "image/png");

            companyService.deleteLogo(company.id());

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> companyService.getLogo(company.id()));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }
}
