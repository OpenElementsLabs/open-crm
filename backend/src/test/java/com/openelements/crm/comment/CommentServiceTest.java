package com.openelements.crm.comment;

import com.openelements.crm.company.CompanyCreateDto;
import com.openelements.crm.company.CompanyDto;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.company.CompanyService;
import com.openelements.crm.contact.ContactCreateDto;
import com.openelements.crm.contact.ContactDto;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.contact.ContactService;
import com.openelements.crm.TestSecurityUtil;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("CommentService")
class CommentServiceTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private ContactService contactService;

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
        return companyService.create(new CompanyCreateDto(name, null, null, null, null, null, null, null, null));
    }

    private ContactDto createContact(final String firstName, final String lastName) {
        return contactService.create(new ContactCreateDto(firstName, lastName, null, null, null, null, null, null, null, null));
    }

    @Nested
    @DisplayName("addToCompany")
    class AddToCompany {

        @Test
        @DisplayName("should create comment on company")
        void shouldCreateComment() {
            final CompanyDto company = createCompany("Commented Corp");

            final CommentDto result = commentService.addToCompany(company.id(), new CommentCreateDto("Great company"));

            assertNotNull(result.id());
            assertEquals("Great company", result.text());
            assertEquals(company.id(), result.companyId());
        }

        @Test
        @DisplayName("should throw 404 for nonexistent company")
        void shouldThrow404ForNonexistentCompany() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> commentService.addToCompany(fakeId, new CommentCreateDto("text")));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("should allow commenting on soft-deleted company")
        void shouldAllowCommentingOnSoftDeletedCompany() {
            final CompanyDto company = createCompany("Deleted Corp");
            companyService.delete(company.id());

            final CommentDto result = commentService.addToCompany(company.id(), new CommentCreateDto("Still commenting"));

            assertNotNull(result.id());
            assertEquals(company.id(), result.companyId());
        }
    }

    @Nested
    @DisplayName("addToContact")
    class AddToContact {

        @Test
        @DisplayName("should create comment on contact")
        void shouldCreateComment() {
            final ContactDto contact = createContact("Alice", "A");

            final CommentDto result = commentService.addToContact(contact.id(), new CommentCreateDto("Nice contact"));

            assertNotNull(result.id());
            assertEquals("Nice contact", result.text());
            assertEquals(contact.id(), result.contactId());
        }

        @Test
        @DisplayName("should throw 404 for nonexistent contact")
        void shouldThrow404ForNonexistentContact() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> commentService.addToContact(fakeId, new CommentCreateDto("text")));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update comment text")
        void shouldUpdateCommentText() {
            final CompanyDto company = createCompany("Corp");
            final CommentDto comment = commentService.addToCompany(company.id(), new CommentCreateDto("Original"));

            final CommentDto updated = commentService.update(comment.id(), new CommentUpdateDto("Updated"));

            assertEquals("Updated", updated.text());
        }

        @Test
        @DisplayName("should throw 404 for nonexistent comment")
        void shouldThrow404() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> commentService.update(fakeId, new CommentUpdateDto("text")));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should remove comment")
        void shouldRemoveComment() {
            final CompanyDto company = createCompany("Corp");
            final CommentDto comment = commentService.addToCompany(company.id(), new CommentCreateDto("To delete"));

            commentService.delete(comment.id());

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> commentService.update(comment.id(), new CommentUpdateDto("gone")));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("should throw 404 for nonexistent comment")
        void shouldThrow404() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class, () -> commentService.delete(fakeId));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("listByCompany")
    class ListByCompany {

        @Test
        @DisplayName("should return paginated comments for company")
        void shouldReturnPaginatedComments() {
            final CompanyDto company = createCompany("Commented Corp");
            commentService.addToCompany(company.id(), new CommentCreateDto("Comment 1"));
            commentService.addToCompany(company.id(), new CommentCreateDto("Comment 2"));

            final var page = commentService.listByCompany(company.id(), PageRequest.of(0, 20));

            assertEquals(2, page.getTotalElements());
        }

        @Test
        @DisplayName("should throw 404 for nonexistent company")
        void shouldThrow404ForNonexistentCompany() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> commentService.listByCompany(fakeId, PageRequest.of(0, 20)));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("listByContact")
    class ListByContact {

        @Test
        @DisplayName("should return paginated comments for contact")
        void shouldReturnPaginatedComments() {
            final ContactDto contact = createContact("Alice", "A");
            commentService.addToContact(contact.id(), new CommentCreateDto("Comment 1"));
            commentService.addToContact(contact.id(), new CommentCreateDto("Comment 2"));

            final var page = commentService.listByContact(contact.id(), PageRequest.of(0, 20));

            assertEquals(2, page.getTotalElements());
        }

        @Test
        @DisplayName("should throw 404 for nonexistent contact")
        void shouldThrow404ForNonexistentContact() {
            final UUID fakeId = UUID.randomUUID();

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> commentService.listByContact(fakeId, PageRequest.of(0, 20)));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }
}
