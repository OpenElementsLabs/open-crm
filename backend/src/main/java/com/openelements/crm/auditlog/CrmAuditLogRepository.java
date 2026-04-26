package com.openelements.crm.auditlog;

import com.openelements.spring.base.services.audit.AuditLogEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Project-local repository over {@link AuditLogEntity} that exposes paginated
 * filter queries. The {@code AuditLogDataService} from {@code spring-services}
 * only offers unpaginated {@code List}-returning filter methods, which would
 * force in-memory pagination over an unbounded audit table.
 */
public interface CrmAuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    Page<AuditLogEntity> findByEntityType(String entityType, Pageable pageable);

    Page<AuditLogEntity> findByUserName(String userName, Pageable pageable);

    Page<AuditLogEntity> findByEntityTypeAndUserName(String entityType, String userName, Pageable pageable);

    @Query("SELECT DISTINCT a.entityType FROM AuditLogEntity a ORDER BY a.entityType")
    List<String> findDistinctEntityTypes();
}
