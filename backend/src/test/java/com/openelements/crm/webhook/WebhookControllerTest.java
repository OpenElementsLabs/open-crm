package com.openelements.crm.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Webhook Controller")
@Disabled
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebhookRepository webhookRepository;

    @BeforeEach
    void setUp() {
        webhookRepository.deleteAll();
    }

    private String createWebhook(final String url) throws Exception {
        final String json = """
                { "url": "%s" }
                """.formatted(url);
        final String response = mockMvc.perform(post("/api/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json).with(testJwt()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Nested
    @DisplayName("POST /api/webhooks")
    class CreateWebhook {

        @Test
        @DisplayName("should create webhook with valid URL")
        void shouldCreateWithValidUrl() throws Exception {
            mockMvc.perform(post("/api/webhooks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "url": "https://example.com/hook" }
                                    """)
                            .with(testJwt()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.url").value("https://example.com/hook"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty())
                    .andExpect(jsonPath("$.updatedAt").isNotEmpty());
        }

        @Test
        @DisplayName("should return 400 for missing URL")
        void shouldReturn400ForMissingUrl() throws Exception {
            mockMvc.perform(post("/api/webhooks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .with(testJwt()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for blank URL")
        void shouldReturn400ForBlankUrl() throws Exception {
            mockMvc.perform(post("/api/webhooks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "url": "" }
                                    """)
                            .with(testJwt()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/webhooks")
    class ListWebhooks {

        @Test
        @DisplayName("should return paginated list")
        void shouldReturnPaginatedList() throws Exception {
            for (int i = 0; i < 25; i++) {
                createWebhook("https://example.com/hook" + i);
            }

            mockMvc.perform(get("/api/webhooks")
                            .param("size", "20")
                            .with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(20)))
                    .andExpect(jsonPath("$.page.totalElements").value(25));
        }

        @Test
        @DisplayName("should return empty when no webhooks exist")
        void shouldReturnEmptyWhenNone() throws Exception {
            mockMvc.perform(get("/api/webhooks").with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.page.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/webhooks/{id}")
    class GetWebhook {

        @Test
        @DisplayName("should return webhook by ID")
        void shouldReturnById() throws Exception {
            final String id = createWebhook("https://example.com/hook");

            mockMvc.perform(get("/api/webhooks/" + id).with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.url").value("https://example.com/hook"));
        }

        @Test
        @DisplayName("should return 404 for unknown ID")
        void shouldReturn404ForUnknownId() throws Exception {
            mockMvc.perform(get("/api/webhooks/00000000-0000-0000-0000-000000000000").with(testJwt()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/webhooks/{id}")
    class UpdateWebhook {

        @Test
        @DisplayName("should update webhook URL")
        void shouldUpdateUrl() throws Exception {
            final String id = createWebhook("https://old.com/hook");

            mockMvc.perform(put("/api/webhooks/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "url": "https://new.com/hook", "active": true }
                                    """)
                            .with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("https://new.com/hook"));
        }

        @Test
        @DisplayName("should deactivate webhook")
        void shouldDeactivate() throws Exception {
            final String id = createWebhook("https://example.com/hook");

            mockMvc.perform(put("/api/webhooks/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "url": "https://example.com/hook", "active": false }
                                    """)
                            .with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
        }

        @Test
        @DisplayName("should return 404 for unknown ID")
        void shouldReturn404ForUnknownId() throws Exception {
            mockMvc.perform(put("/api/webhooks/00000000-0000-0000-0000-000000000000")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "url": "https://example.com", "active": true }
                                    """)
                            .with(testJwt()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/webhooks/{id}")
    class DeleteWebhook {

        @Test
        @DisplayName("should delete webhook")
        void shouldDelete() throws Exception {
            final String id = createWebhook("https://example.com/hook");

            mockMvc.perform(delete("/api/webhooks/" + id).with(testJwt()))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/webhooks/" + id).with(testJwt()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 for unknown ID")
        void shouldReturn404ForUnknownId() throws Exception {
            mockMvc.perform(delete("/api/webhooks/00000000-0000-0000-0000-000000000000").with(testJwt()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/webhooks/{id}/ping")
    class PingWebhook {

        @Test
        @DisplayName("should return 202 Accepted for active webhook")
        void shouldReturn202ForActiveWebhook() throws Exception {
            final String id = createWebhook("https://example.com/hook");

            mockMvc.perform(post("/api/webhooks/" + id + "/ping").with(testJwt()))
                    .andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("should return 202 Accepted for inactive webhook")
        void shouldReturn202ForInactiveWebhook() throws Exception {
            final String id = createWebhook("https://example.com/hook");

            // Deactivate the webhook
            mockMvc.perform(put("/api/webhooks/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "url": "https://example.com/hook", "active": false }
                                    """)
                            .with(testJwt()))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/webhooks/" + id + "/ping").with(testJwt()))
                    .andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("should return 404 for unknown ID")
        void shouldReturn404ForUnknownId() throws Exception {
            mockMvc.perform(post("/api/webhooks/00000000-0000-0000-0000-000000000000/ping").with(testJwt()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Status fields in DTO")
    class StatusFields {

        @Test
        @DisplayName("new webhook should have null lastStatus and lastCalledAt")
        void shouldHaveNullStatusForNewWebhook() throws Exception {
            final String id = createWebhook("https://example.com/hook");

            mockMvc.perform(get("/api/webhooks/" + id).with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lastStatus").isEmpty())
                    .andExpect(jsonPath("$.lastCalledAt").isEmpty());
        }
    }
}
