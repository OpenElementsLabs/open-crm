package com.openelements.crm.opportunity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openelements.crm.AbstractDbTest;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.spring.base.security.roles.Roles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for the opportunity endpoints (spec 113): CRUD, validation,
 * defaults, list filters, comments, delete-blocking on referenced company/contact, tag counts,
 * the user-options endpoint, and audit logging.
 */
class OpportunityEndpointsIntegrationTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void seed() {
        seedSystemUser();
    }

    // -- Helpers -------------------------------------------------------------

    private static MockHttpServletRequestBuilder asUser(final MockHttpServletRequestBuilder builder,
                                                        final List<String> roles) {
        final Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("test-user")
            .claim("preferred_username", "test-user")
            .claim("name", "Test User")
            .claim("email", "test@example.com")
            .claim("roles", roles)
            .build();
        final Collection<GrantedAuthority> authorities = new ArrayList<>();
        for (final String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return builder.with(jwt().jwt(jwt).authorities(authorities));
    }

    private CompanyEntity newCompany(final String name) {
        final CompanyEntity company = new CompanyEntity();
        company.setName(name);
        return companyRepository.saveAndFlush(company);
    }

    private ContactEntity newContact(final String firstName, final String lastName) {
        final ContactEntity contact = new ContactEntity();
        contact.setFirstName(firstName);
        contact.setLastName(lastName);
        return contactRepository.saveAndFlush(contact);
    }

    private ContactEntity newContact(final String firstName, final String lastName, final CompanyEntity company) {
        final ContactEntity contact = new ContactEntity();
        contact.setFirstName(firstName);
        contact.setLastName(lastName);
        contact.setCompany(company);
        return contactRepository.saveAndFlush(contact);
    }

    private UUID newUser(final String userName, final String name) {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO users (id, sub, user_name, name, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            id, userName, userName, name);
        return id;
    }

    private UUID newTag(final String name) {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO tags (id, name, color, created_at, updated_at) "
                + "VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            id, name, "#112233");
        return id;
    }

    private String json(final Map<String, Object> payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }

    private Map<String, Object> minimalPayload(final UUID companyId, final UUID mainContactId) {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("title", "Deal");
        payload.put("companyId", companyId.toString());
        payload.put("mainContactId", mainContactId.toString());
        return payload;
    }

    private JsonNode createOk(final Map<String, Object> payload) throws Exception {
        final String body = mockMvc.perform(asUser(
                post("/api/opportunities").contentType(MediaType.APPLICATION_JSON).content(json(payload)),
                List.of()))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private UUID createOpportunity(final String title, final UUID companyId, final UUID mainContactId) throws Exception {
        final Map<String, Object> payload = minimalPayload(companyId, mainContactId);
        payload.put("title", title);
        return UUID.fromString(createOk(payload).get("id").asText());
    }

    // -- Creation ------------------------------------------------------------

    @Test
    void createWithAllFieldsReturnsFullDto() throws Exception {
        final CompanyEntity company = newCompany("Muster-Bank");
        final ContactEntity main = newContact("Max", "Muster");
        final ContactEntity additional = newContact("Erika", "Muster");
        final UUID owner = newUser("owner-1", "Owner One");
        final UUID tag = newTag("Prio");

        final Map<String, Object> payload = new HashMap<>();
        payload.put("title", "Muster-Bank – CRA Support & Care");
        payload.put("stage", "Angebot");
        payload.put("status", "WON");
        payload.put("product", "Support & Care");
        payload.put("estimatedValue", "25000.00");
        payload.put("companyId", company.getId().toString());
        payload.put("mainContactId", main.getId().toString());
        payload.put("additionalContactIds", List.of(additional.getId().toString()));
        payload.put("ownerId", owner.toString());
        payload.put("tagIds", List.of(tag.toString()));

        final JsonNode dto = createOk(payload);
        org.junit.jupiter.api.Assertions.assertEquals("Muster-Bank – CRA Support & Care", dto.get("title").asText());
        org.junit.jupiter.api.Assertions.assertEquals("Angebot", dto.get("stage").asText());
        org.junit.jupiter.api.Assertions.assertEquals("WON", dto.get("status").asText());
        org.junit.jupiter.api.Assertions.assertEquals("Support & Care", dto.get("product").asText());
        org.junit.jupiter.api.Assertions.assertEquals(company.getId().toString(), dto.get("companyId").asText());
        org.junit.jupiter.api.Assertions.assertEquals("Muster-Bank", dto.get("companyName").asText());
        org.junit.jupiter.api.Assertions.assertEquals(main.getId().toString(), dto.get("mainContactId").asText());
        org.junit.jupiter.api.Assertions.assertEquals(owner.toString(), dto.get("owner").get("id").asText());
        org.junit.jupiter.api.Assertions.assertEquals(0, dto.get("commentCount").asLong());
        org.junit.jupiter.api.Assertions.assertTrue(dto.get("additionalContactIds").toString().contains(additional.getId().toString()));
        org.junit.jupiter.api.Assertions.assertTrue(dto.get("tagIds").toString().contains(tag.toString()));

        // createdAt/updatedAt are set by Hibernate @CreationTimestamp on flush; assert on the re-fetch.
        mockMvc.perform(asUser(get("/api/opportunities/" + dto.get("id").asText()), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void createMinimalAppliesDefaults() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");

        final JsonNode dto = createOk(minimalPayload(company.getId(), main.getId()));
        org.junit.jupiter.api.Assertions.assertEquals("OPEN", dto.get("status").asText());
        org.junit.jupiter.api.Assertions.assertTrue(dto.get("stage").isNull());
        org.junit.jupiter.api.Assertions.assertTrue(dto.get("product").isNull());
        org.junit.jupiter.api.Assertions.assertTrue(dto.get("estimatedValue").isNull());
        // owner defaults to the current user (auto-provisioned from the JWT subject)
        org.junit.jupiter.api.Assertions.assertEquals("Test User", dto.get("owner").get("name").asText());
    }

    @Test
    void stageAcceptsAnyString() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final Map<String, Object> payload = minimalPayload(company.getId(), main.getId());
        payload.put("stage", "Kanban-Spalte-42");
        org.junit.jupiter.api.Assertions.assertEquals("Kanban-Spalte-42", createOk(payload).get("stage").asText());
    }

    @Test
    void missingTitleIsRejected() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final Map<String, Object> payload = minimalPayload(company.getId(), main.getId());
        payload.put("title", "  ");
        mockMvc.perform(asUser(
                post("/api/opportunities").contentType(MediaType.APPLICATION_JSON).content(json(payload)),
                List.of()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void missingCompanyOrMainContactIsRejected() throws Exception {
        final ContactEntity main = newContact("Max", "Muster");
        final Map<String, Object> payload = new HashMap<>();
        payload.put("title", "Deal");
        payload.put("mainContactId", main.getId().toString());
        mockMvc.perform(asUser(
                post("/api/opportunities").contentType(MediaType.APPLICATION_JSON).content(json(payload)),
                List.of()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void unknownReferenceIsRejected() throws Exception {
        final ContactEntity main = newContact("Max", "Muster");
        final Map<String, Object> payload = minimalPayload(UUID.randomUUID(), main.getId());
        mockMvc.perform(asUser(
                post("/api/opportunities").contentType(MediaType.APPLICATION_JSON).content(json(payload)),
                List.of()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void mainContactMustNotBeAdditionalContact() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final Map<String, Object> payload = minimalPayload(company.getId(), main.getId());
        payload.put("additionalContactIds", List.of(main.getId().toString()));
        mockMvc.perform(asUser(
                post("/api/opportunities").contentType(MediaType.APPLICATION_JSON).content(json(payload)),
                List.of()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void negativeEstimatedValueIsRejected() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final Map<String, Object> payload = minimalPayload(company.getId(), main.getId());
        payload.put("estimatedValue", "-1");
        mockMvc.perform(asUser(
                post("/api/opportunities").contentType(MediaType.APPLICATION_JSON).content(json(payload)),
                List.of()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void estimatedValueWithTooManyDecimalsIsRejected() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final Map<String, Object> payload = minimalPayload(company.getId(), main.getId());
        payload.put("estimatedValue", "100.999");
        mockMvc.perform(asUser(
                post("/api/opportunities").contentType(MediaType.APPLICATION_JSON).content(json(payload)),
                List.of()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void contactOfDifferentCompanyIsAccepted() throws Exception {
        final CompanyEntity companyA = newCompany("A");
        final CompanyEntity companyB = newCompany("B");
        final ContactEntity contactOfB = newContact("Max", "Muster", companyB);
        createOk(minimalPayload(companyA.getId(), contactOfB.getId()));
    }

    // -- Retrieval & list ----------------------------------------------------

    @Test
    void getByIdReturnsOpportunity() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final UUID id = createOpportunity("Deal", company.getId(), main.getId());
        mockMvc.perform(asUser(get("/api/opportunities/" + id), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.commentCount").value(0));
    }

    @Test
    void getUnknownReturns404() throws Exception {
        mockMvc.perform(asUser(get("/api/opportunities/" + UUID.randomUUID()), List.of()))
            .andExpect(status().isNotFound());
    }

    @Test
    void paginatedListReturnsStableEnvelope() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        for (int i = 0; i < 25; i++) {
            createOpportunity("Deal " + i, company.getId(), main.getId());
        }
        mockMvc.perform(asUser(get("/api/opportunities?page=0&size=20"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(20))
            .andExpect(jsonPath("$.page.totalElements").value(25));
    }

    @Test
    void listFiltersByStatusAndContactMainOrAdditional() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final ContactEntity additional = newContact("Erika", "Muster");

        // one OPEN with `additional` as additional contact
        final Map<String, Object> p1 = minimalPayload(company.getId(), main.getId());
        p1.put("title", "Open deal");
        p1.put("additionalContactIds", List.of(additional.getId().toString()));
        createOk(p1);
        // one WON without the additional contact
        final Map<String, Object> p2 = minimalPayload(company.getId(), main.getId());
        p2.put("title", "Won deal");
        p2.put("status", "WON");
        createOk(p2);

        mockMvc.perform(asUser(get("/api/opportunities?status=WON"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Won deal"));

        // contactId matches both main and additional; `additional` is only on p1
        mockMvc.perform(asUser(get("/api/opportunities?contactId=" + additional.getId()), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Open deal"));

        // main contact is on both
        mockMvc.perform(asUser(get("/api/opportunities?contactId=" + main.getId()), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void listFiltersBySearchStageCompanyOwnerAndTag() throws Exception {
        final CompanyEntity companyA = newCompany("Alpha AG");
        final CompanyEntity companyB = newCompany("Beta AG");
        final ContactEntity main = newContact("Max", "Muster");
        final UUID ownerA = newUser("owner-a", "Owner A");
        final UUID tag = newTag("Prio");

        final Map<String, Object> p1 = minimalPayload(companyA.getId(), main.getId());
        p1.put("title", "Muster deal");
        p1.put("stage", "Angebot");
        p1.put("ownerId", ownerA.toString());
        p1.put("tagIds", List.of(tag.toString()));
        createOk(p1);

        final Map<String, Object> p2 = minimalPayload(companyB.getId(), main.getId());
        p2.put("title", "Other deal");
        p2.put("stage", "Lead");
        createOk(p2);

        mockMvc.perform(asUser(get("/api/opportunities?search=muster"), List.of()))
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Muster deal"));
        mockMvc.perform(asUser(get("/api/opportunities?stage=Angebot"), List.of()))
            .andExpect(jsonPath("$.page.totalElements").value(1));
        mockMvc.perform(asUser(get("/api/opportunities?companyId=" + companyB.getId()), List.of()))
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Other deal"));
        mockMvc.perform(asUser(get("/api/opportunities?ownerId=" + ownerA), List.of()))
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Muster deal"));
        mockMvc.perform(asUser(get("/api/opportunities?tagIds=" + tag), List.of()))
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Muster deal"));
    }

    @Test
    void deletingTagDetachesItFromOpportunities() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final UUID tag = newTag("Temp");
        final Map<String, Object> payload = minimalPayload(company.getId(), main.getId());
        payload.put("tagIds", List.of(tag.toString()));
        final UUID id = UUID.fromString(createOk(payload).get("id").asText());

        mockMvc.perform(asUser(delete("/api/tags/" + tag), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isNoContent());

        mockMvc.perform(asUser(get("/api/opportunities/" + id), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tagIds.length()").value(0));
    }

    // -- Update --------------------------------------------------------------

    @Test
    void fullUpdateReplacesValues() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final ContactEntity newMain = newContact("Erika", "Muster");
        final UUID owner = newUser("owner-2", "Owner Two");
        final UUID id = createOpportunity("Old", company.getId(), main.getId());

        final Map<String, Object> payload = new HashMap<>();
        payload.put("title", "New title");
        payload.put("stage", "Verloren");
        payload.put("status", "LOST");
        payload.put("product", "Digital Trust");
        payload.put("estimatedValue", "9999.99");
        payload.put("companyId", company.getId().toString());
        payload.put("mainContactId", newMain.getId().toString());
        payload.put("ownerId", owner.toString());

        mockMvc.perform(asUser(
                put("/api/opportunities/" + id).contentType(MediaType.APPLICATION_JSON).content(json(payload)),
                List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("New title"))
            .andExpect(jsonPath("$.status").value("LOST"))
            .andExpect(jsonPath("$.mainContactId").value(newMain.getId().toString()))
            .andExpect(jsonPath("$.owner.id").value(owner.toString()));
    }

    @Test
    void updateWithMissingStatusIsRejected() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final UUID id = createOpportunity("Deal", company.getId(), main.getId());

        final Map<String, Object> payload = new HashMap<>();
        payload.put("title", "New");
        payload.put("companyId", company.getId().toString());
        payload.put("mainContactId", main.getId().toString());
        payload.put("ownerId", newUser("o3", "O3").toString());
        // status omitted -> @NotNull violation
        mockMvc.perform(asUser(
                put("/api/opportunities/" + id).contentType(MediaType.APPLICATION_JSON).content(json(payload)),
                List.of()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateUnknownReturns404() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final Map<String, Object> payload = new HashMap<>();
        payload.put("title", "New");
        payload.put("status", "OPEN");
        payload.put("companyId", company.getId().toString());
        payload.put("mainContactId", main.getId().toString());
        payload.put("ownerId", newUser("o4", "O4").toString());
        mockMvc.perform(asUser(
                put("/api/opportunities/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(json(payload)),
                List.of()))
            .andExpect(status().isNotFound());
    }

    // -- Deletion ------------------------------------------------------------

    @Test
    void adminDeletesOpportunityKeepsReferencedEntities() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final UUID id = createOpportunity("Deal", company.getId(), main.getId());

        mockMvc.perform(asUser(delete("/api/opportunities/" + id), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isNoContent());

        mockMvc.perform(asUser(get("/api/opportunities/" + id), List.of()))
            .andExpect(status().isNotFound());
        // company and contact still exist
        org.junit.jupiter.api.Assertions.assertTrue(companyRepository.findById(company.getId()).isPresent());
        org.junit.jupiter.api.Assertions.assertTrue(contactRepository.findById(main.getId()).isPresent());
    }

    @Test
    void nonAdminCannotDelete() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final UUID id = createOpportunity("Deal", company.getId(), main.getId());
        mockMvc.perform(asUser(delete("/api/opportunities/" + id), List.of()))
            .andExpect(status().isForbidden());
        mockMvc.perform(asUser(get("/api/opportunities/" + id), List.of()))
            .andExpect(status().isOk());
    }

    // -- Delete blocking on referenced entities ------------------------------

    @Test
    void companyDeletionIsBlockedByOpportunity() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        createOpportunity("Deal", company.getId(), main.getId());

        mockMvc.perform(asUser(delete("/api/companies/" + company.getId()), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isConflict());
        org.junit.jupiter.api.Assertions.assertTrue(companyRepository.findById(company.getId()).isPresent());
    }

    @Test
    void companyDeletionWithDeleteContactsIsBlockedByMainContactOfOtherCompany() throws Exception {
        final CompanyEntity companyToDelete = newCompany("ToDelete");
        final CompanyEntity otherCompany = newCompany("Other");
        // contact C belongs to companyToDelete but is main contact of an opportunity of otherCompany
        final ContactEntity contactC = newContact("Max", "Muster", companyToDelete);
        final ContactEntity otherMain = newContact("Erika", "Muster", otherCompany);
        createOpportunity("Other deal", otherCompany.getId(), contactC.getId());

        mockMvc.perform(asUser(delete("/api/companies/" + companyToDelete.getId() + "?deleteContacts=true"),
                List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isConflict());
        // nothing deleted
        org.junit.jupiter.api.Assertions.assertTrue(companyRepository.findById(companyToDelete.getId()).isPresent());
        org.junit.jupiter.api.Assertions.assertTrue(contactRepository.findById(contactC.getId()).isPresent());
        org.junit.jupiter.api.Assertions.assertTrue(contactRepository.findById(otherMain.getId()).isPresent());
    }

    @Test
    void companyWithoutOpportunitiesCanBeDeleted() throws Exception {
        final CompanyEntity company = newCompany("Free");
        mockMvc.perform(asUser(delete("/api/companies/" + company.getId()), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertTrue(companyRepository.findById(company.getId()).isEmpty());
    }

    @Test
    void mainContactDeletionIsBlocked() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        createOpportunity("Deal", company.getId(), main.getId());
        mockMvc.perform(asUser(delete("/api/contacts/" + main.getId()), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isConflict());
        org.junit.jupiter.api.Assertions.assertTrue(contactRepository.findById(main.getId()).isPresent());
    }

    @Test
    void additionalContactDeletionSilentlyUnlinks() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final ContactEntity additional = newContact("Erika", "Muster");
        final Map<String, Object> payload = minimalPayload(company.getId(), main.getId());
        payload.put("additionalContactIds", List.of(additional.getId().toString()));
        final UUID id = UUID.fromString(createOk(payload).get("id").asText());

        mockMvc.perform(asUser(delete("/api/contacts/" + additional.getId()), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertTrue(contactRepository.findById(additional.getId()).isEmpty());
        mockMvc.perform(asUser(get("/api/opportunities/" + id), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.additionalContactIds.length()").value(0));
    }

    // -- Comments ------------------------------------------------------------

    @Test
    void addListUpdateDeleteComment() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final UUID id = createOpportunity("Deal", company.getId(), main.getId());

        final String commentBody = mockMvc.perform(asUser(
                post("/api/opportunities/" + id + "/comments")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Called them today\"}"),
                List.of()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.author.name").value("Test User"))
            .andReturn().getResponse().getContentAsString();
        final UUID commentId = UUID.fromString(objectMapper.readTree(commentBody).get("id").asText());

        // commentCount increased
        mockMvc.perform(asUser(get("/api/opportunities/" + id), List.of()))
            .andExpect(jsonPath("$.commentCount").value(1));
        // an audit entry was written
        org.junit.jupiter.api.Assertions.assertEquals(1, auditCount("OpportunityComment", id, "INSERT"));

        mockMvc.perform(asUser(get("/api/opportunities/" + id + "/comments"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(asUser(
                put("/api/opportunities/" + id + "/comments/" + commentId)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Updated\"}"),
                List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").value("Updated"));

        mockMvc.perform(asUser(delete("/api/opportunities/" + id + "/comments/" + commentId),
                List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isNoContent());
        mockMvc.perform(asUser(get("/api/opportunities/" + id), List.of()))
            .andExpect(jsonPath("$.commentCount").value(0));
    }

    @Test
    void crossOwnerCommentAccessReturns404() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final UUID a = createOpportunity("A", company.getId(), main.getId());
        final UUID b = createOpportunity("B", company.getId(), main.getId());

        final String commentBody = mockMvc.perform(asUser(
                post("/api/opportunities/" + a + "/comments")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"hi\"}"),
                List.of()))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        final UUID commentId = UUID.fromString(objectMapper.readTree(commentBody).get("id").asText());

        mockMvc.perform(asUser(delete("/api/opportunities/" + b + "/comments/" + commentId),
                List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isNotFound());
    }

    @Test
    void blankCommentTextIsRejected() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final UUID id = createOpportunity("Deal", company.getId(), main.getId());
        mockMvc.perform(asUser(
                post("/api/opportunities/" + id + "/comments")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"   \"}"),
                List.of()))
            .andExpect(status().isBadRequest());
    }

    // -- Tags & user options -------------------------------------------------

    @Test
    void tagCountIncludesOpportunities() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final UUID tag = newTag("Shared");

        for (int i = 0; i < 2; i++) {
            final Map<String, Object> payload = minimalPayload(company.getId(), main.getId());
            payload.put("title", "Deal " + i);
            payload.put("tagIds", List.of(tag.toString()));
            createOk(payload);
        }

        mockMvc.perform(asUser(get("/api/tags/" + tag), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.opportunityCount").value(2));
    }

    @Test
    void anyUserCanListUserOptionsExcludingSystemUser() throws Exception {
        newUser("alice", "Alice");
        newUser("bob", "Bob");
        newUser("carol", "Carol");

        mockMvc.perform(asUser(get("/api/users/options"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    @Test
    void userOptionsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/options"))
            .andExpect(status().isUnauthorized());
    }

    // -- Audit & updates feed ------------------------------------------------

    @Test
    void opportunityMutationsAreAudited() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final UUID id = createOpportunity("Deal", company.getId(), main.getId());

        final Map<String, Object> payload = new HashMap<>();
        payload.put("title", "Deal v2");
        payload.put("status", "OPEN");
        payload.put("companyId", company.getId().toString());
        payload.put("mainContactId", main.getId().toString());
        payload.put("ownerId", newUser("audit-owner", "Audit Owner").toString());
        mockMvc.perform(asUser(
                put("/api/opportunities/" + id).contentType(MediaType.APPLICATION_JSON).content(json(payload)),
                List.of()))
            .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertEquals(1, auditCount("OpportunityDto", id, "INSERT"));
        org.junit.jupiter.api.Assertions.assertEquals(1, auditCount("OpportunityDto", id, "UPDATE"));
    }

    @Test
    void opportunityLifecycleAppearsInUpdatesFeed() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        createOpportunity("Feed deal", company.getId(), main.getId());

        mockMvc.perform(asUser(get("/api/updates?size=50"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.type == 'OPPORTUNITY_CREATED')].entityName").value(
                org.hamcrest.Matchers.hasItem("Feed deal")));
    }

    @Test
    void opportunityCommentActivityAppearsInUpdatesFeed() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity main = newContact("Max", "Muster");
        final UUID id = createOpportunity("Comment feed deal", company.getId(), main.getId());
        mockMvc.perform(asUser(
                post("/api/opportunities/" + id + "/comments")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"note\"}"),
                List.of()))
            .andExpect(status().isCreated());

        mockMvc.perform(asUser(get("/api/updates?size=50"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.type == 'OPPORTUNITY_COMMENT_CREATED')].entityName").value(
                org.hamcrest.Matchers.hasItem("Comment feed deal")));
    }

    private int auditCount(final String entityType, final UUID entityId, final String action) {
        final Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM audit_log WHERE entity_type = ? AND entity_id = ? AND action = ?",
            Integer.class, entityType, entityId, action);
        return count == null ? 0 : count;
    }
}
