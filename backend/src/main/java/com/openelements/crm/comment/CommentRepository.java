package com.openelements.crm.comment;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link CommentEntity} persistence operations.
 */
public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    /**
     * Finds all comments attached to the given company, paginated.
     *
     * @param companyId the company ID
     * @param pageable  pagination parameters
     * @return a page of comments
     */
    Page<CommentEntity> findByCompanyId(UUID companyId, Pageable pageable);

    /**
     * Finds all comments attached to the given contact, paginated.
     *
     * @param contactId the contact ID
     * @param pageable  pagination parameters
     * @return a page of comments
     */
    Page<CommentEntity> findByContactId(UUID contactId, Pageable pageable);

    /**
     * Deletes all comments attached to the given contact.
     *
     * @param contactId the contact ID
     */
    void deleteByContactId(UUID contactId);

    /**
     * Deletes all comments attached to the given company.
     *
     * @param companyId the company ID
     */
    void deleteByCompanyId(UUID companyId);

    /**
     * Counts all comments attached to the given company.
     *
     * @param companyId the company ID
     * @return the number of comments
     */
    long countByCompanyId(UUID companyId);

    /**
     * Counts all comments attached to the given contact.
     *
     * @param contactId the contact ID
     * @return the number of comments
     */
    long countByContactId(UUID contactId);
}
