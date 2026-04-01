package com.openelements.crm.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openelements.crm.comment.CommentRepository;
import com.openelements.crm.contact.ContactRepository;
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

import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static com.openelements.crm.TestSecurityUtil.testJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Company Controller")
class CompanyControllerTest {

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

    private String createCompanyJson(final String name, final String email, final String city, final String country) {
        return """
                {
                    "name": "%s",
                    "email": %s,
                    "city": %s,
                    "country": %s
                }
                """.formatted(
                name,
                email != null ? "\"" + email + "\"" : "null",
                city != null ? "\"" + city + "\"" : "null",
                country != null ? "\"" + country + "\"" : "null"
        );
    }

    private String createCompany(final String name) throws Exception {
        final String json = createCompanyJson(name, null, null, null);
        final String response = mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json).with(testJwt()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Nested
    @DisplayName("POST /api/companies")
    class CreateCompany {

        @Test
        @DisplayName("should create company with all fields")
        void shouldCreateWithAllFields() throws Exception {
            //GIVEN
            final String json = """
                    {
                        "name": "Open Elements GmbH",
                        "email": "info@open-elements.com",
                        "website": "https://open-elements.com",
                        "street": "Musterstraße",
                        "houseNumber": "42",
                        "zipCode": "12345",
                        "city": "Berlin",
                        "country": "Germany"
                    }
                    """;

            //WHEN
            final var result = mockMvc.perform(post("/api/companies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json).with(testJwt()));

            //THEN
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("Open Elements GmbH"))
                    .andExpect(jsonPath("$.email").value("info@open-elements.com"))
                    .andExpect(jsonPath("$.city").value("Berlin"))
                    .andExpect(jsonPath("$.brevo").value(false))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        @DisplayName("should create company with only required fields")
        void shouldCreateWithOnlyName() throws Exception {
            //GIVEN
            final String json = """
                    {"name": "Minimal Corp"}
                    """;

            //WHEN
            final var result = mockMvc.perform(post("/api/companies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json).with(testJwt()));

            //THEN
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Minimal Corp"))
                    .andExpect(jsonPath("$.email").isEmpty())
                    .andExpect(jsonPath("$.website").isEmpty());
        }

        @Test
        @DisplayName("should fail without name")
        void shouldFailWithoutName() throws Exception {
            //GIVEN
            final String json = """
                    {"email": "test@test.com"}
                    """;

            //WHEN
            final var result = mockMvc.perform(post("/api/companies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json).with(testJwt()));

            //THEN
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should fail with blank name")
        void shouldFailWithBlankName() throws Exception {
            //GIVEN
            final String json = """
                    {"name": "   "}
                    """;

            //WHEN
            final var result = mockMvc.perform(post("/api/companies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json).with(testJwt()));

            //THEN
            result.andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/companies/{id}")
    class GetCompany {

        @Test
        @DisplayName("should return company by ID")
        void shouldReturnById() throws Exception {
            //GIVEN
            final String id = createCompany("Test Corp");

            //WHEN
            final var result = mockMvc.perform(get("/api/companies/" + id).with(testJwt()));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.name").value("Test Corp"));
        }

        @Test
        @DisplayName("should return 404 for non-existent ID")
        void shouldReturn404() throws Exception {
            //GIVEN
            //  no company exists

            //WHEN
            final var result = mockMvc.perform(get("/api/companies/00000000-0000-0000-0000-000000000001").with(testJwt()));

            //THEN
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 for hard-deleted company")
        void shouldReturn404ForHardDeletedCompany() throws Exception {
            //GIVEN
            final String id = createCompany("Deleted Corp");
            mockMvc.perform(delete("/api/companies/" + id).with(testJwt()));

            //WHEN
            final var result = mockMvc.perform(get("/api/companies/" + id).with(testJwt()));

            //THEN
            result.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/companies/{id}")
    class UpdateCompany {

        @Test
        @DisplayName("should update company")
        void shouldUpdate() throws Exception {
            //GIVEN
            final String id = createCompany("Old Name");
            final String json = """
                    {"name": "New Name", "city": "Munich"}
                    """;

            //WHEN
            final var result = mockMvc.perform(put("/api/companies/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json).with(testJwt()));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("New Name"))
                    .andExpect(jsonPath("$.city").value("Munich"));
        }

        @Test
        @DisplayName("should return 404 for non-existent ID")
        void shouldReturn404() throws Exception {
            //GIVEN
            final String json = """
                    {"name": "Does Not Matter"}
                    """;

            //WHEN
            final var result = mockMvc.perform(put("/api/companies/00000000-0000-0000-0000-000000000001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json).with(testJwt()));

            //THEN
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should fail with blank name")
        void shouldFailWithBlankName() throws Exception {
            //GIVEN
            final String id = createCompany("Valid Name");
            final String json = """
                    {"name": "  "}
                    """;

            //WHEN
            final var result = mockMvc.perform(put("/api/companies/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json).with(testJwt()));

            //THEN
            result.andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/companies/{id}")
    class DeleteCompany {

        @Test
        @DisplayName("should hard-delete company without contacts")
        void shouldHardDelete() throws Exception {
            //GIVEN
            final String id = createCompany("To Delete");

            //WHEN
            final var result = mockMvc.perform(delete("/api/companies/" + id).with(testJwt()));

            //THEN
            result.andExpect(status().isNoContent());

            mockMvc.perform(get("/api/companies/" + id).with(testJwt()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should delete company and contacts when deleteContacts=true")
        void shouldDeleteWithContacts() throws Exception {
            //GIVEN
            final String companyId = createCompany("Has Contacts");
            final String contactJson = """
                    {"firstName": "John", "lastName": "Doe", "language": "EN", "companyId": "%s"}
                    """.formatted(companyId);
            mockMvc.perform(post("/api/contacts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(contactJson).with(testJwt()));

            //WHEN
            final var result = mockMvc.perform(delete("/api/companies/" + companyId + "?deleteContacts=true").with(testJwt()));

            //THEN
            result.andExpect(status().isNoContent());
            mockMvc.perform(get("/api/companies/" + companyId).with(testJwt()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should delete company only and unlink contacts when deleteContacts=false")
        void shouldDeleteCompanyOnlyAndUnlinkContacts() throws Exception {
            //GIVEN
            final String companyId = createCompany("Has Contacts");
            final String contactJson = """
                    {"firstName": "John", "lastName": "Doe", "language": "EN", "companyId": "%s"}
                    """.formatted(companyId);
            mockMvc.perform(post("/api/contacts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(contactJson).with(testJwt()));

            //WHEN
            final var result = mockMvc.perform(delete("/api/companies/" + companyId).with(testJwt()));

            //THEN
            result.andExpect(status().isNoContent());
            mockMvc.perform(get("/api/companies/" + companyId).with(testJwt()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 for non-existent ID")
        void shouldReturn404() throws Exception {
            //GIVEN
            //  no company exists

            //WHEN
            final var result = mockMvc.perform(delete("/api/companies/00000000-0000-0000-0000-000000000001").with(testJwt()));

            //THEN
            result.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/companies (list)")
    class ListCompanies {

        @Test
        @DisplayName("should return default pagination with 20 items")
        void shouldReturnDefaultPagination() throws Exception {
            //GIVEN
            for (int i = 0; i < 25; i++) {
                createCompany("Company " + i);
            }

            //WHEN
            final var result = mockMvc.perform(get("/api/companies").with(testJwt()));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(20)))
                    .andExpect(jsonPath("$.page.totalElements").value(25))
                    .andExpect(jsonPath("$.page.totalPages").value(2));
        }

        @Test
        @DisplayName("should support custom page size")
        void shouldSupportCustomPageSize() throws Exception {
            //GIVEN
            for (int i = 0; i < 25; i++) {
                createCompany("Company " + i);
            }

            //WHEN
            final var result = mockMvc.perform(get("/api/companies?page=0&size=10").with(testJwt()));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(10)))
                    .andExpect(jsonPath("$.page.totalElements").value(25));
        }

        @Test
        @DisplayName("should filter by name (partial, case-insensitive)")
        void shouldFilterByName() throws Exception {
            //GIVEN
            createCompany("Open Elements");
            createCompany("Acme Corp");

            //WHEN
            final var result = mockMvc.perform(get("/api/companies?name=open").with(testJwt()));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name").value("Open Elements"));
        }

        @Test
        @DisplayName("should sort by name ascending")
        void shouldSortByName() throws Exception {
            //GIVEN
            createCompany("Zebra Inc");
            createCompany("Alpha GmbH");

            //WHEN
            final var result = mockMvc.perform(get("/api/companies?sort=name,asc").with(testJwt()));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Alpha GmbH"))
                    .andExpect(jsonPath("$.content[1].name").value("Zebra Inc"));
        }

        @Test
        @DisplayName("should sort by createdAt descending")
        void shouldSortByCreatedAt() throws Exception {
            //GIVEN
            createCompany("First");
            createCompany("Second");

            //WHEN
            final var result = mockMvc.perform(get("/api/companies?sort=createdAt,desc").with(testJwt()));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Second"))
                    .andExpect(jsonPath("$.content[1].name").value("First"));
        }

        @Test
        @DisplayName("should filter by brevo=true")
        void shouldFilterByBrevoTrue() throws Exception {
            //GIVEN
            createCompany("Normal Corp");
            final CompanyEntity brevoEntity = new CompanyEntity();
            brevoEntity.setName("Brevo Corp");
            brevoEntity.setBrevoCompanyId("brevo-123");
            companyRepository.saveAndFlush(brevoEntity);

            //WHEN
            final var result = mockMvc.perform(get("/api/companies?brevo=true").with(testJwt()));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name").value("Brevo Corp"));
        }

        @Test
        @DisplayName("should filter by brevo=false")
        void shouldFilterByBrevoFalse() throws Exception {
            //GIVEN
            createCompany("Normal Corp");
            final CompanyEntity brevoEntity = new CompanyEntity();
            brevoEntity.setName("Brevo Corp");
            brevoEntity.setBrevoCompanyId("brevo-123");
            companyRepository.saveAndFlush(brevoEntity);

            //WHEN
            final var result = mockMvc.perform(get("/api/companies?brevo=false").with(testJwt()));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name").value("Normal Corp"));
        }
    }

    @Nested
    @DisplayName("Company Logo")
    class CompanyLogo {

        @Test
        @DisplayName("should upload PNG logo and return 200")
        void shouldUploadPngLogo() throws Exception {
            //GIVEN
            final String id = createCompany("Logo Corp");
            final MockMultipartFile file = new MockMultipartFile(
                    "file", "logo.png", "image/png", new byte[]{1, 2, 3});

            //WHEN
            final var result = mockMvc.perform(multipart("/api/companies/" + id + "/logo").file(file).with(testJwt()));

            //THEN
            result.andExpect(status().isOk());
        }

        @Test
        @DisplayName("should upload JPEG logo and return 200")
        void shouldUploadJpegLogo() throws Exception {
            //GIVEN
            final String id = createCompany("JPEG Corp");
            final MockMultipartFile file = new MockMultipartFile(
                    "file", "logo.jpg", "image/jpeg", new byte[]{1, 2, 3});

            //WHEN
            final var result = mockMvc.perform(multipart("/api/companies/" + id + "/logo").file(file).with(testJwt()));

            //THEN
            result.andExpect(status().isOk());
        }

        @Test
        @DisplayName("should upload SVG logo and return 200")
        void shouldUploadSvgLogo() throws Exception {
            //GIVEN
            final String id = createCompany("SVG Corp");
            final MockMultipartFile file = new MockMultipartFile(
                    "file", "logo.svg", "image/svg+xml", "<svg/>".getBytes());

            //WHEN
            final var result = mockMvc.perform(multipart("/api/companies/" + id + "/logo").file(file).with(testJwt()));

            //THEN
            result.andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return logo with correct content type")
        void shouldGetLogoWithCorrectContentType() throws Exception {
            //GIVEN
            final String id = createCompany("Get Logo Corp");
            final byte[] imageBytes = new byte[]{1, 2, 3, 4, 5};
            final MockMultipartFile file = new MockMultipartFile(
                    "file", "logo.png", "image/png", imageBytes);
            mockMvc.perform(multipart("/api/companies/" + id + "/logo").file(file).with(testJwt()));

            //WHEN
            final var result = mockMvc.perform(get("/api/companies/" + id + "/logo").with(testJwt()));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "image/png"))
                    .andExpect(content().bytes(imageBytes));
        }

        @Test
        @DisplayName("should return 404 when no logo exists")
        void shouldReturn404WhenNoLogo() throws Exception {
            //GIVEN
            final String id = createCompany("No Logo Corp");

            //WHEN
            final var result = mockMvc.perform(get("/api/companies/" + id + "/logo").with(testJwt()));

            //THEN
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 for non-existent company")
        void shouldReturn404ForNonExistentCompany() throws Exception {
            //GIVEN
            //  no company exists

            //WHEN
            final var result = mockMvc.perform(get("/api/companies/00000000-0000-0000-0000-000000000001/logo").with(testJwt()));

            //THEN
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 for invalid content type")
        void shouldReturn400ForInvalidContentType() throws Exception {
            //GIVEN
            final String id = createCompany("Invalid Type Corp");
            final MockMultipartFile file = new MockMultipartFile(
                    "file", "logo.gif", "image/gif", new byte[]{1, 2, 3});

            //WHEN
            final var result = mockMvc.perform(multipart("/api/companies/" + id + "/logo").file(file).with(testJwt()));

            //THEN
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should delete logo and return 204")
        void shouldDeleteLogo() throws Exception {
            //GIVEN
            final String id = createCompany("Delete Logo Corp");
            final MockMultipartFile file = new MockMultipartFile(
                    "file", "logo.png", "image/png", new byte[]{1, 2, 3});
            mockMvc.perform(multipart("/api/companies/" + id + "/logo").file(file).with(testJwt()));

            //WHEN
            final var result = mockMvc.perform(delete("/api/companies/" + id + "/logo").with(testJwt()));

            //THEN
            result.andExpect(status().isNoContent());
            mockMvc.perform(get("/api/companies/" + id + "/logo").with(testJwt()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should replace existing logo")
        void shouldReplaceExistingLogo() throws Exception {
            //GIVEN
            final String id = createCompany("Replace Logo Corp");
            final MockMultipartFile oldFile = new MockMultipartFile(
                    "file", "old.png", "image/png", new byte[]{1, 2, 3});
            mockMvc.perform(multipart("/api/companies/" + id + "/logo").file(oldFile).with(testJwt()));

            final byte[] newBytes = new byte[]{4, 5, 6, 7};
            final MockMultipartFile newFile = new MockMultipartFile(
                    "file", "new.jpg", "image/jpeg", newBytes);

            //WHEN
            mockMvc.perform(multipart("/api/companies/" + id + "/logo").file(newFile).with(testJwt()));

            //THEN
            mockMvc.perform(get("/api/companies/" + id + "/logo").with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "image/jpeg"))
                    .andExpect(content().bytes(newBytes));
        }

        @Test
        @DisplayName("should set hasLogo to true when logo exists")
        void shouldSetHasLogoTrue() throws Exception {
            //GIVEN
            final String id = createCompany("HasLogo Corp");
            final MockMultipartFile file = new MockMultipartFile(
                    "file", "logo.png", "image/png", new byte[]{1, 2, 3});
            mockMvc.perform(multipart("/api/companies/" + id + "/logo").file(file).with(testJwt()));

            //WHEN
            final var result = mockMvc.perform(get("/api/companies/" + id).with(testJwt()));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasLogo").value(true));
        }

        @Test
        @DisplayName("should set hasLogo to false when no logo exists")
        void shouldSetHasLogoFalse() throws Exception {
            //GIVEN
            final String id = createCompany("NoLogo Corp");

            //WHEN
            final var result = mockMvc.perform(get("/api/companies/" + id).with(testJwt()));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasLogo").value(false));
        }

        @Test
        @DisplayName("should delete logo when company is hard-deleted")
        void shouldDeleteLogoOnHardDelete() throws Exception {
            //GIVEN
            final String id = createCompany("HardDelete Logo Corp");
            final byte[] imageBytes = new byte[]{10, 20, 30};
            final MockMultipartFile file = new MockMultipartFile(
                    "file", "logo.png", "image/png", imageBytes);
            mockMvc.perform(multipart("/api/companies/" + id + "/logo").file(file).with(testJwt()));

            //WHEN
            mockMvc.perform(delete("/api/companies/" + id).with(testJwt()));

            //THEN
            mockMvc.perform(get("/api/companies/" + id).with(testJwt()))
                    .andExpect(status().isNotFound());
        }
    }
}
