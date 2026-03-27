package com.openelements.crm.company;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Spring Data repository for {@link CompanyEntity} persistence operations.
 */
public interface CompanyRepository extends JpaRepository<CompanyEntity, UUID>,
        JpaSpecificationExecutor<CompanyEntity> {
}
