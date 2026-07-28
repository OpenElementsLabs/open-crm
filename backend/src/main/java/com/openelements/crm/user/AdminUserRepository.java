package com.openelements.crm.user;

import com.openelements.spring.base.services.user.UserEntity;
import com.openelements.spring.base.services.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * CRM-side extension of the lib {@link UserRepository} that adds queries
 * excluding the well-known SYSTEM-USER row, used by the admin user list and the
 * owner-selection options endpoint.
 */
public interface AdminUserRepository extends UserRepository {

    Page<UserEntity> findBySubNot(String sub, Pageable pageable);

    List<UserEntity> findBySubNot(String sub, Sort sort);
}
