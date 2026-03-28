package com.openelements.crm.contact;

import com.openelements.crm.ImageData;
import com.openelements.crm.comment.CommentCreateDto;
import com.openelements.crm.comment.CommentRepository;
import com.openelements.crm.comment.CommentService;
import com.openelements.crm.company.CompanyCreateDto;
import com.openelements.crm.company.CompanyDto;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.company.CompanyService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ContactService")
class ContactServiceTest {

    @Autowired
    private ContactService contactService;

    @Autowired
    private CompanyService companyService;

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

    private ContactDto createContact(final String firstName, final String lastName, final UUID companyId) {
        return contactService.create(new ContactCreateDto(firstName, lastName, null, null, null, null, null, companyId, null, null));
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create contact without company")
        void shouldCreateWithoutCompany() {
            final ContactDto result = createContact("Jane", "Doe", null);

            assertNotNull(result.id());
            assertEquals("Jane", result.firstName());
            assertEquals("Doe", result.lastName());
            assertNull(result.companyId());
        }

        @Test
        @DisplayName("should create contact with valid company")
        void shouldCreateWithValidCompany() {
            final CompanyDto company = createCompany("Open Elements");

            final ContactDto result = createContact("Hendrik", "Ebbers", company.id());

            assertEquals(company.id(), result.companyId());
            assertEquals("Open Elements", result.companyName());
        }

        @Test
        @DisplayName("should throw 400 for nonexistent company")
        void shouldThrow400ForNonexistentCompany() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> createContact("Jane", "Doe", fakeId));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("should throw 400 for soft-deleted company")
        void shouldThrow400ForSoftDeletedCompany() {
            final CompanyDto company = createCompany("Deleted Co");
            companyService.delete(company.id());

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> createContact("Jane", "Doe", company.id()));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("should return with comment count")
        void shouldReturnWithCommentCount() {
            final ContactDto contact = createContact("Alice", "A", null);
            commentService.addToContact(contact.id(), new CommentCreateDto("Comment 1"));
            commentService.addToContact(contact.id(), new CommentCreateDto("Comment 2"));

            final ContactDto result = contactService.getById(contact.id());

            assertEquals(2, result.commentCount());
        }

        @Test
        @DisplayName("should throw 404 for nonexistent ID")
        void shouldThrow404() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class, () -> contactService.getById(fakeId));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update all fields")
        void shouldUpdateAllFields() {
            final CompanyDto company = createCompany("Company A");
            final ContactDto contact = createContact("Old", "Name", null);

            final ContactDto updated = contactService.update(contact.id(),
                    new ContactUpdateDto("New", "Last", "new@test.com", "CTO",
                            Gender.FEMALE, "https://linkedin.com/in/new", "+49 999",
                            company.id(), Language.EN, java.time.LocalDate.of(1990, 3, 15)));

            assertEquals("New", updated.firstName());
            assertEquals("Last", updated.lastName());
            assertEquals("new@test.com", updated.email());
            assertEquals("CTO", updated.position());
            assertEquals(Gender.FEMALE, updated.gender());
            assertEquals("https://linkedin.com/in/new", updated.linkedInUrl());
            assertEquals("+49 999", updated.phoneNumber());
            assertEquals(company.id(), updated.companyId());
            assertEquals(Language.EN, updated.language());
            assertEquals(java.time.LocalDate.of(1990, 3, 15), updated.birthday());
        }

        @Test
        @DisplayName("should reject reassignment to soft-deleted company")
        void shouldRejectReassignmentToSoftDeletedCompany() {
            final CompanyDto company = createCompany("To Delete");
            final ContactDto contact = createContact("Jane", "Doe", null);
            companyService.delete(company.id());

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> contactService.update(contact.id(),
                            new ContactUpdateDto("Jane", "Doe", null, null, null, null, null,
                                    company.id(), null, null)));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("should throw 404 for nonexistent ID")
        void shouldThrow404() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> contactService.update(fakeId,
                            new ContactUpdateDto("Jane", "Doe", null, null, null, null, null, null, null, null)));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should hard-delete contact")
        void shouldHardDelete() {
            final ContactDto contact = createContact("To", "Delete", null);

            contactService.delete(contact.id());

            final var ex = assertThrows(ResponseStatusException.class, () -> contactService.getById(contact.id()));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("should cascade to comments")
        void shouldCascadeToComments() {
            final ContactDto contact = createContact("Has", "Comments", null);
            commentService.addToContact(contact.id(), new CommentCreateDto("Comment 1"));
            commentService.addToContact(contact.id(), new CommentCreateDto("Comment 2"));

            contactService.delete(contact.id());

            assertEquals(0, commentRepository.count());
        }

        @Test
        @DisplayName("should throw 404 for nonexistent ID")
        void shouldThrow404() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class, () -> contactService.delete(fakeId));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("list")
    class List {

        @Test
        @DisplayName("should return all contacts with no filter")
        void shouldReturnAllContactsNoFilter() {
            createContact("Alice", "A", null);
            createContact("Bob", "B", null);

            final var page = contactService.list(null, null, null, null, null, PageRequest.of(0, 20));

            assertEquals(2, page.getTotalElements());
        }

        @Test
        @DisplayName("should filter by firstName")
        void shouldFilterByFirstName() {
            createContact("Hendrik", "A", null);
            createContact("Hans", "B", null);

            final var page = contactService.list("Hendrik", null, null, null, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("Hendrik", page.getContent().get(0).firstName());
        }

        @Test
        @DisplayName("should filter by lastName")
        void shouldFilterByLastName() {
            createContact("A", "Ebbers", null);
            createContact("B", "Schmidt", null);

            final var page = contactService.list(null, "Ebbers", null, null, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("Ebbers", page.getContent().get(0).lastName());
        }

        @Test
        @DisplayName("should filter by email")
        void shouldFilterByEmail() {
            contactService.create(new ContactCreateDto("A", "A", "a@example.com", null, null, null, null, null, null, null));
            contactService.create(new ContactCreateDto("B", "B", "b@example.com", null, null, null, null, null, null, null));

            final var page = contactService.list(null, null, "a@example", null, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
        }

        @Test
        @DisplayName("should filter by companyId")
        void shouldFilterByCompanyId() {
            final CompanyDto companyA = createCompany("Company A");
            final CompanyDto companyB = createCompany("Company B");
            createContact("Alice", "A", companyA.id());
            createContact("Bob", "B", companyB.id());

            final var page = contactService.list(null, null, null, companyA.id(), null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("Alice", page.getContent().get(0).firstName());
        }

        @Test
        @DisplayName("should filter by language DE")
        void shouldFilterByLanguageDE() {
            contactService.create(new ContactCreateDto("DE", "Contact", null, null, null, null, null, null, Language.DE, null));
            contactService.create(new ContactCreateDto("EN", "Contact", null, null, null, null, null, null, Language.EN, null));

            final var page = contactService.list(null, null, null, null, "DE", PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("DE", page.getContent().get(0).firstName());
        }

        @Test
        @DisplayName("should filter by language UNKNOWN for null-language contacts")
        void shouldFilterByLanguageUnknown() {
            contactService.create(new ContactCreateDto("DE", "Contact", null, null, null, null, null, null, Language.DE, null));
            createContact("No", "Lang", null);

            final var page = contactService.list(null, null, null, null, "UNKNOWN", PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("No", page.getContent().get(0).firstName());
        }
    }

    @Nested
    @DisplayName("photo")
    class Photo {

        @Test
        @DisplayName("should store photo data as JPEG")
        void shouldStorePhotoData() {
            final ContactDto contact = createContact("Photo", "Person", null);
            final byte[] data = new byte[]{1, 2, 3, 4, 5};

            contactService.uploadPhoto(contact.id(), data, "image/jpeg");

            final ImageData photo = contactService.getPhoto(contact.id());
            assertArrayEquals(data, photo.data());
            assertEquals("image/jpeg", photo.contentType());
        }

        @Test
        @DisplayName("should reject non-JPEG content type")
        void shouldRejectNonJpeg() {
            final ContactDto contact = createContact("PNG", "Rejected", null);

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> contactService.uploadPhoto(contact.id(), new byte[]{1}, "image/png"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("should throw 404 for nonexistent contact")
        void shouldThrow404ForNonexistentContact() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> contactService.uploadPhoto(fakeId, new byte[]{1}, "image/jpeg"));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("should throw 404 when no photo exists")
        void shouldThrow404WhenNoPhotoExists() {
            final ContactDto contact = createContact("No", "Photo", null);

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> contactService.getPhoto(contact.id()));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("should remove photo data")
        void shouldRemovePhotoData() {
            final ContactDto contact = createContact("Delete", "Photo", null);
            contactService.uploadPhoto(contact.id(), new byte[]{1, 2, 3}, "image/jpeg");

            contactService.deletePhoto(contact.id());

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> contactService.getPhoto(contact.id()));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }
}
