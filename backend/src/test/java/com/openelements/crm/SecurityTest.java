package com.openelements.crm;

import com.openelements.crm.comment.CommentRepository;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.user.UserInfo;
import com.openelements.crm.user.UserService;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security")
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

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

    @Nested
    @DisplayName("Protected endpoints")
    class ProtectedEndpoints {

        @Test
        @DisplayName("GET /api/companies without token returns 401")
        void companiesWithoutToken() throws Exception {
            mockMvc.perform(get("/api/companies"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/companies without token returns 401")
        void createCompanyWithoutToken() throws Exception {
            mockMvc.perform(post("/api/companies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("PUT /api/companies/{id} without token returns 401")
        void updateCompanyWithoutToken() throws Exception {
            mockMvc.perform(put("/api/companies/00000000-0000-0000-0000-000000000001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("DELETE /api/companies/{id} without token returns 401")
        void deleteCompanyWithoutToken() throws Exception {
            mockMvc.perform(delete("/api/companies/00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/contacts without token returns 401")
        void contactsWithoutToken() throws Exception {
            mockMvc.perform(get("/api/contacts"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/contacts without token returns 401")
        void createContactWithoutToken() throws Exception {
            mockMvc.perform(post("/api/contacts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "A", "lastName": "B"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/companies/{id}/comments without token returns 401")
        void commentsWithoutToken() throws Exception {
            mockMvc.perform(get("/api/companies/00000000-0000-0000-0000-000000000001/comments"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/companies/{id}/comments without token returns 401")
        void createCommentWithoutToken() throws Exception {
            mockMvc.perform(post("/api/companies/00000000-0000-0000-0000-000000000001/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"text": "Test"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/companies/export without token returns 401")
        void csvExportWithoutToken() throws Exception {
            mockMvc.perform(get("/api/companies/export"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/companies/{id}/logo without token returns 401")
        void logoWithoutToken() throws Exception {
            mockMvc.perform(get("/api/companies/00000000-0000-0000-0000-000000000001/logo"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/brevo/sync without token returns 401")
        void brevoSyncWithoutToken() throws Exception {
            mockMvc.perform(post("/api/brevo/sync"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/companies with valid token returns 200")
        void companiesWithToken() throws Exception {
            mockMvc.perform(get("/api/companies").with(testJwt()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Public endpoints")
    class PublicEndpoints {

        @Test
        @DisplayName("GET /api/health without token returns 200")
        void healthWithoutToken() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /swagger-ui/index.html without token returns 200")
        void swaggerWithoutToken() throws Exception {
            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /v3/api-docs without token returns 200")
        void openApiDocsWithoutToken() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("User extraction from JWT")
    class UserExtraction {

        @Test
        @DisplayName("UserService returns user from JWT claims")
        void returnsUserFromJwt() {
            TestSecurityUtil.setSecurityContext("Alice", "alice@example.com");
            try {
                final UserInfo user = userService.getCurrentUser();
                assertEquals("Alice", user.name());
                assertEquals("alice@example.com", user.email());
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("UserService handles missing name claim")
        void handlesMissingName() {
            TestSecurityUtil.setSecurityContext(null, "test@example.com");
            try {
                final UserInfo user = userService.getCurrentUser();
                assertEquals("Unknown", user.name());
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("UserService handles missing email claim")
        void handlesMissingEmail() {
            TestSecurityUtil.setSecurityContext("Alice", null);
            try {
                final UserInfo user = userService.getCurrentUser();
                assertEquals("", user.email());
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("UserService throws without authentication")
        void throwsWithoutAuth() {
            TestSecurityUtil.clearSecurityContext();
            assertThrows(IllegalStateException.class, () -> userService.getCurrentUser());
        }
    }

    @Nested
    @DisplayName("Comment author from token")
    class CommentAuthor {

        @Test
        @DisplayName("comment author set from authenticated user name")
        void authorFromToken() throws Exception {
            // Create company
            final String companyResponse = mockMvc.perform(post("/api/companies")
                            .with(testJwt("Alice", "alice@example.com"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test Corp"}
                                    """))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            final String companyId = companyResponse.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

            // Create comment as Alice
            mockMvc.perform(post("/api/companies/" + companyId + "/comments")
                            .with(testJwt("Alice", "alice@example.com"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"text": "Hello from Alice"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.author").value("Alice"));
        }

        @Test
        @DisplayName("different users create comments with their own names")
        void differentUsers() throws Exception {
            // Create company
            final String companyResponse = mockMvc.perform(post("/api/companies")
                            .with(testJwt("Alice", "alice@example.com"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test Corp"}
                                    """))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            final String companyId = companyResponse.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

            // Comment as Alice
            mockMvc.perform(post("/api/companies/" + companyId + "/comments")
                            .with(testJwt("Alice", "alice@example.com"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"text": "From Alice"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.author").value("Alice"));

            // Comment as Bob
            mockMvc.perform(post("/api/companies/" + companyId + "/comments")
                            .with(testJwt("Bob", "bob@example.com"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"text": "From Bob"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.author").value("Bob"));
        }
    }
}
