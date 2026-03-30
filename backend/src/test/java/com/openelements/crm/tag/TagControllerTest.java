package com.openelements.crm.tag;

import com.openelements.crm.comment.CommentRepository;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static com.openelements.crm.TestSecurityUtil.testJwt;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Tag Controller")
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        contactRepository.deleteAll();
        companyRepository.deleteAll();
        tagRepository.deleteAll();
    }

    private String createTag(final String name, final String color) throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/tags")
                        .with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "color": "%s"}
                                """.formatted(name, color)))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private String extractId(final String json) {
        return json.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }

    @Nested
    @DisplayName("Tag CRUD")
    class TagCrud {

        @Test
        @DisplayName("Create a tag")
        void shouldCreateTag() throws Exception {
            mockMvc.perform(post("/api/tags")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "VIP", "description": "Important clients", "color": "#E63277"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("VIP"))
                    .andExpect(jsonPath("$.description").value("Important clients"))
                    .andExpect(jsonPath("$.color").value("#E63277"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        @DisplayName("Create a tag without description")
        void createTagWithoutDescription() throws Exception {
            mockMvc.perform(post("/api/tags")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Lead", "color": "#5CBA9E"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.description").doesNotExist());
        }

        @Test
        @DisplayName("Create a tag with duplicate name fails")
        void createTagDuplicateName() throws Exception {
            createTag("VIP", "#E63277");
            mockMvc.perform(post("/api/tags")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "VIP", "color": "#000000"}
                                    """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Create a tag without name fails")
        void createTagWithoutName() throws Exception {
            mockMvc.perform(post("/api/tags")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"color": "#5CBA9E"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Create a tag without color fails")
        void createTagWithoutColor() throws Exception {
            mockMvc.perform(post("/api/tags")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Get a tag by ID")
        void getTagById() throws Exception {
            final String id = extractId(createTag("VIP", "#E63277"));
            mockMvc.perform(get("/api/tags/" + id).with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("VIP"));
        }

        @Test
        @DisplayName("Get a non-existent tag returns 404")
        void getNonExistentTag() throws Exception {
            mockMvc.perform(get("/api/tags/" + UUID.randomUUID()).with(testJwt()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("List all tags paginated")
        void listTags() throws Exception {
            for (int i = 1; i <= 25; i++) {
                createTag("Tag-" + String.format("%02d", i), "#000000");
            }
            mockMvc.perform(get("/api/tags").with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(20)))
                    .andExpect(jsonPath("$.page.totalElements").value(25));
        }

        @Test
        @DisplayName("Update a tag")
        void updateTag() throws Exception {
            final String id = extractId(createTag("VIP", "#E63277"));
            mockMvc.perform(put("/api/tags/" + id)
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Premium", "color": "#5DB9F5"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Premium"))
                    .andExpect(jsonPath("$.color").value("#5DB9F5"));
        }

        @Test
        @DisplayName("Update a tag with duplicate name fails")
        void updateTagDuplicateName() throws Exception {
            createTag("VIP", "#E63277");
            final String leadId = extractId(createTag("Lead", "#5CBA9E"));
            mockMvc.perform(put("/api/tags/" + leadId)
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "VIP", "color": "#000000"}
                                    """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Update a non-existent tag returns 404")
        void updateNonExistentTag() throws Exception {
            mockMvc.perform(put("/api/tags/" + UUID.randomUUID())
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test", "color": "#000000"}
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Delete a tag")
        void shouldDeleteTag() throws Exception {
            final String id = extractId(createTag("VIP", "#E63277"));
            mockMvc.perform(delete("/api/tags/" + id).with(testJwt()))
                    .andExpect(status().isNoContent());
            mockMvc.perform(get("/api/tags/" + id).with(testJwt()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Delete a non-existent tag returns 404")
        void deleteNonExistentTag() throws Exception {
            mockMvc.perform(delete("/api/tags/" + UUID.randomUUID()).with(testJwt()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Tag Assignment via Company")
    class CompanyTags {

        @Test
        @DisplayName("Create company with tags")
        void createCompanyWithTags() throws Exception {
            final String vipId = extractId(createTag("VIP", "#E63277"));
            final String leadId = extractId(createTag("Lead", "#5CBA9E"));
            mockMvc.perform(post("/api/companies")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test Corp", "tagIds": ["%s", "%s"]}
                                    """.formatted(vipId, leadId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tagIds", hasSize(2)));
        }

        @Test
        @DisplayName("Create company without tags")
        void createCompanyWithoutTags() throws Exception {
            mockMvc.perform(post("/api/companies")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test Corp"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tagIds", hasSize(0)));
        }

        @Test
        @DisplayName("Update company: add tags")
        void updateCompanyAddTags() throws Exception {
            final String vipId = extractId(createTag("VIP", "#E63277"));
            final MvcResult companyResult = mockMvc.perform(post("/api/companies")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test Corp"}
                                    """))
                    .andReturn();
            final String companyId = extractId(companyResult.getResponse().getContentAsString());
            mockMvc.perform(put("/api/companies/" + companyId)
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test Corp", "tagIds": ["%s"]}
                                    """.formatted(vipId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tagIds", hasSize(1)));
        }

        @Test
        @DisplayName("Update company: replace tags")
        void updateCompanyReplaceTags() throws Exception {
            final String vipId = extractId(createTag("VIP", "#E63277"));
            final String leadId = extractId(createTag("Lead", "#5CBA9E"));
            final String premiumId = extractId(createTag("Premium", "#5DB9F5"));
            final MvcResult companyResult = mockMvc.perform(post("/api/companies")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test Corp", "tagIds": ["%s", "%s"]}
                                    """.formatted(vipId, leadId)))
                    .andReturn();
            final String companyId = extractId(companyResult.getResponse().getContentAsString());
            mockMvc.perform(put("/api/companies/" + companyId)
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test Corp", "tagIds": ["%s"]}
                                    """.formatted(premiumId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tagIds", hasSize(1)));
        }

        @Test
        @DisplayName("Update company: remove all tags")
        void updateCompanyRemoveAllTags() throws Exception {
            final String vipId = extractId(createTag("VIP", "#E63277"));
            final MvcResult companyResult = mockMvc.perform(post("/api/companies")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test Corp", "tagIds": ["%s"]}
                                    """.formatted(vipId)))
                    .andReturn();
            final String companyId = extractId(companyResult.getResponse().getContentAsString());
            mockMvc.perform(put("/api/companies/" + companyId)
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test Corp", "tagIds": []}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tagIds", hasSize(0)));
        }

        @Test
        @DisplayName("Update company: null tagIds preserves existing tags")
        void updateCompanyNullTagIdsPreserves() throws Exception {
            final String vipId = extractId(createTag("VIP", "#E63277"));
            final MvcResult companyResult = mockMvc.perform(post("/api/companies")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test Corp", "tagIds": ["%s"]}
                                    """.formatted(vipId)))
                    .andReturn();
            final String companyId = extractId(companyResult.getResponse().getContentAsString());
            // Update without tagIds field
            mockMvc.perform(put("/api/companies/" + companyId)
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Updated Corp"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tagIds", hasSize(1)));
        }

        @Test
        @DisplayName("Company response includes tagIds")
        void companyResponseIncludesTagIds() throws Exception {
            final String vipId = extractId(createTag("VIP", "#E63277"));
            final MvcResult companyResult = mockMvc.perform(post("/api/companies")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test Corp", "tagIds": ["%s"]}
                                    """.formatted(vipId)))
                    .andReturn();
            final String companyId = extractId(companyResult.getResponse().getContentAsString());
            mockMvc.perform(get("/api/companies/" + companyId).with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tagIds", hasSize(1)));
        }

        @Test
        @DisplayName("Company list response includes tagIds")
        void companyListIncludesTagIds() throws Exception {
            final String vipId = extractId(createTag("VIP", "#E63277"));
            mockMvc.perform(post("/api/companies")
                    .with(testJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"name": "Test Corp", "tagIds": ["%s"]}
                            """.formatted(vipId)));
            mockMvc.perform(get("/api/companies").with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].tagIds").exists());
        }
    }

    @Nested
    @DisplayName("Tag Assignment via Contact")
    class ContactTags {

        @Test
        @DisplayName("Create contact with tags")
        void createContactWithTags() throws Exception {
            final String vipId = extractId(createTag("VIP", "#E63277"));
            final String leadId = extractId(createTag("Lead", "#5CBA9E"));
            mockMvc.perform(post("/api/contacts")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "John", "lastName": "Doe", "tagIds": ["%s", "%s"]}
                                    """.formatted(vipId, leadId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tagIds", hasSize(2)));
        }

        @Test
        @DisplayName("Update contact: null tagIds preserves existing tags")
        void updateContactNullTagIds() throws Exception {
            final String vipId = extractId(createTag("VIP", "#E63277"));
            final MvcResult contactResult = mockMvc.perform(post("/api/contacts")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "John", "lastName": "Doe", "tagIds": ["%s"]}
                                    """.formatted(vipId)))
                    .andReturn();
            final String contactId = extractId(contactResult.getResponse().getContentAsString());
            mockMvc.perform(put("/api/contacts/" + contactId)
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "John", "lastName": "Doe"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tagIds", hasSize(1)));
        }

        @Test
        @DisplayName("Update contact: empty list removes all tags")
        void updateContactEmptyTags() throws Exception {
            final String vipId = extractId(createTag("VIP", "#E63277"));
            final MvcResult contactResult = mockMvc.perform(post("/api/contacts")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "John", "lastName": "Doe", "tagIds": ["%s"]}
                                    """.formatted(vipId)))
                    .andReturn();
            final String contactId = extractId(contactResult.getResponse().getContentAsString());
            mockMvc.perform(put("/api/contacts/" + contactId)
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "John", "lastName": "Doe", "tagIds": []}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tagIds", hasSize(0)));
        }

        @Test
        @DisplayName("Contact response includes tagIds")
        void contactResponseIncludesTagIds() throws Exception {
            final String leadId = extractId(createTag("Lead", "#5CBA9E"));
            final MvcResult contactResult = mockMvc.perform(post("/api/contacts")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "John", "lastName": "Doe", "tagIds": ["%s"]}
                                    """.formatted(leadId)))
                    .andReturn();
            final String contactId = extractId(contactResult.getResponse().getContentAsString());
            mockMvc.perform(get("/api/contacts/" + contactId).with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tagIds", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("Cascade Delete")
    class CascadeDelete {

        @Test
        @DisplayName("Deleting a tag removes company assignments")
        void deleteTagRemovesCompanyAssignments() throws Exception {
            final String vipId = extractId(createTag("VIP", "#E63277"));
            // Create 3 companies with this tag
            for (int i = 1; i <= 3; i++) {
                mockMvc.perform(post("/api/companies")
                        .with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Company %d", "tagIds": ["%s"]}
                                """.formatted(i, vipId)));
            }
            // Delete the tag
            mockMvc.perform(delete("/api/tags/" + vipId).with(testJwt()))
                    .andExpect(status().isNoContent());
            // Companies still exist but without the tag
            mockMvc.perform(get("/api/companies").with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.content[0].tagIds", hasSize(0)));
        }

        @Test
        @DisplayName("Deleting a tag removes contact assignments")
        void deleteTagRemovesContactAssignments() throws Exception {
            final String vipId = extractId(createTag("VIP", "#E63277"));
            for (int i = 1; i <= 2; i++) {
                mockMvc.perform(post("/api/contacts")
                        .with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Contact", "lastName": "%d", "tagIds": ["%s"]}
                                """.formatted(i, vipId)));
            }
            mockMvc.perform(delete("/api/tags/" + vipId).with(testJwt()))
                    .andExpect(status().isNoContent());
            mockMvc.perform(get("/api/contacts").with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].tagIds", hasSize(0)));
        }

        @Test
        @DisplayName("Deleting a company removes its tag assignments")
        void deleteCompanyRemovesTagAssignments() throws Exception {
            final String vipId = extractId(createTag("VIP", "#E63277"));
            final MvcResult companyResult = mockMvc.perform(post("/api/companies")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test Corp", "tagIds": ["%s"]}
                                    """.formatted(vipId)))
                    .andReturn();
            final String companyId = extractId(companyResult.getResponse().getContentAsString());
            mockMvc.perform(delete("/api/companies/" + companyId).with(testJwt()))
                    .andExpect(status().isNoContent());
            // Tag still exists
            mockMvc.perform(get("/api/tags/" + vipId).with(testJwt()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Deleting a contact removes its tag assignments")
        void deleteContactRemovesTagAssignments() throws Exception {
            final String leadId = extractId(createTag("Lead", "#5CBA9E"));
            final MvcResult contactResult = mockMvc.perform(post("/api/contacts")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "John", "lastName": "Doe", "tagIds": ["%s"]}
                                    """.formatted(leadId)))
                    .andReturn();
            final String contactId = extractId(contactResult.getResponse().getContentAsString());
            mockMvc.perform(delete("/api/contacts/" + contactId).with(testJwt()))
                    .andExpect(status().isNoContent());
            // Tag still exists
            mockMvc.perform(get("/api/tags/" + leadId).with(testJwt()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Invalid Tag IDs")
    class InvalidTagIds {

        @Test
        @DisplayName("Create company with non-existent tag ID")
        void createCompanyWithFakeTagId() throws Exception {
            mockMvc.perform(post("/api/companies")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test Corp", "tagIds": ["%s"]}
                                    """.formatted(UUID.randomUUID())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Update contact with non-existent tag ID")
        void updateContactWithFakeTagId() throws Exception {
            final MvcResult contactResult = mockMvc.perform(post("/api/contacts")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "John", "lastName": "Doe"}
                                    """))
                    .andReturn();
            final String contactId = extractId(contactResult.getResponse().getContentAsString());
            mockMvc.perform(put("/api/contacts/" + contactId)
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "John", "lastName": "Doe", "tagIds": ["%s"]}
                                    """.formatted(UUID.randomUUID())))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Backward Compatibility")
    class BackwardCompatibility {

        @Test
        @DisplayName("Existing frontend can update company without tagIds")
        void updateCompanyWithoutTagIds() throws Exception {
            final String vipId = extractId(createTag("VIP", "#E63277"));
            final MvcResult companyResult = mockMvc.perform(post("/api/companies")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test Corp", "tagIds": ["%s"]}
                                    """.formatted(vipId)))
                    .andReturn();
            final String companyId = extractId(companyResult.getResponse().getContentAsString());
            // Frontend sends update without tagIds field
            mockMvc.perform(put("/api/companies/" + companyId)
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Updated Corp"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tagIds", hasSize(1)));
        }

        @Test
        @DisplayName("Existing frontend can create company without tagIds")
        void createCompanyWithoutTagIds() throws Exception {
            mockMvc.perform(post("/api/companies")
                            .with(testJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Test Corp"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tagIds", hasSize(0)));
        }
    }
}
