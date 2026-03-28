package com.openelements.crm.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.openelements.crm.company.CompanyEntity;
import java.lang.reflect.Constructor;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ContactRepository")
class ContactRepositoryTest {

    @Autowired
    private ContactRepository contactRepository;

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

    @Nested
    @DisplayName("Save and retrieve")
    class SaveAndRetrieve {

        @Test
        @DisplayName("Saves and retrieves a contact")
        void savesAndRetrievesContact() throws Exception {
            // GIVEN
            final CompanyEntity company = createAndPersistCompany("Test Corp");
            final ContactEntity persisted = createAndPersistContact("John", "Doe", company);
            entityManager.clear();

            // WHEN
            final Optional<ContactEntity> found = contactRepository.findById(persisted.getId());

            // THEN
            assertThat(found).isPresent();
            assertThat(found.get().getFirstName()).isEqualTo("John");
            assertThat(found.get().getLastName()).isEqualTo("Doe");
            assertThat(found.get().getCompany()).isNotNull();
            assertThat(found.get().getCreatedAt()).isNotNull();
            assertThat(found.get().getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Rejects a contact without firstName")
        void rejectsContactWithoutFirstName() throws Exception {
            // GIVEN
            final CompanyEntity company = createAndPersistCompany("Test Corp");
            final Constructor<ContactEntity> constructor = ContactEntity.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            final ContactEntity entity = constructor.newInstance();
            entity.setLastName("Doe");
            entity.setCompany(company);

            // WHEN / THEN
            assertThrows(Exception.class, () -> entityManager.persistAndFlush(entity));
        }

        @Test
        @DisplayName("Rejects a contact without lastName")
        void rejectsContactWithoutLastName() throws Exception {
            // GIVEN
            final CompanyEntity company = createAndPersistCompany("Test Corp");
            final Constructor<ContactEntity> constructor = ContactEntity.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            final ContactEntity entity = constructor.newInstance();
            entity.setFirstName("John");
            entity.setCompany(company);

            // WHEN / THEN
            assertThrows(Exception.class, () -> entityManager.persistAndFlush(entity));
        }
    }

    @Nested
    @DisplayName("existsByCompanyId")
    class ExistsByCompanyId {

        @Test
        @DisplayName("existsByCompanyId returns true when contacts reference the company")
        void returnsTrueWhenContactsExist() throws Exception {
            // GIVEN
            final CompanyEntity company = createAndPersistCompany("Test Corp");
            createAndPersistContact("John", "Doe", company);

            // WHEN / THEN
            assertThat(contactRepository.existsByCompanyId(company.getId())).isTrue();
        }

        @Test
        @DisplayName("existsByCompanyId returns false when no contacts reference the company")
        void returnsFalseWhenNoContactsExist() throws Exception {
            // GIVEN
            final CompanyEntity company = createAndPersistCompany("Empty Corp");

            // WHEN / THEN
            assertThat(contactRepository.existsByCompanyId(company.getId())).isFalse();
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
            createAndPersistContact("Alice", "Smith", company);
            createAndPersistContact("Bob", "Jones", company);
            createAndPersistContact("Carol", "White", company);

            // WHEN / THEN
            assertThat(contactRepository.countByCompanyId(company.getId())).isEqualTo(3);
        }

        @Test
        @DisplayName("countByCompanyId returns zero for company with no contacts")
        void returnsZeroForCompanyWithNoContacts() throws Exception {
            // GIVEN
            final CompanyEntity company = createAndPersistCompany("Empty Corp");

            // WHEN / THEN
            assertThat(contactRepository.countByCompanyId(company.getId())).isZero();
        }
    }
}
