package com.openelements.crm.contact;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Spring Data repository for {@link ContactEntity} persistence operations.
 */
public interface ContactRepository extends JpaRepository<ContactEntity, UUID>,
        JpaSpecificationExecutor<ContactEntity> {

    /**
     * Checks if any contacts reference the given company.
     *
     * @param companyId the company ID
     * @return true if at least one contact references the company
     */
    boolean existsByCompanyId(UUID companyId);

    /**
     * Counts contacts associated with the given company.
     *
     * @param companyId the company ID
     * @return the number of contacts
     */
    long countByCompanyId(UUID companyId);
}
