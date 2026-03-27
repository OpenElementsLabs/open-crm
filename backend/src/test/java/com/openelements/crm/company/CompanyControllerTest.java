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
                        .content(json))
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
                    .content(json));

            //THEN
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("Open Elements GmbH"))
                    .andExpect(jsonPath("$.email").value("info@open-elements.com"))
                    .andExpect(jsonPath("$.city").value("Berlin"))
                    .andExpect(jsonPath("$.deleted").value(false))
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
                    .content(json));

            //THEN
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Minimal Corp"))
                    .andExpect(jsonPath("$.email").isEmpty())
                    .andExpect(jsonPath("$.website").isEmpty())
                    .andExpect(jsonPath("$.deleted").value(false));
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
                    .content(json));

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
                    .content(json));

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
            final var result = mockMvc.perform(get("/api/companies/" + id));

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
            final var result = mockMvc.perform(get("/api/companies/00000000-0000-0000-0000-000000000001"));

            //THEN
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return soft-deleted company with deleted=true")
        void shouldReturnSoftDeletedCompany() throws Exception {
            //GIVEN
            final String id = createCompany("Deleted Corp");
            mockMvc.perform(delete("/api/companies/" + id));

            //WHEN
            final var result = mockMvc.perform(get("/api/companies/" + id));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.deleted").value(true));
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
                    .content(json));

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
                    .content(json));

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
                    .content(json));

            //THEN
            result.andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/companies/{id}")
    class DeleteCompany {

        @Test
        @DisplayName("should soft-delete company without contacts")
        void shouldSoftDelete() throws Exception {
            //GIVEN
            final String id = createCompany("To Delete");

            //WHEN
            final var result = mockMvc.perform(delete("/api/companies/" + id));

            //THEN
            result.andExpect(status().isNoContent());

            mockMvc.perform(get("/api/companies/" + id))
                    .andExpect(jsonPath("$.deleted").value(true));
        }

        @Test
        @DisplayName("should fail with 409 when contacts exist")
        void shouldFailWithContacts() throws Exception {
            //GIVEN
            final String companyId = createCompany("Has Contacts");
            final String contactJson = """
                    {"firstName": "John", "lastName": "Doe", "language": "EN", "companyId": "%s"}
                    """.formatted(companyId);
            mockMvc.perform(post("/api/contacts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(contactJson));

            //WHEN
            final var result = mockMvc.perform(delete("/api/companies/" + companyId));

            //THEN
            result.andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 404 for non-existent ID")
        void shouldReturn404() throws Exception {
            //GIVEN
            //  no company exists

            //WHEN
            final var result = mockMvc.perform(delete("/api/companies/00000000-0000-0000-0000-000000000001"));

            //THEN
            result.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/companies/{id}/restore")
    class RestoreCompany {

        @Test
        @DisplayName("should restore soft-deleted company")
        void shouldRestore() throws Exception {
            //GIVEN
            final String id = createCompany("Restore Me");
            mockMvc.perform(delete("/api/companies/" + id));

            //WHEN
            final var result = mockMvc.perform(post("/api/companies/" + id + "/restore"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.deleted").value(false));
        }

        @Test
        @DisplayName("should be idempotent for non-deleted company")
        void shouldBeIdempotent() throws Exception {
            //GIVEN
            final String id = createCompany("Not Deleted");

            //WHEN
            final var result = mockMvc.perform(post("/api/companies/" + id + "/restore"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.deleted").value(false));
        }

        @Test
        @DisplayName("should return 404 for non-existent ID")
        void shouldReturn404() throws Exception {
            //GIVEN
            //  no company exists

            //WHEN
            final var result = mockMvc.perform(post("/api/companies/00000000-0000-0000-0000-000000000001/restore"));

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
            final var result = mockMvc.perform(get("/api/companies"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(20)))
                    .andExpect(jsonPath("$.totalElements").value(25))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }

        @Test
        @DisplayName("should support custom page size")
        void shouldSupportCustomPageSize() throws Exception {
            //GIVEN
            for (int i = 0; i < 25; i++) {
                createCompany("Company " + i);
            }

            //WHEN
            final var result = mockMvc.perform(get("/api/companies?page=0&size=10"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(10)))
                    .andExpect(jsonPath("$.totalElements").value(25));
        }

        @Test
        @DisplayName("should exclude soft-deleted by default")
        void shouldExcludeSoftDeleted() throws Exception {
            //GIVEN
            createCompany("Active 1");
            createCompany("Active 2");
            final String deletedId = createCompany("To Delete");
            mockMvc.perform(delete("/api/companies/" + deletedId));

            //WHEN
            final var result = mockMvc.perform(get("/api/companies"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("should include soft-deleted with filter")
        void shouldIncludeSoftDeletedWithFilter() throws Exception {
            //GIVEN
            createCompany("Active");
            final String deletedId = createCompany("Deleted");
            mockMvc.perform(delete("/api/companies/" + deletedId));

            //WHEN
            final var result = mockMvc.perform(get("/api/companies?includeDeleted=true"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("should filter by name (partial, case-insensitive)")
        void shouldFilterByName() throws Exception {
            //GIVEN
            createCompany("Open Elements");
            createCompany("Acme Corp");

            //WHEN
            final var result = mockMvc.perform(get("/api/companies?name=open"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name").value("Open Elements"));
        }

        @Test
        @DisplayName("should filter by city")
        void shouldFilterByCity() throws Exception {
            //GIVEN
            final String berlinJson = createCompanyJson("Berlin Co", null, "Berlin", null);
            final String munichJson = createCompanyJson("Munich Co", null, "Munich", null);
            mockMvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON).content(berlinJson));
            mockMvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON).content(munichJson));

            //WHEN
            final var result = mockMvc.perform(get("/api/companies?city=Berlin"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name").value("Berlin Co"));
        }

        @Test
        @DisplayName("should filter by country")
        void shouldFilterByCountry() throws Exception {
            //GIVEN
            final String germanJson = createCompanyJson("German Co", null, null, "Germany");
            final String austrianJson = createCompanyJson("Austrian Co", null, null, "Austria");
            mockMvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON).content(germanJson));
            mockMvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON).content(austrianJson));

            //WHEN
            final var result = mockMvc.perform(get("/api/companies?country=Germany"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name").value("German Co"));
        }

        @Test
        @DisplayName("should sort by name ascending")
        void shouldSortByName() throws Exception {
            //GIVEN
            createCompany("Zebra Inc");
            createCompany("Alpha GmbH");

            //WHEN
            final var result = mockMvc.perform(get("/api/companies?sort=name,asc"));

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
            final var result = mockMvc.perform(get("/api/companies?sort=createdAt,desc"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Second"))
                    .andExpect(jsonPath("$.content[1].name").value("First"));
        }
    }
}
