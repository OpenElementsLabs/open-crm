package com.openelements.crm.tag;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository extends JpaRepository<TagEntity, UUID> {

    Optional<TagEntity> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT COUNT(c) FROM CompanyEntity c JOIN c.tags t WHERE t.id = :tagId AND c.deleted = false")
    long countActiveCompaniesByTagId(@Param("tagId") UUID tagId);

    @Query("SELECT COUNT(c) FROM ContactEntity c JOIN c.tags t WHERE t.id = :tagId")
    long countContactsByTagId(@Param("tagId") UUID tagId);

    @Query("SELECT COUNT(t) FROM TaskEntity t JOIN t.tags tag WHERE tag.id = :tagId AND t.status <> com.openelements.crm.task.TaskStatus.DONE")
    long countActiveTasksByTagId(@Param("tagId") UUID tagId);
}
