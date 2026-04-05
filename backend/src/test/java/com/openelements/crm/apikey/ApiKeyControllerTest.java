package com.openelements.crm.apikey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openelements.crm.TestSecurityUtil;
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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ApiKey Controller")
class ApiKeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @BeforeEach
    void setUp() {
        apiKeyRepository.deleteAll();
    }

    private String createApiKey(final String name) throws Exception {
        final String response = mockMvc.perform(post("/api/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}")
                        .with(testJwt()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Nested
    @DisplayName("POST /api/api-keys")
    class CreateKey {

        @Test
        @DisplayName("should create key with valid name")
        void shouldCreateWithValidName() throws Exception {
            mockMvc.perform(post("/api/api-keys")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"CI Pipeline\"}")
                            .with(testJwt()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.name").value("CI Pipeline"))
                    .andExpect(jsonPath("$.key").isNotEmpty())
                    .andExpect(jsonPath("$.keyPrefix").isNotEmpty())
                    .andExpect(jsonPath("$.createdBy").isNotEmpty())
                    .andExpect(jsonPath("$.createdAt").isNotEmpty());
        }

        @Test
        @DisplayName("should return 400 for blank name")
        void shouldReturn400ForBlankName() throws Exception {
            mockMvc.perform(post("/api/api-keys")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\"}")
                            .with(testJwt()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for null name")
        void shouldReturn400ForNullName() throws Exception {
            mockMvc.perform(post("/api/api-keys")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .with(testJwt()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 401 without auth")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(post("/api/api-keys")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/api-keys")
    class ListKeys {

        @Test
        @DisplayName("should return paginated list")
        void shouldReturnPaginatedList() throws Exception {
            for (int i = 0; i < 25; i++) {
                createApiKey("Key " + i);
            }

            mockMvc.perform(get("/api/api-keys")
                            .param("size", "10")
                            .with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(10)))
                    .andExpect(jsonPath("$.page.totalElements").value(25));
        }

        @Test
        @DisplayName("should not return raw key in list")
        void shouldNotReturnRawKey() throws Exception {
            createApiKey("Test Key");

            mockMvc.perform(get("/api/api-keys").with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].key").doesNotExist())
                    .andExpect(jsonPath("$.content[0].keyPrefix").isNotEmpty());
        }

        @Test
        @DisplayName("should return empty when no keys")
        void shouldReturnEmpty() throws Exception {
            mockMvc.perform(get("/api/api-keys").with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.page.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("DELETE /api/api-keys/{id}")
    class DeleteKey {

        @Test
        @DisplayName("should delete existing key")
        void shouldDeleteExisting() throws Exception {
            final String id = createApiKey("To Delete");

            mockMvc.perform(delete("/api/api-keys/" + id).with(testJwt()))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/api-keys").with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }

        @Test
        @DisplayName("should return 404 for non-existent key")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(delete("/api/api-keys/00000000-0000-0000-0000-000000000000").with(testJwt()))
                    .andExpect(status().isNotFound());
        }
    }
}
