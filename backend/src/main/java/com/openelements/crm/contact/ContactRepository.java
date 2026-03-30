package com.openelements.crm.contact;

import java.util.List;
import java.util.Optional;
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

    /**
     * Finds a contact by its Brevo contact ID.
     *
     * @param brevoId the Brevo contact ID
     * @return the contact, or empty if not found
     */
    Optional<ContactEntity> findByBrevoId(String brevoId);

    /**
     * Finds a contact by email (case-insensitive exact match).
     *
     * @param email the email address
     * @return the contact, or empty if not found
     */
    Optional<ContactEntity> findByEmailIgnoreCase(String email);

    List<ContactEntity> findAllByBrevoIdIsNotNull();
}
