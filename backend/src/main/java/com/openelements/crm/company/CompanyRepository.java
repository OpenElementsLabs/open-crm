package com.openelements.crm.company;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Spring Data repository for {@link CompanyEntity} persistence operations.
 */
public interface CompanyRepository extends JpaRepository<CompanyEntity, UUID>,
        JpaSpecificationExecutor<CompanyEntity> {

    /**
     * Finds a company by its Brevo CRM company ID.
     *
     * @param brevoCompanyId the Brevo company ID
     * @return the company, or empty if not found
     */
    Optional<CompanyEntity> findByBrevoCompanyId(String brevoCompanyId);

    /**
     * Finds a company by name (case-insensitive exact match).
     *
     * @param name the company name
     * @return the company, or empty if not found
     */
    Optional<CompanyEntity> findByNameIgnoreCase(String name);
}
