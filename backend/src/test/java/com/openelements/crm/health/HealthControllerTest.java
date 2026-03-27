package com.openelements.crm.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Health Controller")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /api/health")
    class GetHealth {

        @Test
        @DisplayName("should return 200 with status UP")
        void shouldReturnUpStatus() throws Exception {
            //GIVEN
            //  the application is running

            //WHEN
            final var result = mockMvc.perform(get("/api/health"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }

    @Nested
    @DisplayName("Swagger UI")
    class SwaggerUi {

        @Test
        @DisplayName("should be accessible")
        void shouldBeAccessible() throws Exception {
            //GIVEN
            //  the application is running

            //WHEN
            final var result = mockMvc.perform(get("/swagger-ui/index.html"));

            //THEN
            result.andExpect(status().isOk());
        }

        @Test
        @DisplayName("should have OpenAPI spec available")
        void shouldHaveOpenApiSpec() throws Exception {
            //GIVEN
            //  the application is running

            //WHEN
            final var result = mockMvc.perform(get("/v3/api-docs"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.paths./api/health").exists())
                    .andExpect(jsonPath("$.paths./api/companies").exists())
                    .andExpect(jsonPath("$.paths./api/companies/{id}").exists())
                    .andExpect(jsonPath("$.paths./api/companies/{id}/restore").exists())
                    .andExpect(jsonPath("$.paths./api/companies/{id}/comments").exists())
                    .andExpect(jsonPath("$.paths./api/contacts").exists())
                    .andExpect(jsonPath("$.paths./api/contacts/{id}").exists())
                    .andExpect(jsonPath("$.paths./api/contacts/{id}/comments").exists())
                    .andExpect(jsonPath("$.paths./api/comments/{id}").exists());
        }
    }
}
