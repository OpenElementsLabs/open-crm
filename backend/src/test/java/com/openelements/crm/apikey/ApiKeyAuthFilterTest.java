package com.openelements.crm.apikey;

import com.openelements.crm.TestSecurityUtil;
import com.openelements.crm.company.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.openelements.crm.TestSecurityUtil.testJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("API Key Authentication Filter")
class ApiKeyAuthFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private CompanyService companyService;

    private String rawKey;

    @BeforeEach
    void setUp() {
        apiKeyRepository.deleteAll();
        TestSecurityUtil.setSecurityContext();
        final var created = apiKeyService.create(new ApiKeyCreateDto("Test Key"));
        rawKey = created.key();
        TestSecurityUtil.clearSecurityContext();
    }

    @Nested
    @DisplayName("Read access (GET)")
    class ReadAccess {

        @Test
        @DisplayName("valid API key on GET /api/companies returns 200")
        void validKeyOnGetCompanies() throws Exception {
            mockMvc.perform(get("/api/companies").header("X-API-Key", rawKey))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("valid API key on GET /api/contacts with pagination returns 200")
        void validKeyOnGetContactsWithPagination() throws Exception {
            mockMvc.perform(get("/api/contacts")
                    .param("page", "0")
                    .param("size", "5")
                    .header("X-API-Key", rawKey))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("valid API key on GET /api/api-keys returns 200")
        void validKeyOnGetApiKeys() throws Exception {
            mockMvc.perform(get("/api/api-keys").header("X-API-Key", rawKey))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Write rejection")
    class WriteRejection {

        @Test
        @DisplayName("valid API key on POST returns 403")
        void validKeyOnPostReturns403() throws Exception {
            mockMvc.perform(post("/api/companies")
                    .header("X-API-Key", rawKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Test Company\"}"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("valid API key on PUT returns 403")
        void validKeyOnPutReturns403() throws Exception {
            mockMvc.perform(put("/api/companies/00000000-0000-0000-0000-000000000000")
                    .header("X-API-Key", rawKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Test\"}"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("valid API key on DELETE returns 403")
        void validKeyOnDeleteReturns403() throws Exception {
            mockMvc.perform(delete("/api/companies/00000000-0000-0000-0000-000000000000")
                    .header("X-API-Key", rawKey))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("valid API key on DELETE /api/api-keys returns 403")
        void validKeyOnDeleteApiKeysReturns403() throws Exception {
            mockMvc.perform(delete("/api/api-keys/00000000-0000-0000-0000-000000000000")
                    .header("X-API-Key", rawKey))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Invalid keys")
    class InvalidKeys {

        @Test
        @DisplayName("invalid API key returns 401")
        void invalidKeyReturns401() throws Exception {
            mockMvc.perform(get("/api/companies")
                    .header("X-API-Key", "crm_invalid000000000000000000000000000000000000000000"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("empty API key header returns 401")
        void emptyKeyReturns401() throws Exception {
            mockMvc.perform(get("/api/companies")
                    .header("X-API-Key", ""))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("no API key and no JWT returns 401")
        void noKeyNoJwtReturns401() throws Exception {
            mockMvc.perform(get("/api/companies"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Precedence")
    class Precedence {

        @Test
        @DisplayName("API key takes precedence over JWT")
        void apiKeyTakesPrecedence() throws Exception {
            mockMvc.perform(get("/api/companies")
                    .header("X-API-Key", rawKey)
                    .with(testJwt()))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Deleted key")
    class DeletedKey {

        @Test
        @DisplayName("deleted key stops working immediately")
        void deletedKeyStopsWorking() throws Exception {
            // Verify key works
            mockMvc.perform(get("/api/companies").header("X-API-Key", rawKey))
                .andExpect(status().isOk());

            // Delete the key
            final var entity = apiKeyRepository.findByKeyHash(
                ApiKeyService.sha256Hex(rawKey)).orElseThrow();
            mockMvc.perform(delete("/api/api-keys/" + entity.getId()).with(testJwt()))
                .andExpect(status().isNoContent());

            // Verify key no longer works
            mockMvc.perform(get("/api/companies").header("X-API-Key", rawKey))
                .andExpect(status().isUnauthorized());
        }
    }
}
