package com.openelements.crm.opportunity;

import com.openelements.spring.base.data.EntityRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Spring Data repository for {@link OpportunityEntity} persistence operations.
 */
public interface OpportunityRepository extends EntityRepository<OpportunityEntity>,
        JpaSpecificationExecutor<OpportunityEntity> {

    /**
     * @param companyId the company ID
     * @return {@code true} if at least one opportunity references the given company
     */
    boolean existsByCompanyId(UUID companyId);

    /**
     * @param contactId the contact ID
     * @return {@code true} if at least one opportunity has the given contact as its main contact
     */
    boolean existsByMainContactId(UUID contactId);
}
