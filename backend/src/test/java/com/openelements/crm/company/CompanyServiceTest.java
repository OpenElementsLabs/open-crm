package com.openelements.crm.company;

import com.openelements.crm.ImageData;
import com.openelements.crm.comment.CommentCreateDto;
import com.openelements.crm.comment.CommentRepository;
import com.openelements.crm.comment.CommentService;
import com.openelements.crm.contact.ContactCreateDto;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.contact.ContactService;
import java.util.UUID;
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
    }

    private CompanyDto createCompany(final String name) {
        return companyService.create(new CompanyCreateDto(name, null, null, null, null, null, null, null));
    }

    private void createContact(final String firstName, final String lastName, final UUID companyId) {
        contactService.create(new ContactCreateDto(firstName, lastName, null, null, null, null, null, companyId, null, null));
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
            assertFalse(result.deleted());
        }

        @Test
        @DisplayName("should persist to database")
        void shouldPersistToDatabase() {
            final CompanyDto created = createCompany("Persisted Corp");

            final CompanyDto fetched = companyService.getById(created.id());
            assertEquals(created.id(), fetched.id());
            assertEquals("Persisted Corp", fetched.name());
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
                            "New Street", "99", "54321", "Munich", "Austria"));

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
        @DisplayName("should throw 404 for nonexistent ID")
        void shouldThrow404ForNonexistentId() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> companyService.update(fakeId, new CompanyUpdateDto("Name", null, null, null, null, null, null, null)));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should soft-delete company without contacts")
        void shouldSoftDeleteCompanyWithoutContacts() {
            final CompanyDto company = createCompany("To Delete");

            companyService.delete(company.id());

            final CompanyDto result = companyService.getById(company.id());
            assertTrue(result.deleted());
        }

        @Test
        @DisplayName("should throw 409 when company has contacts")
        void shouldThrow409WhenCompanyHasContacts() {
            final CompanyDto company = createCompany("Has Contacts");
            createContact("John", "Doe", company.id());

            final var ex = assertThrows(ResponseStatusException.class, () -> companyService.delete(company.id()));
            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        }

        @Test
        @DisplayName("should throw 404 for nonexistent ID")
        void shouldThrow404ForNonexistentId() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class, () -> companyService.delete(fakeId));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("restore")
    class Restore {

        @Test
        @DisplayName("should restore soft-deleted company")
        void shouldRestoreSoftDeletedCompany() {
            final CompanyDto company = createCompany("Restore Me");
            companyService.delete(company.id());

            final CompanyDto restored = companyService.restore(company.id());

            assertFalse(restored.deleted());
        }

        @Test
        @DisplayName("should be idempotent for non-deleted company")
        void shouldBeIdempotentForNonDeletedCompany() {
            final CompanyDto company = createCompany("Not Deleted");

            final CompanyDto result = companyService.restore(company.id());

            assertFalse(result.deleted());
        }

        @Test
        @DisplayName("should throw 404 for nonexistent ID")
        void shouldThrow404ForNonexistentId() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class, () -> companyService.restore(fakeId));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("list")
    class List {

        @Test
        @DisplayName("should exclude soft-deleted by default")
        void shouldExcludeSoftDeletedByDefault() {
            createCompany("Active 1");
            createCompany("Active 2");
            final CompanyDto toDelete = createCompany("To Delete");
            companyService.delete(toDelete.id());

            final var page = companyService.list(null, false, PageRequest.of(0, 20));

            assertEquals(2, page.getTotalElements());
        }

        @Test
        @DisplayName("should include soft-deleted when includeDeleted is true")
        void shouldIncludeSoftDeletedWhenIncludeDeletedIsTrue() {
            createCompany("Active");
            final CompanyDto toDelete = createCompany("Deleted");
            companyService.delete(toDelete.id());

            final var page = companyService.list(null, true, PageRequest.of(0, 20));

            assertEquals(2, page.getTotalElements());
        }

        @Test
        @DisplayName("should filter by name (partial, case-insensitive)")
        void shouldFilterByName() {
            createCompany("Open Elements");
            createCompany("Acme Corp");

            final var page = companyService.list("open", false, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("Open Elements", page.getContent().get(0).name());
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
