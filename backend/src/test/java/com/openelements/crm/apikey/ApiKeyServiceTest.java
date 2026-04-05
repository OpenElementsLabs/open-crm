package com.openelements.crm.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openelements.crm.TestSecurityUtil;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ApiKeyService")
class ApiKeyServiceTest {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @BeforeEach
    void setUp() {
        apiKeyRepository.deleteAll();
        TestSecurityUtil.setSecurityContext();
    }

    @AfterEach
    void tearDown() {
        TestSecurityUtil.clearSecurityContext();
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create key with valid name")
        void shouldCreateWithValidName() {
            final var result = apiKeyService.create(new ApiKeyCreateDto("CI Pipeline"));

            assertNotNull(result.id());
            assertEquals("CI Pipeline", result.name());
            assertNotNull(result.key());
            assertTrue(result.key().startsWith("crm_"));
            assertEquals(52, result.key().length());
            assertNotNull(result.keyPrefix());
            assertTrue(result.keyPrefix().startsWith("crm_"));
            assertTrue(result.keyPrefix().contains("..."));
            assertEquals("Test User", result.createdBy());
            assertNotNull(result.createdAt());
        }

        @Test
        @DisplayName("should produce unique keys for same name")
        void shouldProduceUniqueKeys() {
            final var key1 = apiKeyService.create(new ApiKeyCreateDto("Same Name"));
            final var key2 = apiKeyService.create(new ApiKeyCreateDto("Same Name"));

            assertNotEquals(key1.id(), key2.id());
            assertNotEquals(key1.key(), key2.key());
        }

        @Test
        @DisplayName("key prefix matches key format")
        void keyPrefixMatchesKey() {
            final var result = apiKeyService.create(new ApiKeyCreateDto("Test"));
            final String key = result.key();
            final String expectedPrefix = key.substring(0, 8) + "..." + key.substring(key.length() - 4);
            assertEquals(expectedPrefix, result.keyPrefix());
        }
    }

    @Nested
    @DisplayName("list")
    class ListKeys {

        @Test
        @DisplayName("should return paginated results")
        void shouldReturnPaginated() {
            for (int i = 0; i < 25; i++) {
                apiKeyService.create(new ApiKeyCreateDto("Key " + i));
            }

            final var page = apiKeyService.list(PageRequest.of(0, 10));
            assertEquals(10, page.getContent().size());
            assertEquals(25, page.getTotalElements());
        }

        @Test
        @DisplayName("should return empty when no keys")
        void shouldReturnEmpty() {
            final var page = apiKeyService.list(PageRequest.of(0, 20));
            assertTrue(page.getContent().isEmpty());
            assertEquals(0, page.getTotalElements());
        }

        @Test
        @DisplayName("list DTO should not contain raw key")
        void listShouldNotContainRawKey() {
            apiKeyService.create(new ApiKeyCreateDto("Test Key"));

            final var page = apiKeyService.list(PageRequest.of(0, 20));
            final var dto = page.getContent().get(0);
            assertNotNull(dto.id());
            assertNotNull(dto.name());
            assertNotNull(dto.keyPrefix());
            assertNotNull(dto.createdBy());
            assertNotNull(dto.createdAt());
            // ApiKeyDto record has no 'key' field — only 5 components
            assertEquals(5, ApiKeyDto.class.getRecordComponents().length);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete existing key")
        void shouldDeleteExisting() {
            final var created = apiKeyService.create(new ApiKeyCreateDto("To Delete"));
            apiKeyService.delete(created.id());

            final var exception = assertThrows(ResponseStatusException.class,
                    () -> apiKeyService.delete(created.id()));
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }

        @Test
        @DisplayName("should throw 404 for non-existent key")
        void shouldThrow404ForNonExistent() {
            final var exception = assertThrows(ResponseStatusException.class,
                    () -> apiKeyService.delete(UUID.randomUUID()));
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }
    }

    @Nested
    @DisplayName("authenticate")
    class Authenticate {

        @Test
        @DisplayName("should find key by raw key")
        void shouldFindByRawKey() {
            final var created = apiKeyService.create(new ApiKeyCreateDto("Auth Test"));
            final var result = apiKeyService.authenticate(created.key());

            assertTrue(result.isPresent());
            assertEquals(created.id(), result.get().getId());
        }

        @Test
        @DisplayName("should return empty for invalid key")
        void shouldReturnEmptyForInvalid() {
            final var result = apiKeyService.authenticate("crm_invalid000000000000000000000000000000000000000000");
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("should return empty for blank key")
        void shouldReturnEmptyForBlank() {
            final var result = apiKeyService.authenticate("");
            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("hashing")
    class Hashing {

        @Test
        @DisplayName("hash is 64 character hex string")
        void hashIs64CharHex() {
            final var created = apiKeyService.create(new ApiKeyCreateDto("Hash Test"));
            final var entity = apiKeyRepository.findById(created.id()).orElseThrow();

            assertEquals(64, entity.getKeyHash().length());
            assertTrue(entity.getKeyHash().matches("[0-9a-f]{64}"));
        }

        @Test
        @DisplayName("different keys produce different hashes")
        void differentKeysProduceDifferentHashes() {
            final var key1 = apiKeyService.create(new ApiKeyCreateDto("Key 1"));
            final var key2 = apiKeyService.create(new ApiKeyCreateDto("Key 2"));

            final var entity1 = apiKeyRepository.findById(key1.id()).orElseThrow();
            final var entity2 = apiKeyRepository.findById(key2.id()).orElseThrow();

            assertNotEquals(entity1.getKeyHash(), entity2.getKeyHash());
        }
    }
}
