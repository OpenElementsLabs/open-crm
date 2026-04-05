package com.openelements.crm.apikey;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for API key entities.
 */
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    /**
     * Finds an API key by its SHA-256 hash.
     *
     * @param keyHash the SHA-256 hex digest of the raw key
     * @return the API key entity if found
     */
    Optional<ApiKeyEntity> findByKeyHash(String keyHash);
}
