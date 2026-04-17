package com.openelements.crm.comment;

import com.openelements.spring.base.data.EntityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Spring Data repository for {@link CommentEntity} persistence operations.
 */
public interface CommentRepository extends EntityRepository<CommentEntity> {

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

    /**
     * Finds all comments attached to the given task, paginated.
     *
     * @param taskId   the task ID
     * @param pageable pagination parameters
     * @return a page of comments
     */
    Page<CommentEntity> findByTaskId(UUID taskId, Pageable pageable);

    /**
     * Counts all comments attached to the given task.
     *
     * @param taskId the task ID
     * @return the number of comments
     */
    long countByTaskId(UUID taskId);
}
