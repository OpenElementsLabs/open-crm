package com.openelements.crm.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.contact.ContactEntity;
import java.lang.reflect.Constructor;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CommentRepository")
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private CompanyEntity createAndPersistCompany(final String name) throws Exception {
        final Constructor<CompanyEntity> constructor = CompanyEntity.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        final CompanyEntity entity = constructor.newInstance();
        entity.setName(name);
        return entityManager.persistAndFlush(entity);
    }

    private ContactEntity createAndPersistContact(final String firstName, final String lastName,
            final CompanyEntity company) throws Exception {
        final Constructor<ContactEntity> constructor = ContactEntity.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        final ContactEntity entity = constructor.newInstance();
        entity.setFirstName(firstName);
        entity.setLastName(lastName);
        entity.setCompany(company);
        return entityManager.persistAndFlush(entity);
    }

    private CommentEntity createAndPersistCompanyComment(final String text, final String author,
            final CompanyEntity company) throws Exception {
        final Constructor<CommentEntity> constructor = CommentEntity.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        final CommentEntity entity = constructor.newInstance();
        entity.setText(text);
        entity.setAuthor(author);
        entity.setCompany(company);
        return entityManager.persistAndFlush(entity);
    }

    private CommentEntity createAndPersistContactComment(final String text, final String author,
            final ContactEntity contact) throws Exception {
        final Constructor<CommentEntity> constructor = CommentEntity.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        final CommentEntity entity = constructor.newInstance();
        entity.setText(text);
        entity.setAuthor(author);
        entity.setContact(contact);
        return entityManager.persistAndFlush(entity);
    }

    @Nested
    @DisplayName("Save and retrieve")
    class SaveAndRetrieve {

        @Test
        @DisplayName("Saves and retrieves a comment")
        void savesAndRetrievesComment() throws Exception {
            // GIVEN
            final CompanyEntity company = createAndPersistCompany("Test Corp");
            final CommentEntity persisted = createAndPersistCompanyComment("Great company", "Admin", company);
            entityManager.clear();

            // WHEN
            final Optional<CommentEntity> found = commentRepository.findById(persisted.getId());

            // THEN
            assertThat(found).isPresent();
            assertThat(found.get().getText()).isEqualTo("Great company");
            assertThat(found.get().getAuthor()).isEqualTo("Admin");
            assertThat(found.get().getCreatedAt()).isNotNull();
            assertThat(found.get().getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Rejects a comment without text")
        void rejectsCommentWithoutText() throws Exception {
            // GIVEN
            final CompanyEntity company = createAndPersistCompany("Test Corp");
            final Constructor<CommentEntity> constructor = CommentEntity.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            final CommentEntity entity = constructor.newInstance();
            entity.setAuthor("Admin");
            entity.setCompany(company);

            // WHEN / THEN
            assertThrows(Exception.class, () -> entityManager.persistAndFlush(entity));
        }

        @Test
        @DisplayName("Rejects a comment without author")
        void rejectsCommentWithoutAuthor() throws Exception {
            // GIVEN
            final CompanyEntity company = createAndPersistCompany("Test Corp");
            final Constructor<CommentEntity> constructor = CommentEntity.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            final CommentEntity entity = constructor.newInstance();
            entity.setText("Some text");
            entity.setCompany(company);

            // WHEN / THEN
            assertThrows(Exception.class, () -> entityManager.persistAndFlush(entity));
        }
    }

    @Nested
    @DisplayName("findByCompanyId")
    class FindByCompanyId {

        @Test
        @DisplayName("findByCompanyId returns comments for the given company")
        void returnsCommentsForCompany() throws Exception {
            // GIVEN
            final CompanyEntity companyA = createAndPersistCompany("Company A");
            final CompanyEntity companyB = createAndPersistCompany("Company B");
            createAndPersistCompanyComment("Comment 1", "Admin", companyA);
            createAndPersistCompanyComment("Comment 2", "Admin", companyA);
            createAndPersistCompanyComment("Comment 3", "Admin", companyA);
            createAndPersistCompanyComment("Comment 4", "Admin", companyB);
            createAndPersistCompanyComment("Comment 5", "Admin", companyB);

            // WHEN
            final Page<CommentEntity> page = commentRepository.findByCompanyId(
                    companyA.getId(), PageRequest.of(0, 10));

            // THEN
            assertThat(page.getContent()).hasSize(3);
        }

        @Test
        @DisplayName("findByCompanyId returns empty page for company with no comments")
        void returnsEmptyPageForCompanyWithNoComments() throws Exception {
            // GIVEN
            final CompanyEntity company = createAndPersistCompany("Empty Corp");

            // WHEN
            final Page<CommentEntity> page = commentRepository.findByCompanyId(
                    company.getId(), PageRequest.of(0, 10));

            // THEN
            assertThat(page.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByContactId")
    class FindByContactId {

        @Test
        @DisplayName("findByContactId returns comments for the given contact")
        void returnsCommentsForContact() throws Exception {
            // GIVEN
            final CompanyEntity company = createAndPersistCompany("Test Corp");
            final ContactEntity contact = createAndPersistContact("John", "Doe", company);
            createAndPersistContactComment("Comment 1", "Admin", contact);
            createAndPersistContactComment("Comment 2", "Admin", contact);

            // WHEN
            final Page<CommentEntity> page = commentRepository.findByContactId(
                    contact.getId(), PageRequest.of(0, 10));

            // THEN
            assertThat(page.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("deleteByContactId")
    class DeleteByContactId {

        @Test
        @Transactional
        @DisplayName("deleteByContactId removes all comments for the contact")
        void removesAllCommentsForContact() throws Exception {
            // GIVEN
            final CompanyEntity company = createAndPersistCompany("Test Corp");
            final ContactEntity contactA = createAndPersistContact("Alice", "Smith", company);
            final ContactEntity contactB = createAndPersistContact("Bob", "Jones", company);
            createAndPersistContactComment("Comment 1", "Admin", contactA);
            createAndPersistContactComment("Comment 2", "Admin", contactA);
            createAndPersistContactComment("Comment 3", "Admin", contactA);
            createAndPersistContactComment("Comment 4", "Admin", contactB);
            createAndPersistContactComment("Comment 5", "Admin", contactB);

            // WHEN
            commentRepository.deleteByContactId(contactA.getId());
            entityManager.flush();
            entityManager.clear();

            // THEN
            final Page<CommentEntity> contactAComments = commentRepository.findByContactId(
                    contactA.getId(), PageRequest.of(0, 10));
            assertThat(contactAComments.getContent()).isEmpty();

            final Page<CommentEntity> contactBComments = commentRepository.findByContactId(
                    contactB.getId(), PageRequest.of(0, 10));
            assertThat(contactBComments.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("countByCompanyId")
    class CountByCompanyId {

        @Test
        @DisplayName("countByCompanyId returns correct count")
        void returnsCorrectCount() throws Exception {
            // GIVEN
            final CompanyEntity company = createAndPersistCompany("Test Corp");
            createAndPersistCompanyComment("Comment 1", "Admin", company);
            createAndPersistCompanyComment("Comment 2", "Admin", company);
            createAndPersistCompanyComment("Comment 3", "Admin", company);
            createAndPersistCompanyComment("Comment 4", "Admin", company);

            // WHEN / THEN
            assertThat(commentRepository.countByCompanyId(company.getId())).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("countByContactId")
    class CountByContactId {

        @Test
        @DisplayName("countByContactId returns correct count")
        void returnsCorrectCount() throws Exception {
            // GIVEN
            final CompanyEntity company = createAndPersistCompany("Test Corp");
            final ContactEntity contact = createAndPersistContact("John", "Doe", company);
            createAndPersistContactComment("Comment 1", "Admin", contact);
            createAndPersistContactComment("Comment 2", "Admin", contact);

            // WHEN / THEN
            assertThat(commentRepository.countByContactId(contact.getId())).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Pagination")
    class Pagination {

        @Test
        @DisplayName("findByCompanyId respects pagination")
        void respectsPagination() throws Exception {
            // GIVEN
            final CompanyEntity company = createAndPersistCompany("Test Corp");
            for (int i = 1; i <= 5; i++) {
                createAndPersistCompanyComment("Comment " + i, "Admin", company);
            }

            // WHEN
            final Page<CommentEntity> page = commentRepository.findByCompanyId(
                    company.getId(), PageRequest.of(0, 2));

            // THEN
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(3);
        }
    }
}
