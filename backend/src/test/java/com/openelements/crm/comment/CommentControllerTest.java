package com.openelements.crm.comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.company.CompanyRepository;
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
@DisplayName("Comment Controller")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        contactRepository.deleteAll();
        companyRepository.deleteAll();
    }

    private String createCompany(final String name) throws Exception {
        final String json = """
                {"name": "%s"}
                """.formatted(name);
        final String response = mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String createContact(final String firstName, final String lastName) throws Exception {
        final String json = """
                {"firstName": "%s", "lastName": "%s", "language": "DE"}
                """.formatted(firstName, lastName);
        final String response = mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String addCommentToCompany(final String companyId, final String text) throws Exception {
        final String json = """
                {"text": "%s", "author": "Test User"}
                """.formatted(text);
        final String response = mockMvc.perform(post("/api/companies/" + companyId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String addCommentToContact(final String contactId, final String text) throws Exception {
        final String json = """
                {"text": "%s", "author": "Test User"}
                """.formatted(text);
        final String response = mockMvc.perform(post("/api/contacts/" + contactId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Nested
    @DisplayName("POST comments")
    class CreateComment {

        @Test
        @DisplayName("should add comment to company")
        void shouldAddToCompany() throws Exception {
            //GIVEN
            final String companyId = createCompany("Test Co");
            final String json = """
                    {"text": "Great meeting", "author": "Hendrik"}
                    """;

            //WHEN
            final var result = mockMvc.perform(post("/api/companies/" + companyId + "/comments")
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.text").value("Great meeting"))
                    .andExpect(jsonPath("$.author").value("Hendrik"))
                    .andExpect(jsonPath("$.companyId").value(companyId))
                    .andExpect(jsonPath("$.contactId").isEmpty())
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("should add comment to contact")
        void shouldAddToContact() throws Exception {
            //GIVEN
            final String contactId = createContact("John", "Doe");
            final String json = """
                    {"text": "Follow up needed", "author": "Admin"}
                    """;

            //WHEN
            final var result = mockMvc.perform(post("/api/contacts/" + contactId + "/comments")
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.contactId").value(contactId))
                    .andExpect(jsonPath("$.companyId").isEmpty());
        }

        @Test
        @DisplayName("should fail for non-existent company")
        void shouldFailForNonExistentCompany() throws Exception {
            //GIVEN
            final String json = """
                    {"text": "Comment", "author": "User"}
                    """;

            //WHEN
            final var result = mockMvc.perform(post("/api/companies/00000000-0000-0000-0000-000000000001/comments")
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should fail for non-existent contact")
        void shouldFailForNonExistentContact() throws Exception {
            //GIVEN
            final String json = """
                    {"text": "Comment", "author": "User"}
                    """;

            //WHEN
            final var result = mockMvc.perform(post("/api/contacts/00000000-0000-0000-0000-000000000001/comments")
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should fail without text")
        void shouldFailWithoutText() throws Exception {
            //GIVEN
            final String companyId = createCompany("Test Co");
            final String json = """
                    {"text": "  ", "author": "User"}
                    """;

            //WHEN
            final var result = mockMvc.perform(post("/api/companies/" + companyId + "/comments")
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should fail without author")
        void shouldFailWithoutAuthor() throws Exception {
            //GIVEN
            final String companyId = createCompany("Test Co");
            final String json = """
                    {"text": "A comment", "author": " "}
                    """;

            //WHEN
            final var result = mockMvc.perform(post("/api/companies/" + companyId + "/comments")
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should allow comment on soft-deleted company")
        void shouldAllowOnSoftDeletedCompany() throws Exception {
            //GIVEN
            final String companyId = createCompany("Deleted Co");
            mockMvc.perform(delete("/api/companies/" + companyId));
            final String json = """
                    {"text": "Still commenting", "author": "User"}
                    """;

            //WHEN
            final var result = mockMvc.perform(post("/api/companies/" + companyId + "/comments")
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("GET comments")
    class ListComments {

        @Test
        @DisplayName("should list company comments sorted by createdAt desc")
        void shouldListCompanyComments() throws Exception {
            //GIVEN
            final String companyId = createCompany("Test Co");
            addCommentToCompany(companyId, "First");
            addCommentToCompany(companyId, "Second");
            addCommentToCompany(companyId, "Third");

            //WHEN
            final var result = mockMvc.perform(get("/api/companies/" + companyId + "/comments"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.content[0].text").value("Third"))
                    .andExpect(jsonPath("$.content[2].text").value("First"));
        }

        @Test
        @DisplayName("should list contact comments sorted by createdAt desc")
        void shouldListContactComments() throws Exception {
            //GIVEN
            final String contactId = createContact("John", "Doe");
            addCommentToContact(contactId, "Note 1");
            addCommentToContact(contactId, "Note 2");
            addCommentToContact(contactId, "Note 3");

            //WHEN
            final var result = mockMvc.perform(get("/api/contacts/" + contactId + "/comments"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.content[0].text").value("Note 3"));
        }
    }

    @Nested
    @DisplayName("PUT /api/comments/{id}")
    class UpdateComment {

        @Test
        @DisplayName("should update comment text")
        void shouldUpdate() throws Exception {
            //GIVEN
            final String companyId = createCompany("Test Co");
            final String commentId = addCommentToCompany(companyId, "Original text");
            final String json = """
                    {"text": "Updated text", "author": "Test User"}
                    """;

            //WHEN
            final var result = mockMvc.perform(put("/api/comments/" + commentId)
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.text").value("Updated text"));
        }

        @Test
        @DisplayName("should fail with blank text")
        void shouldFailWithBlankText() throws Exception {
            //GIVEN
            final String companyId = createCompany("Test Co");
            final String commentId = addCommentToCompany(companyId, "Original");
            final String json = """
                    {"text": "  ", "author": "User"}
                    """;

            //WHEN
            final var result = mockMvc.perform(put("/api/comments/" + commentId)
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/comments/{id}")
    class DeleteComment {

        @Test
        @DisplayName("should delete comment")
        void shouldDelete() throws Exception {
            //GIVEN
            final String companyId = createCompany("Test Co");
            final String commentId = addCommentToCompany(companyId, "To delete");

            //WHEN
            final var result = mockMvc.perform(delete("/api/comments/" + commentId));

            //THEN
            result.andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 for non-existent comment")
        void shouldReturn404() throws Exception {
            //GIVEN
            //  no comment exists

            //WHEN
            final var result = mockMvc.perform(delete("/api/comments/00000000-0000-0000-0000-000000000001"));

            //THEN
            result.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Cascade and Referential Integrity")
    class CascadeTests {

        @Test
        @DisplayName("contact deletion should cascade comments")
        void contactDeletionShouldCascadeComments() throws Exception {
            //GIVEN
            final String contactId = createContact("John", "Doe");
            addCommentToContact(contactId, "Comment 1");
            addCommentToContact(contactId, "Comment 2");
            addCommentToContact(contactId, "Comment 3");

            //WHEN
            mockMvc.perform(delete("/api/contacts/" + contactId))
                    .andExpect(status().isNoContent());

            //THEN
            // Contact is gone
            mockMvc.perform(get("/api/contacts/" + contactId))
                    .andExpect(status().isNotFound());
            // All comments are gone too (verified via repository)
            org.assertj.core.api.Assertions.assertThat(commentRepository.count()).isZero();
        }

        @Test
        @DisplayName("company soft-delete should preserve comments")
        void companySoftDeleteShouldPreserveComments() throws Exception {
            //GIVEN
            final String companyId = createCompany("Test Co");
            addCommentToCompany(companyId, "Comment 1");
            addCommentToCompany(companyId, "Comment 2");

            //WHEN
            mockMvc.perform(delete("/api/companies/" + companyId))
                    .andExpect(status().isNoContent());

            //THEN
            mockMvc.perform(get("/api/companies/" + companyId + "/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)));
        }

        @Test
        @DisplayName("contact cannot reference soft-deleted company")
        void contactCannotReferenceSoftDeletedCompany() throws Exception {
            //GIVEN
            final String companyId = createCompany("Deleted Co");
            mockMvc.perform(delete("/api/companies/" + companyId));

            //WHEN
            final String json = """
                    {"firstName": "Jane", "lastName": "Doe", "language": "EN", "companyId": "%s"}
                    """.formatted(companyId);
            final var result = mockMvc.perform(post("/api/contacts")
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("contact cannot be moved to soft-deleted company")
        void contactCannotBeMovedToSoftDeletedCompany() throws Exception {
            //GIVEN
            final String companyA = createCompany("Active Co");
            final String companyB = createCompany("To Delete");
            final String contactJson = """
                    {"firstName": "Jane", "lastName": "Doe", "language": "EN", "companyId": "%s"}
                    """.formatted(companyA);
            final String response = mockMvc.perform(post("/api/contacts")
                            .contentType(MediaType.APPLICATION_JSON).content(contactJson))
                    .andReturn().getResponse().getContentAsString();
            final String contactId = objectMapper.readTree(response).get("id").asText();

            mockMvc.perform(delete("/api/companies/" + companyB));

            //WHEN
            final String updateJson = """
                    {"firstName": "Jane", "lastName": "Doe", "language": "EN", "companyId": "%s"}
                    """.formatted(companyB);
            final var result = mockMvc.perform(put("/api/contacts/" + contactId)
                    .contentType(MediaType.APPLICATION_JSON).content(updateJson));

            //THEN
            result.andExpect(status().isBadRequest());
        }
    }
}
