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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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

        @Test
        @DisplayName("should mark required properties in CompanyCreateDto schema")
        void shouldMarkRequiredInCompanyCreateDto() throws Exception {
            //GIVEN
            //  the application is running

            //WHEN
            final var result = mockMvc.perform(get("/v3/api-docs"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.components.schemas.CompanyCreateDto.required", hasItem("name")))
                    .andExpect(jsonPath("$.components.schemas.CompanyCreateDto.required", not(hasItem("email"))))
                    .andExpect(jsonPath("$.components.schemas.CompanyCreateDto.required", not(hasItem("city"))));
        }

        @Test
        @DisplayName("should mark required properties in ContactCreateDto schema")
        void shouldMarkRequiredInContactCreateDto() throws Exception {
            //GIVEN
            //  the application is running

            //WHEN
            final var result = mockMvc.perform(get("/v3/api-docs"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.components.schemas.ContactCreateDto.required", hasItem("firstName")))
                    .andExpect(jsonPath("$.components.schemas.ContactCreateDto.required", hasItem("lastName")))
                    .andExpect(jsonPath("$.components.schemas.ContactCreateDto.required", not(hasItem("language"))))
                    .andExpect(jsonPath("$.components.schemas.ContactCreateDto.required", not(hasItem("email"))))
                    .andExpect(jsonPath("$.components.schemas.ContactCreateDto.required", not(hasItem("gender"))));
        }

        @Test
        @DisplayName("should mark required properties in CommentCreateDto schema")
        void shouldMarkRequiredInCommentCreateDto() throws Exception {
            //GIVEN
            //  the application is running

            //WHEN
            final var result = mockMvc.perform(get("/v3/api-docs"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.components.schemas.CommentCreateDto.required", hasItem("text")));
        }

        @Test
        @DisplayName("should mark required properties in CompanyDto schema")
        void shouldMarkRequiredInCompanyDto() throws Exception {
            //GIVEN
            //  the application is running

            //WHEN
            final var result = mockMvc.perform(get("/v3/api-docs"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.components.schemas.CompanyDto.required", hasItem("id")))
                    .andExpect(jsonPath("$.components.schemas.CompanyDto.required", hasItem("name")))
                    .andExpect(jsonPath("$.components.schemas.CompanyDto.required", hasItem("deleted")))
                    .andExpect(jsonPath("$.components.schemas.CompanyDto.required", hasItem("createdAt")))
                    .andExpect(jsonPath("$.components.schemas.CompanyDto.required", hasItem("updatedAt")));
        }

        @Test
        @DisplayName("should mark required properties in ContactDto schema")
        void shouldMarkRequiredInContactDto() throws Exception {
            //GIVEN
            //  the application is running

            //WHEN
            final var result = mockMvc.perform(get("/v3/api-docs"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.components.schemas.ContactDto.required", hasItem("id")))
                    .andExpect(jsonPath("$.components.schemas.ContactDto.required", hasItem("firstName")))
                    .andExpect(jsonPath("$.components.schemas.ContactDto.required", hasItem("lastName")))
                    .andExpect(jsonPath("$.components.schemas.ContactDto.required", hasItem("brevo")))
                    .andExpect(jsonPath("$.components.schemas.ContactDto.required", not(hasItem("language"))))
                    .andExpect(jsonPath("$.components.schemas.ContactDto.required", hasItem("createdAt")))
                    .andExpect(jsonPath("$.components.schemas.ContactDto.required", hasItem("updatedAt")));
        }

        @Test
        @DisplayName("should mark required properties in CommentDto schema")
        void shouldMarkRequiredInCommentDto() throws Exception {
            //GIVEN
            //  the application is running

            //WHEN
            final var result = mockMvc.perform(get("/v3/api-docs"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.components.schemas.CommentDto.required", hasItem("id")))
                    .andExpect(jsonPath("$.components.schemas.CommentDto.required", hasItem("text")))
                    .andExpect(jsonPath("$.components.schemas.CommentDto.required", hasItem("author")))
                    .andExpect(jsonPath("$.components.schemas.CommentDto.required", hasItem("createdAt")))
                    .andExpect(jsonPath("$.components.schemas.CommentDto.required", hasItem("updatedAt")));
        }

        @Test
        @DisplayName("should mark required properties in HealthDto schema")
        void shouldMarkRequiredInHealthDto() throws Exception {
            //GIVEN
            //  the application is running

            //WHEN
            final var result = mockMvc.perform(get("/v3/api-docs"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.components.schemas.HealthDto.required", hasItem("status")));
        }
    }
}
