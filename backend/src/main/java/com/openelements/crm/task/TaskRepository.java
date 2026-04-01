package com.openelements.crm.task;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskRepository extends JpaRepository<TaskEntity, UUID>, JpaSpecificationExecutor<TaskEntity> {

    Page<TaskEntity> findByCompanyId(UUID companyId, Pageable pageable);

    Page<TaskEntity> findByContactId(UUID contactId, Pageable pageable);

    long countByCompanyId(UUID companyId);

    long countByContactId(UUID contactId);

    void deleteByContactId(UUID contactId);

    void deleteByCompanyId(UUID companyId);
}
