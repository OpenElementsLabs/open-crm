package com.openelements.crm.contact;

import com.openelements.crm.ImageData;
import com.openelements.crm.TestSecurityUtil;
import com.openelements.crm.comment.CommentCreateDto;
import com.openelements.crm.comment.CommentRepository;
import com.openelements.crm.comment.CommentService;
import com.openelements.crm.company.CompanyCreateDto;
import com.openelements.crm.company.CompanyDto;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.company.CompanyService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
        TestSecurityUtil.setSecurityContext();
    }

    @AfterEach
    void tearDown() {
        TestSecurityUtil.clearSecurityContext();
    }

    private CompanyDto createCompany(final String name) {
        return companyService.create(new CompanyCreateDto(name, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
    }

    private ContactDto createContact(final String firstName, final String lastName, final UUID companyId) {
        return contactService.create(new ContactCreateDto(null, firstName, lastName, null, null, null, null, null, companyId, null, null, null, null));
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
        @DisplayName("should create contact with description")
        void shouldCreateWithDescription() {
            final ContactDto result = contactService.create(
                    new ContactCreateDto(null, "Jane", "Doe", null, null, null, null, null, null, null, null, "A key contact", null));

            assertEquals("A key contact", result.description());
        }

        @Test
        @DisplayName("should create contact without description")
        void shouldCreateWithoutDescription() {
            final ContactDto result = createContact("Jane", "Doe", null);

            assertNull(result.description());
        }

        @Test
        @DisplayName("should throw 400 for deleted company")
        void shouldThrow400ForDeletedCompany() {
            final CompanyDto company = createCompany("Deleted Co");
            companyService.delete(company.id(), false);

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
                    new ContactUpdateDto(null, "New", "Last", "new@test.com", "CTO",
                            Gender.FEMALE, java.util.List.of(new SocialLinkCreateDto("LINKEDIN", "new")), "+49 999",
                            company.id(), Language.EN, java.time.LocalDate.of(1990, 3, 15), null, null));

            assertEquals("New", updated.firstName());
            assertEquals("Last", updated.lastName());
            assertEquals("new@test.com", updated.email());
            assertEquals("CTO", updated.position());
            assertEquals(Gender.FEMALE, updated.gender());
            assertEquals(1, updated.socialLinks().size());
            assertEquals("LINKEDIN", updated.socialLinks().get(0).networkType());
            assertEquals("+49 999", updated.phoneNumber());
            assertEquals(company.id(), updated.companyId());
            assertEquals(Language.EN, updated.language());
            assertEquals(java.time.LocalDate.of(1990, 3, 15), updated.birthday());
        }

        @Test
        @DisplayName("should update description")
        void shouldUpdateDescription() {
            final ContactDto contact = createContact("Jane", "Doe", null);

            final ContactDto updated = contactService.update(contact.id(),
                    new ContactUpdateDto(null, "Jane", "Doe", null, null, null, null, null, null, null, null, "Updated desc", null));

            assertEquals("Updated desc", updated.description());
        }

        @Test
        @DisplayName("should clear description when set to null")
        void shouldClearDescription() {
            final ContactDto contact = contactService.create(
                    new ContactCreateDto(null, "Jane", "Doe", null, null, null, null, null, null, null, null, "Initial desc", null));

            final ContactDto updated = contactService.update(contact.id(),
                    new ContactUpdateDto(null, "Jane", "Doe", null, null, null, null, null, null, null, null, null, null));

            assertNull(updated.description());
        }

        @Test
        @DisplayName("should reject reassignment to deleted company")
        void shouldRejectReassignmentToDeletedCompany() {
            final CompanyDto company = createCompany("To Delete");
            final ContactDto contact = createContact("Jane", "Doe", null);
            companyService.delete(company.id(), false);

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> contactService.update(contact.id(),
                            new ContactUpdateDto(null, "Jane", "Doe", null, null, null, null, null,
                                    company.id(), null, null, null, null)));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("should throw 404 for nonexistent ID")
        void shouldThrow404() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> contactService.update(fakeId,
                            new ContactUpdateDto(null, "Jane", "Doe", null, null, null, null, null, null, null, null, null, null)));
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

            final var page = contactService.list(null, null, false, null, null, null, PageRequest.of(0, 20));

            assertEquals(2, page.getTotalElements());
        }

        @Test
        @DisplayName("searches by firstName")
        void searchesByFirstName() {
            createContact("Hendrik", "A", null);
            createContact("Hans", "B", null);

            final var page = contactService.list("ali", null, false, null, null, null, PageRequest.of(0, 20));

            // "ali" does not match either contact
            assertEquals(0, page.getTotalElements());

            final var page2 = contactService.list("Hendrik", null, false, null, null, null, PageRequest.of(0, 20));

            assertEquals(1, page2.getTotalElements());
            assertEquals("Hendrik", page2.getContent().get(0).firstName());
        }

        @Test
        @DisplayName("searches by lastName")
        void searchesByLastName() {
            createContact("A", "Ebbers", null);
            createContact("B", "Schmidt", null);

            final var page = contactService.list("Ebbers", null, false, null, null, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("Ebbers", page.getContent().get(0).lastName());
        }

        @Test
        @DisplayName("searches by email")
        void searchesByEmail() {
            contactService.create(new ContactCreateDto(null, "A", "A", "a@example.com", null, null, null, null, null, null, null, null, null));
            contactService.create(new ContactCreateDto(null, "B", "B", "b@example.com", null, null, null, null, null, null, null, null, null));

            final var page = contactService.list("a@example", null, false, null, null, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
        }

        @Test
        @DisplayName("searches across company name")
        void searchesAcrossCompanyName() {
            final CompanyDto company = createCompany("Acme");
            createContact("John", "Doe", company.id());
            createContact("Jane", "Smith", null);

            final var page = contactService.list("Acme", null, false, null, null, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("John", page.getContent().get(0).firstName());
        }

        @Test
        @DisplayName("multi-word search ANDs across fields")
        void multiWordSearchAndsAcrossFields() {
            createContact("Anna", "Schmidt", null);
            createContact("Anna", "Mueller", null);

            final var page = contactService.list("Anna Schmidt", null, false, null, null, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("Schmidt", page.getContent().get(0).lastName());
        }

        @Test
        @DisplayName("search finds contacts without company by name")
        void searchFindsContactsWithoutCompanyByName() {
            createContact("Solo", "Person", null);
            final CompanyDto company = createCompany("BigCorp");
            createContact("Corp", "Employee", company.id());

            final var page = contactService.list("Solo", null, false, null, null, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("Solo", page.getContent().get(0).firstName());
        }

        @Test
        @DisplayName("should filter by companyId")
        void shouldFilterByCompanyId() {
            final CompanyDto companyA = createCompany("Company A");
            final CompanyDto companyB = createCompany("Company B");
            createContact("Alice", "A", companyA.id());
            createContact("Bob", "B", companyB.id());

            final var page = contactService.list(null, companyA.id(), false, null, null, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("Alice", page.getContent().get(0).firstName());
        }

        @Test
        @DisplayName("should filter by language DE")
        void shouldFilterByLanguageDE() {
            contactService.create(new ContactCreateDto(null, "DE", "Contact", null, null, null, null, null, null, Language.DE, null, null, null));
            contactService.create(new ContactCreateDto(null, "EN", "Contact", null, null, null, null, null, null, Language.EN, null, null, null));

            final var page = contactService.list(null, null, false, "DE", null, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("DE", page.getContent().get(0).firstName());
        }

        @Test
        @DisplayName("should filter by language UNKNOWN for null-language contacts")
        void shouldFilterByLanguageUnknown() {
            contactService.create(new ContactCreateDto(null, "DE", "Contact", null, null, null, null, null, null, Language.DE, null, null, null));
            createContact("No", "Lang", null);

            final var page = contactService.list(null, null, false, "UNKNOWN", null, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("No", page.getContent().get(0).firstName());
        }

        @Test
        @DisplayName("filters by brevo true")
        void filtersByBrevoTrue() {
            createContact("Normal", "Contact", null);
            final ContactEntity brevoEntity = new ContactEntity();
            brevoEntity.setFirstName("Brevo");
            brevoEntity.setLastName("Contact");
            brevoEntity.setBrevoId("brevo-456");
            contactRepository.saveAndFlush(brevoEntity);

            final var page = contactService.list(null, null, false, null, true, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("Brevo", page.getContent().get(0).firstName());
        }

        @Test
        @DisplayName("filters by brevo false")
        void filtersByBrevoFalse() {
            createContact("Normal", "Contact", null);
            final ContactEntity brevoEntity = new ContactEntity();
            brevoEntity.setFirstName("Brevo");
            brevoEntity.setLastName("Contact");
            brevoEntity.setBrevoId("brevo-456");
            contactRepository.saveAndFlush(brevoEntity);

            final var page = contactService.list(null, null, false, null, false, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("Normal", page.getContent().get(0).firstName());
        }
    }

    @Nested
    @DisplayName("BrevoFieldProtection")
    class BrevoFieldProtection {

        @Test
        @DisplayName("update rejects changed firstName on Brevo contact")
        void updateRejectsChangedFirstNameOnBrevoContact() {
            final ContactDto contact = createContact("Original", "Last", null);
            final ContactEntity entity = contactRepository.findById(contact.id()).orElseThrow();
            entity.setBrevoId("200");
            contactRepository.saveAndFlush(entity);

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> contactService.update(contact.id(),
                            new ContactUpdateDto(null, "Changed", "Last", null, null, null, null, null, null, null, null, null, null)));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("update rejects changed email on Brevo contact")
        void updateRejectsChangedEmailOnBrevoContact() {
            final ContactDto contact = contactService.create(
                    new ContactCreateDto(null, "Jane", "Doe", "old@test.com", null, null, null, null, null, null, null, null, null));
            final ContactEntity entity = contactRepository.findById(contact.id()).orElseThrow();
            entity.setBrevoId("200");
            contactRepository.saveAndFlush(entity);

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> contactService.update(contact.id(),
                            new ContactUpdateDto(null, "Jane", "Doe", "new@test.com", null, null, null, null, null, null, null, null, null)));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("update rejects changed language on Brevo contact")
        void updateRejectsChangedLanguageOnBrevoContact() {
            final ContactDto contact = contactService.create(
                    new ContactCreateDto(null, "Jane", "Doe", null, null, null, null, null, null, Language.DE, null, null, null));
            final ContactEntity entity = contactRepository.findById(contact.id()).orElseThrow();
            entity.setBrevoId("200");
            contactRepository.saveAndFlush(entity);

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> contactService.update(contact.id(),
                            new ContactUpdateDto(null, "Jane", "Doe", null, null, null, null, null, null, Language.EN, null, null, null)));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("update lists all violated fields")
        void updateListsAllViolatedFields() {
            final ContactDto contact = contactService.create(
                    new ContactCreateDto(null, "Jane", "Doe", "old@test.com", null, null, null, null, null, null, null, null, null));
            final ContactEntity entity = contactRepository.findById(contact.id()).orElseThrow();
            entity.setBrevoId("200");
            contactRepository.saveAndFlush(entity);

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> contactService.update(contact.id(),
                            new ContactUpdateDto(null, "Changed", "Doe", "new@test.com", null, null, null, null, null, null, null, null, null)));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertNotNull(ex.getReason());
            assert ex.getReason().contains("firstName");
            assert ex.getReason().contains("email");
        }

        @Test
        @DisplayName("update succeeds with unchanged protected fields on Brevo contact")
        void updateSucceedsWithUnchangedProtectedFieldsOnBrevoContact() {
            final ContactDto contact = contactService.create(
                    new ContactCreateDto(null, "Jane", "Doe", "jane@test.com", "Dev", null, null, null, null, Language.DE, null, null, null));
            final ContactEntity entity = contactRepository.findById(contact.id()).orElseThrow();
            entity.setBrevoId("200");
            contactRepository.saveAndFlush(entity);

            final ContactDto updated = contactService.update(contact.id(),
                    new ContactUpdateDto(null, "Jane", "Doe", "jane@test.com", "Manager", null, null, null, null, Language.DE, null, null, null));

            assertEquals("Manager", updated.position());
        }

        @Test
        @DisplayName("update allows all changes on non-Brevo contact")
        void updateAllowsAllChangesOnNonBrevoContact() {
            final ContactDto contact = createContact("Original", "Last", null);

            final ContactDto updated = contactService.update(contact.id(),
                    new ContactUpdateDto(null, "Changed", "Last", null, null, null, null, null, null, null, null, null, null));

            assertEquals("Changed", updated.firstName());
        }

        @Test
        @DisplayName("update succeeds when null email unchanged on Brevo contact")
        void updateSucceedsWhenNullEmailUnchangedOnBrevoContact() {
            final ContactDto contact = createContact("Jane", "Doe", null);
            final ContactEntity entity = contactRepository.findById(contact.id()).orElseThrow();
            entity.setBrevoId("200");
            contactRepository.saveAndFlush(entity);

            final ContactDto updated = contactService.update(contact.id(),
                    new ContactUpdateDto(null, "Jane", "Doe", null, "Manager", null, null, null, null, null, null, null, null));

            assertEquals("Manager", updated.position());
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

    @Nested
    @DisplayName("SocialLinks")
    class SocialLinks {

        @Test
        @DisplayName("should create contact with social links")
        void shouldCreateWithSocialLinks() {
            final ContactDto result = contactService.create(
                    new ContactCreateDto(null, "Jane", "Doe", null, null, null,
                            java.util.List.of(new SocialLinkCreateDto("GITHUB", "hendrikebbers")),
                            null, null, null, null, null, null));

            assertNotNull(result.socialLinks());
            assertEquals(1, result.socialLinks().size());
            assertEquals("GITHUB", result.socialLinks().get(0).networkType());
            assertEquals("hendrikebbers", result.socialLinks().get(0).value());
            assertEquals("https://github.com/hendrikebbers", result.socialLinks().get(0).url());
        }

        @Test
        @DisplayName("should create contact with multiple links same network")
        void shouldCreateWithMultipleLinksSameNetwork() {
            final ContactDto result = contactService.create(
                    new ContactCreateDto(null, "Jane", "Doe", null, null, null,
                            java.util.List.of(
                                    new SocialLinkCreateDto("GITHUB", "hendrikebbers"),
                                    new SocialLinkCreateDto("GITHUB", "janedev")
                            ),
                            null, null, null, null, null, null));

            assertEquals(2, result.socialLinks().size());
            assertEquals("GITHUB", result.socialLinks().get(0).networkType());
            assertEquals("GITHUB", result.socialLinks().get(1).networkType());
        }

        @Test
        @DisplayName("should create contact with links across networks")
        void shouldCreateWithLinksAcrossNetworks() {
            final ContactDto result = contactService.create(
                    new ContactCreateDto(null, "Jane", "Doe", null, null, null,
                            java.util.List.of(
                                    new SocialLinkCreateDto("GITHUB", "hendrikebbers"),
                                    new SocialLinkCreateDto("LINKEDIN", "hendrik-ebbers"),
                                    new SocialLinkCreateDto("WEBSITE", "https://open-elements.com")
                            ),
                            null, null, null, null, null, null));

            assertEquals(3, result.socialLinks().size());
        }

        @Test
        @DisplayName("should create contact with no social links")
        void shouldCreateWithNoSocialLinks() {
            final ContactDto result = contactService.create(
                    new ContactCreateDto(null, "Jane", "Doe", null, null, null,
                            java.util.List.of(),
                            null, null, null, null, null, null));

            assertNotNull(result.socialLinks());
            assertEquals(0, result.socialLinks().size());
        }

        @Test
        @DisplayName("should replace all social links on update")
        void shouldReplaceAllSocialLinksOnUpdate() {
            final ContactDto contact = contactService.create(
                    new ContactCreateDto(null, "Jane", "Doe", null, null, null,
                            java.util.List.of(new SocialLinkCreateDto("GITHUB", "hendrikebbers")),
                            null, null, null, null, null, null));

            final ContactDto updated = contactService.update(contact.id(),
                    new ContactUpdateDto(null, "Jane", "Doe", null, null, null,
                            java.util.List.of(new SocialLinkCreateDto("LINKEDIN", "hendrik-ebbers")),
                            null, null, null, null, null, null));

            assertEquals(1, updated.socialLinks().size());
            assertEquals("LINKEDIN", updated.socialLinks().get(0).networkType());
            assertEquals("hendrik-ebbers", updated.socialLinks().get(0).value());
        }

        @Test
        @DisplayName("should preserve social links when null")
        void shouldPreserveSocialLinksWhenNull() {
            final ContactDto contact = contactService.create(
                    new ContactCreateDto(null, "Jane", "Doe", null, null, null,
                            java.util.List.of(new SocialLinkCreateDto("GITHUB", "hendrikebbers")),
                            null, null, null, null, null, null));

            final ContactDto updated = contactService.update(contact.id(),
                    new ContactUpdateDto(null, "Jane", "Doe", null, null, null,
                            null,
                            null, null, null, null, null, null));

            assertEquals(1, updated.socialLinks().size());
            assertEquals("GITHUB", updated.socialLinks().get(0).networkType());
        }

        @Test
        @DisplayName("should clear social links with empty list")
        void shouldClearSocialLinksWithEmptyList() {
            final ContactDto contact = contactService.create(
                    new ContactCreateDto(null, "Jane", "Doe", null, null, null,
                            java.util.List.of(new SocialLinkCreateDto("GITHUB", "hendrikebbers")),
                            null, null, null, null, null, null));

            final ContactDto updated = contactService.update(contact.id(),
                    new ContactUpdateDto(null, "Jane", "Doe", null, null, null,
                            java.util.List.of(),
                            null, null, null, null, null, null));

            assertEquals(0, updated.socialLinks().size());
        }

        @Test
        @DisplayName("should delete contact and cascade social links")
        void shouldDeleteContactAndCascadeSocialLinks() {
            final ContactDto contact = contactService.create(
                    new ContactCreateDto(null, "Jane", "Doe", null, null, null,
                            java.util.List.of(new SocialLinkCreateDto("GITHUB", "hendrikebbers"),
                                    new SocialLinkCreateDto("LINKEDIN", "hendrik-ebbers")),
                            null, null, null, null, null, null));

            contactService.delete(contact.id());

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> contactService.getById(contact.id()));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("should find contact by social link value")
        void shouldFindContactBySocialLinkValue() {
            contactService.create(
                    new ContactCreateDto(null, "Jane", "Doe", null, null, null,
                            java.util.List.of(new SocialLinkCreateDto("GITHUB", "hendrikebbers")),
                            null, null, null, null, null, null));
            createContact("Other", "Person", null);

            final var page = contactService.list("hendrikebbers", null, false, null, null, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("Jane", page.getContent().get(0).firstName());
        }

        @Test
        @DisplayName("should find contact by partial social link value")
        void shouldFindContactByPartialSocialLinkValue() {
            contactService.create(
                    new ContactCreateDto(null, "Jane", "Doe", null, null, null,
                            java.util.List.of(new SocialLinkCreateDto("GITHUB", "hendrikebbers")),
                            null, null, null, null, null, null));
            createContact("Other", "Person", null);

            final var page = contactService.list("hendrik", null, false, null, null, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("Jane", page.getContent().get(0).firstName());
        }

        @Test
        @DisplayName("should reject social link update for Brevo contacts")
        void shouldRejectSocialLinkUpdateForBrevoContacts() {
            final ContactDto contact = createContact("Jane", "Doe", null);
            final ContactEntity entity = contactRepository.findById(contact.id()).orElseThrow();
            entity.setBrevoId("200");
            contactRepository.saveAndFlush(entity);

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> contactService.update(contact.id(),
                            new ContactUpdateDto(null, "Jane", "Doe", null, null, null,
                                    java.util.List.of(new SocialLinkCreateDto("GITHUB", "hendrikebbers")),
                                    null, null, null, null, null, null)));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertNotNull(ex.getReason());
            assert ex.getReason().contains("socialLinks");
        }
    }
}
