package com.openelements.crm.task;

import com.openelements.spring.base.data.EntityRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskRepository extends EntityRepository<TaskEntity>, JpaSpecificationExecutor<TaskEntity> {

}
