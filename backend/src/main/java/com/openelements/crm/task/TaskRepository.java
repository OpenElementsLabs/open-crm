package com.openelements.crm.task;

import com.openelements.spring.base.data.EntityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TaskRepository extends EntityRepository<TaskEntity>, JpaSpecificationExecutor<TaskEntity> {

    Page<TaskEntity> findByCompanyId(UUID companyId, Pageable pageable);

    Page<TaskEntity> findByContactId(UUID contactId, Pageable pageable);

    long countByCompanyId(UUID companyId);

    long countByContactId(UUID contactId);

    void deleteByContactId(UUID contactId);

    void deleteByCompanyId(UUID companyId);
}
