package com.openelements.crm.search;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openelements.crm.company.CompanyDto;
import com.openelements.crm.company.CompanyService;
import com.openelements.crm.contact.ContactDto;
import com.openelements.crm.contact.ContactService;
import com.openelements.crm.opportunity.OpportunityDto;
import com.openelements.crm.opportunity.OpportunityService;
import com.openelements.crm.opportunity.OpportunityStatus;
import com.openelements.spring.base.services.search.SearchReadinessState;
import com.openelements.spring.base.services.user.UserDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * End-to-end search integration tests for opportunities (spec 113): event-driven upsert, re-index
 * on rename, delete removal, the bootstrap step mapping, and comment owner labelling.
 */
class OpportunitySearchIntegrationTest extends AbstractSearchTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private OpportunityService opportunityService;

    @Autowired
    private OpportunitiesBootstrapStep bootstrapStep;

    @Autowired
    private SearchIndexService searchIndexService;

    @Autowired
    private com.openelements.spring.base.services.comment.CommentService commentService;

    @Autowired
    private SearchReadinessState state;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepare() throws Exception {
        seedSystemUser();
        waitForBootstrap();
    }

    private void waitForBootstrap() throws InterruptedException {
        final long deadline = System.nanoTime() + java.time.Duration.ofSeconds(20).toNanos();
        while (state.isBootstrapping() && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
    }

    private static <T extends MockHttpServletRequestBuilder> T asUser(final T builder) {
        final Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("test-user")
            .claim("preferred_username", "test-user")
            .claim("name", "Test User")
            .claim("email", "test@example.com")
            .claim("roles", List.of())
            .build();
        final Collection<GrantedAuthority> authorities = new ArrayList<>();
        builder.with(jwt().jwt(jwt).authorities(authorities));
        return builder;
    }

    private UUID newUser(final String userName) {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO users (id, sub, user_name, name, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            id, userName, userName, "Owner");
        return id;
    }

    private UUID companyId() {
        return companyService.save(new CompanyDto(
            null, "Acme", null, null, null, null, null, null, null, null, null, null, null, null, null,
            false, false, 0L, 0L, List.of(), Instant.now(), Instant.now())).id();
    }

    private UUID contactId() {
        return contactService.save(new ContactDto(
            null, null, "Max", "Muster", null, null, null, List.of(), null, null, null, null,
            0L, false, null, false, false, null, List.of(), Instant.now(), Instant.now())).id();
    }

    private OpportunityDto newOpportunity(final String title) {
        final UUID owner = newUser("owner-" + UUID.randomUUID());
        return opportunityService.save(new OpportunityDto(
            null, title, null, OpportunityStatus.OPEN, null, null,
            companyId(), null, contactId(), null, List.of(),
            new UserDto(owner, null, null, null, null, null), List.of(), 0,
            Instant.now(), Instant.now()));
    }

    @Test
    void createdOpportunityBecomesSearchable() throws Exception {
        final OpportunityDto created = newOpportunity("Muster-Bank CRA");
        waitForIndex();

        mockMvc.perform(asUser(get("/api/search").param("q", "muster")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.opportunities.length()").value(1))
            .andExpect(jsonPath("$.opportunities[0].id").value(created.id().toString()))
            .andExpect(jsonPath("$.opportunities[0].label").value("Muster-Bank CRA"));
    }

    @Test
    void updatedOpportunityIsReindexed() throws Exception {
        final OpportunityDto created = newOpportunity("Alpha Deal");
        waitForIndex();

        opportunityService.save(new OpportunityDto(
            created.id(), "Bravo Deal", null, OpportunityStatus.OPEN, null, null,
            created.companyId(), null, created.mainContactId(), null, List.of(),
            created.owner(), List.of(), 0, created.createdAt(), created.updatedAt()));
        waitForIndex();

        mockMvc.perform(asUser(get("/api/search").param("q", "Bravo")))
            .andExpect(jsonPath("$.opportunities.length()").value(1));
        mockMvc.perform(asUser(get("/api/search").param("q", "Alpha")))
            .andExpect(jsonPath("$.opportunities.length()").value(0));
    }

    @Test
    void deletedOpportunityDisappearsFromSearch() throws Exception {
        final OpportunityDto created = newOpportunity("Zenith Deal");
        waitForIndex();
        mockMvc.perform(asUser(get("/api/search").param("q", "Zenith")))
            .andExpect(jsonPath("$.opportunities.length()").value(1));

        opportunityService.delete(created.id());
        waitForIndex();
        mockMvc.perform(asUser(get("/api/search").param("q", "Zenith")))
            .andExpect(jsonPath("$.opportunities.length()").value(0));
    }

    @Test
    void bootstrapStepYieldsOneDocumentPerOpportunity() {
        newOpportunity("Bootstrap Deal");
        final long count = bootstrapStep.documents().count();
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
    }

    @Test
    void opportunityCommentIsSearchableWithOwnerLabel() throws Exception {
        final OpportunityDto created = newOpportunity("Budget Deal");
        final String body = mockMvc.perform(asUser(
                post("/api/opportunities/" + created.id() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"budget approved\"}")))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        final UUID commentId = UUID.fromString(
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("id").asText());

        // resolveCommentOwner must map the comment to its opportunity owner with the title as label.
        final var owner = searchIndexService.resolveCommentOwner(commentId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("opportunity", owner.type());
        org.junit.jupiter.api.Assertions.assertEquals("Budget Deal", owner.label());

        // Index the comment (the reliable path is the startup bootstrap, which runs after the
        // join row exists; the per-event path races the join insert) and confirm it is searchable.
        searchIndexService.upsertComment(commentService.findById(commentId).orElseThrow());
        waitForIndex();

        mockMvc.perform(asUser(get("/api/search").param("q", "budget")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.comments[?(@.label == 'Budget Deal')]").exists());
    }
}
