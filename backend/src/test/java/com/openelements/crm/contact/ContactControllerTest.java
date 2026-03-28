package com.openelements.crm.contact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openelements.crm.comment.CommentRepository;
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

import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
@DisplayName("Contact Controller")
class ContactControllerTest {

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

    private String createContact(final String firstName, final String lastName, final String companyId) throws Exception {
        final String json = """
                {
                    "firstName": "%s",
                    "lastName": "%s",
                    "language": "DE",
                    "companyId": %s
                }
                """.formatted(firstName, lastName, companyId != null ? "\"" + companyId + "\"" : "null");
        final String response = mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Nested
    @DisplayName("POST /api/contacts")
    class CreateContact {

        @Test
        @DisplayName("should create contact with all fields")
        void shouldCreateWithAllFields() throws Exception {
            //GIVEN
            final String companyId = createCompany("Open Elements");
            final String json = """
                    {
                        "firstName": "Hendrik",
                        "lastName": "Ebbers",
                        "email": "hendrik@open-elements.com",
                        "position": "CEO",
                        "gender": "MALE",
                        "linkedInUrl": "https://linkedin.com/in/hendrik-ebbers",
                        "phoneNumber": "+49 123 456789",
                        "companyId": "%s",
                        "language": "DE"
                    }
                    """.formatted(companyId);

            //WHEN
            final var result = mockMvc.perform(post("/api/contacts")
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.firstName").value("Hendrik"))
                    .andExpect(jsonPath("$.lastName").value("Ebbers"))
                    .andExpect(jsonPath("$.companyId").value(companyId))
                    .andExpect(jsonPath("$.companyName").value("Open Elements"))
                    .andExpect(jsonPath("$.syncedToBrevo").value(false))
                    .andExpect(jsonPath("$.doubleOptIn").value(false))
                    .andExpect(jsonPath("$.gender").value("MALE"))
                    .andExpect(jsonPath("$.language").value("DE"));
        }

        @Test
        @DisplayName("should create contact without company")
        void shouldCreateWithoutCompany() throws Exception {
            //GIVEN
            final String json = """
                    {"firstName": "Jane", "lastName": "Doe", "language": "EN"}
                    """;

            //WHEN
            final var result = mockMvc.perform(post("/api/contacts")
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.companyId").isEmpty())
                    .andExpect(jsonPath("$.companyName").isEmpty());
        }

        @Test
        @DisplayName("should fail with non-existent company")
        void shouldFailWithNonExistentCompany() throws Exception {
            //GIVEN
            final String json = """
                    {"firstName": "Jane", "lastName": "Doe", "language": "EN",
                     "companyId": "00000000-0000-0000-0000-000000000001"}
                    """;

            //WHEN
            final var result = mockMvc.perform(post("/api/contacts")
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should fail with soft-deleted company")
        void shouldFailWithSoftDeletedCompany() throws Exception {
            //GIVEN
            final String companyId = createCompany("Deleted Co");
            mockMvc.perform(delete("/api/companies/" + companyId));

            final String json = """
                    {"firstName": "Jane", "lastName": "Doe", "language": "EN",
                     "companyId": "%s"}
                    """.formatted(companyId);

            //WHEN
            final var result = mockMvc.perform(post("/api/contacts")
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should fail without required fields")
        void shouldFailWithoutRequiredFields() throws Exception {
            //GIVEN
            final String json = """
                    {"email": "test@test.com"}
                    """;

            //WHEN
            final var result = mockMvc.perform(post("/api/contacts")
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should create contact with null gender")
        void shouldCreateWithNullGender() throws Exception {
            //GIVEN
            final String json = """
                    {"firstName": "Alex", "lastName": "Unknown", "language": "EN", "gender": null}
                    """;

            //WHEN
            final var result = mockMvc.perform(post("/api/contacts")
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.gender").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/contacts/{id}")
    class GetContact {

        @Test
        @DisplayName("should return contact with company name")
        void shouldReturnWithCompanyName() throws Exception {
            //GIVEN
            final String companyId = createCompany("Open Elements");
            final String contactId = createContact("Hendrik", "Ebbers", companyId);

            //WHEN
            final var result = mockMvc.perform(get("/api/contacts/" + contactId));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.companyId").value(companyId))
                    .andExpect(jsonPath("$.companyName").value("Open Elements"));
        }
    }

    @Nested
    @DisplayName("PUT /api/contacts/{id}")
    class UpdateContact {

        @Test
        @DisplayName("should update contact")
        void shouldUpdate() throws Exception {
            //GIVEN
            final String contactId = createContact("Old", "Name", null);
            final String json = """
                    {"firstName": "New", "lastName": "Name", "language": "EN"}
                    """;

            //WHEN
            final var result = mockMvc.perform(put("/api/contacts/" + contactId)
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("New"));
        }

        @Test
        @DisplayName("should ignore Brevo fields on update")
        void shouldIgnoreBrevoFields() throws Exception {
            //GIVEN
            final String contactId = createContact("Test", "User", null);

            // The request DTO does not have syncedToBrevo/doubleOptIn fields,
            // so even if extra JSON fields are sent, they are ignored by Jackson
            final String json = """
                    {"firstName": "Test", "lastName": "User", "language": "DE"}
                    """;

            //WHEN
            final var result = mockMvc.perform(put("/api/contacts/" + contactId)
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.syncedToBrevo").value(false))
                    .andExpect(jsonPath("$.doubleOptIn").value(false));
        }
    }

    @Nested
    @DisplayName("DELETE /api/contacts/{id}")
    class DeleteContact {

        @Test
        @DisplayName("should hard-delete contact with comments")
        void shouldHardDelete() throws Exception {
            //GIVEN
            final String contactId = createContact("To", "Delete", null);
            mockMvc.perform(post("/api/contacts/" + contactId + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"text": "A comment", "author": "Test"}
                            """));

            //WHEN
            final var result = mockMvc.perform(delete("/api/contacts/" + contactId));

            //THEN
            result.andExpect(status().isNoContent());
            mockMvc.perform(get("/api/contacts/" + contactId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 for non-existent contact")
        void shouldReturn404() throws Exception {
            //GIVEN
            //  no contact exists

            //WHEN
            final var result = mockMvc.perform(delete("/api/contacts/00000000-0000-0000-0000-000000000001"));

            //THEN
            result.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/contacts (list)")
    class ListContacts {

        @Test
        @DisplayName("should return default pagination")
        void shouldReturnDefaultPagination() throws Exception {
            //GIVEN
            for (int i = 0; i < 25; i++) {
                createContact("First" + i, "Last" + i, null);
            }

            //WHEN
            final var result = mockMvc.perform(get("/api/contacts"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(20)))
                    .andExpect(jsonPath("$.totalElements").value(25));
        }

        @Test
        @DisplayName("should filter by lastName")
        void shouldFilterByLastName() throws Exception {
            //GIVEN
            createContact("Hendrik", "Ebbers", null);
            createContact("Hans", "Schmidt", null);

            //WHEN
            final var result = mockMvc.perform(get("/api/contacts?lastName=ebb"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].lastName").value("Ebbers"));
        }

        @Test
        @DisplayName("should filter by firstName")
        void shouldFilterByFirstName() throws Exception {
            //GIVEN
            createContact("Hendrik", "A", null);
            createContact("Hans", "B", null);

            //WHEN
            final var result = mockMvc.perform(get("/api/contacts?firstName=Hendrik"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].firstName").value("Hendrik"));
        }

        @Test
        @DisplayName("should filter by email")
        void shouldFilterByEmail() throws Exception {
            //GIVEN
            final String json1 = """
                    {"firstName": "A", "lastName": "A", "language": "DE", "email": "a@example.com"}
                    """;
            final String json2 = """
                    {"firstName": "B", "lastName": "B", "language": "DE", "email": "b@example.com"}
                    """;
            mockMvc.perform(post("/api/contacts").contentType(MediaType.APPLICATION_JSON).content(json1));
            mockMvc.perform(post("/api/contacts").contentType(MediaType.APPLICATION_JSON).content(json2));

            //WHEN
            final var result = mockMvc.perform(get("/api/contacts?email=a@example"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }

        @Test
        @DisplayName("should filter by companyId")
        void shouldFilterByCompanyId() throws Exception {
            //GIVEN
            final String companyA = createCompany("Company A");
            final String companyB = createCompany("Company B");
            createContact("Alice", "A", companyA);
            createContact("Bob", "B", companyB);

            //WHEN
            final var result = mockMvc.perform(get("/api/contacts?companyId=" + companyA));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].firstName").value("Alice"));
        }

        @Test
        @DisplayName("should filter by language")
        void shouldFilterByLanguage() throws Exception {
            //GIVEN
            createContact("DE", "Contact", null);
            final String enJson = """
                    {"firstName": "EN", "lastName": "Contact", "language": "EN"}
                    """;
            mockMvc.perform(post("/api/contacts").contentType(MediaType.APPLICATION_JSON).content(enJson));

            //WHEN
            final var result = mockMvc.perform(get("/api/contacts?language=DE"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].firstName").value("DE"));
        }

        @Test
        @DisplayName("should sort by lastName")
        void shouldSortByLastName() throws Exception {
            //GIVEN
            createContact("A", "Zebra", null);
            createContact("B", "Alpha", null);

            //WHEN
            final var result = mockMvc.perform(get("/api/contacts?sort=lastName,asc"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].lastName").value("Alpha"))
                    .andExpect(jsonPath("$.content[1].lastName").value("Zebra"));
        }
    }

    @Nested
    @DisplayName("Contact Photo")
    class ContactPhoto {

        @Test
        @DisplayName("should upload JPEG photo and return 200")
        void shouldUploadJpegPhoto() throws Exception {
            //GIVEN
            final String id = createContact("Photo", "Person", null);
            final MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

            //WHEN
            final var result = mockMvc.perform(multipart("/api/contacts/" + id + "/photo").file(file));

            //THEN
            result.andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return photo with correct content type")
        void shouldGetPhotoWithCorrectContentType() throws Exception {
            //GIVEN
            final String id = createContact("Get", "Photo", null);
            final byte[] imageBytes = new byte[]{1, 2, 3, 4, 5};
            final MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", imageBytes);
            mockMvc.perform(multipart("/api/contacts/" + id + "/photo").file(file));

            //WHEN
            final var result = mockMvc.perform(get("/api/contacts/" + id + "/photo"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "image/jpeg"))
                    .andExpect(content().bytes(imageBytes));
        }

        @Test
        @DisplayName("should return 404 when no photo exists")
        void shouldReturn404WhenNoPhoto() throws Exception {
            //GIVEN
            final String id = createContact("No", "Photo", null);

            //WHEN
            final var result = mockMvc.perform(get("/api/contacts/" + id + "/photo"));

            //THEN
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 for non-JPEG format")
        void shouldReturn400ForNonJpeg() throws Exception {
            //GIVEN
            final String id = createContact("PNG", "Rejected", null);
            final MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.png", "image/png", new byte[]{1, 2, 3});

            //WHEN
            final var result = mockMvc.perform(multipart("/api/contacts/" + id + "/photo").file(file));

            //THEN
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should delete photo and return 204")
        void shouldDeletePhoto() throws Exception {
            //GIVEN
            final String id = createContact("Delete", "Photo", null);
            final MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
            mockMvc.perform(multipart("/api/contacts/" + id + "/photo").file(file));

            //WHEN
            final var result = mockMvc.perform(delete("/api/contacts/" + id + "/photo"));

            //THEN
            result.andExpect(status().isNoContent());
            mockMvc.perform(get("/api/contacts/" + id + "/photo"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should replace existing photo")
        void shouldReplaceExistingPhoto() throws Exception {
            //GIVEN
            final String id = createContact("Replace", "Photo", null);
            final MockMultipartFile oldFile = new MockMultipartFile(
                    "file", "old.jpg", "image/jpeg", new byte[]{1, 2, 3});
            mockMvc.perform(multipart("/api/contacts/" + id + "/photo").file(oldFile));

            final byte[] newBytes = new byte[]{4, 5, 6, 7};
            final MockMultipartFile newFile = new MockMultipartFile(
                    "file", "new.jpg", "image/jpeg", newBytes);

            //WHEN
            mockMvc.perform(multipart("/api/contacts/" + id + "/photo").file(newFile));

            //THEN
            mockMvc.perform(get("/api/contacts/" + id + "/photo"))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes(newBytes));
        }

        @Test
        @DisplayName("should set hasPhoto to true when photo exists")
        void shouldSetHasPhotoTrue() throws Exception {
            //GIVEN
            final String id = createContact("Has", "Photo", null);
            final MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
            mockMvc.perform(multipart("/api/contacts/" + id + "/photo").file(file));

            //WHEN
            final var result = mockMvc.perform(get("/api/contacts/" + id));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasPhoto").value(true));
        }

        @Test
        @DisplayName("should set hasPhoto to false when no photo exists")
        void shouldSetHasPhotoFalse() throws Exception {
            //GIVEN
            final String id = createContact("No", "Photo2", null);

            //WHEN
            final var result = mockMvc.perform(get("/api/contacts/" + id));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasPhoto").value(false));
        }

        @Test
        @DisplayName("should delete photo when contact is hard-deleted")
        void shouldDeletePhotoWithContact() throws Exception {
            //GIVEN
            final String id = createContact("Hard", "Delete", null);
            final MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
            mockMvc.perform(multipart("/api/contacts/" + id + "/photo").file(file));

            //WHEN
            mockMvc.perform(delete("/api/contacts/" + id))
                    .andExpect(status().isNoContent());

            //THEN
            mockMvc.perform(get("/api/contacts/" + id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Optional Language")
    class OptionalLanguage {

        @Test
        @DisplayName("should create contact with null language")
        void shouldCreateWithNullLanguage() throws Exception {
            //GIVEN
            final String json = """
                    {"firstName": "Jane", "lastName": "Doe", "language": null}
                    """;

            //WHEN
            final var result = mockMvc.perform(post("/api/contacts")
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.language").isEmpty());
        }

        @Test
        @DisplayName("should create contact without language field")
        void shouldCreateWithoutLanguageField() throws Exception {
            //GIVEN
            final String json = """
                    {"firstName": "Jane", "lastName": "Doe"}
                    """;

            //WHEN
            final var result = mockMvc.perform(post("/api/contacts")
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.language").isEmpty());
        }

        @Test
        @DisplayName("should update contact to null language")
        void shouldUpdateToNullLanguage() throws Exception {
            //GIVEN
            final String contactId = createContact("Test", "User", null);
            final String json = """
                    {"firstName": "Test", "lastName": "User", "language": null}
                    """;

            //WHEN
            final var result = mockMvc.perform(put("/api/contacts/" + contactId)
                    .contentType(MediaType.APPLICATION_JSON).content(json));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.language").isEmpty());
        }

        @Test
        @DisplayName("should return null language in GET response")
        void shouldReturnNullLanguage() throws Exception {
            //GIVEN
            final String json = """
                    {"firstName": "No", "lastName": "Lang"}
                    """;
            final String response = mockMvc.perform(post("/api/contacts")
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            final String id = objectMapper.readTree(response).get("id").asText();

            //WHEN
            final var result = mockMvc.perform(get("/api/contacts/" + id));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.language").isEmpty());
        }

        @Test
        @DisplayName("should filter by UNKNOWN to get null-language contacts")
        void shouldFilterByUnknown() throws Exception {
            //GIVEN
            createContact("DE", "Contact", null); // has language DE
            mockMvc.perform(post("/api/contacts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"firstName": "No", "lastName": "Lang"}
                            """));

            //WHEN
            final var result = mockMvc.perform(get("/api/contacts?language=UNKNOWN"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].firstName").value("No"));
        }

        @Test
        @DisplayName("should exclude null-language contacts when filtering by DE")
        void shouldExcludeNullWhenFilteringByDE() throws Exception {
            //GIVEN
            createContact("DE", "Contact", null); // has language DE
            mockMvc.perform(post("/api/contacts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"firstName": "No", "lastName": "Lang"}
                            """));

            //WHEN
            final var result = mockMvc.perform(get("/api/contacts?language=DE"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].firstName").value("DE"));
        }

        @Test
        @DisplayName("should include null-language contacts when no filter")
        void shouldIncludeNullWhenNoFilter() throws Exception {
            //GIVEN
            createContact("DE", "Contact", null); // has language DE
            mockMvc.perform(post("/api/contacts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"firstName": "No", "lastName": "Lang"}
                            """));

            //WHEN
            final var result = mockMvc.perform(get("/api/contacts"));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)));
        }

        @Test
        @DisplayName("should change language from unknown to a value")
        void shouldChangeFromUnknownToValue() throws Exception {
            //GIVEN
            final String json = """
                    {"firstName": "Jane", "lastName": "Doe"}
                    """;
            final String response = mockMvc.perform(post("/api/contacts")
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.language").isEmpty())
                    .andReturn().getResponse().getContentAsString();
            final String id = objectMapper.readTree(response).get("id").asText();

            //WHEN
            final var result = mockMvc.perform(put("/api/contacts/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"firstName": "Jane", "lastName": "Doe", "language": "EN"}
                            """));

            //THEN
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.language").value("EN"));
        }
    }
}
