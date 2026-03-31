package com.openelements.crm.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CompanyRepository")
class CompanyRepositoryTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private TestEntityManager entityManager;

    private CompanyEntity createAndPersistCompany(final String name) throws Exception {
        final Constructor<CompanyEntity> constructor = CompanyEntity.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        final CompanyEntity entity = constructor.newInstance();
        entity.setName(name);
        return entityManager.persistAndFlush(entity);
    }

    @Nested
    @DisplayName("Save and retrieve")
    class SaveAndRetrieve {

        @Test
        @DisplayName("Saves and retrieves a company")
        void savesAndRetrievesCompany() throws Exception {
            // GIVEN
            final CompanyEntity persisted = createAndPersistCompany("Open Elements GmbH");
            entityManager.clear();

            // WHEN
            final Optional<CompanyEntity> found = companyRepository.findById(persisted.getId());

            // THEN
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Open Elements GmbH");
            assertThat(found.get().getCreatedAt()).isNotNull();
            assertThat(found.get().getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Rejects a company without a name")
        void rejectsCompanyWithoutName() throws Exception {
            // GIVEN
            final Constructor<CompanyEntity> constructor = CompanyEntity.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            final CompanyEntity entity = constructor.newInstance();

            // WHEN / THEN
            assertThrows(Exception.class, () -> entityManager.persistAndFlush(entity));
        }
    }

    @Nested
    @DisplayName("Find all")
    class FindAll {

        @Test
        @DisplayName("Finds all companies")
        void findsAllCompanies() throws Exception {
            // GIVEN
            createAndPersistCompany("Company A");
            createAndPersistCompany("Company B");
            createAndPersistCompany("Company C");

            // WHEN
            final List<CompanyEntity> all = companyRepository.findAll();

            // THEN
            assertThat(all).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Delete")
    class Delete {

        @Test
        @DisplayName("Deletes a company by ID")
        void deletesCompanyById() throws Exception {
            // GIVEN
            final CompanyEntity persisted = createAndPersistCompany("To Delete");

            // WHEN
            companyRepository.deleteById(persisted.getId());
            companyRepository.flush();

            // THEN
            final Optional<CompanyEntity> found = companyRepository.findById(persisted.getId());
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Specification queries")
    class SpecificationQueries {

        @Test
        @DisplayName("Supports Specification-based queries")
        void supportsSpecificationBasedQueries() throws Exception {
            // GIVEN
            createAndPersistCompany("Alpha");
            createAndPersistCompany("Beta");

            // WHEN
            final Specification<CompanyEntity> spec = (root, query, cb) ->
                    cb.like(root.get("name"), "%lph%");
            final List<CompanyEntity> results = companyRepository.findAll(spec);

            // THEN
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("Alpha");
        }
    }

    @Nested
    @DisplayName("Exists by ID")
    class ExistsById {

        @Test
        @DisplayName("existsById returns false for nonexistent ID")
        void existsByIdReturnsFalseForNonexistentId() {
            // WHEN / THEN
            assertThat(companyRepository.existsById(UUID.randomUUID())).isFalse();
        }
    }
}
