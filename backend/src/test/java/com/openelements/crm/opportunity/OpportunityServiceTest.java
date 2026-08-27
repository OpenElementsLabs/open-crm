package com.openelements.crm.opportunity;

import com.openelements.crm.AbstractDbTest;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.spring.base.services.comment.CommentCreateDto;
import com.openelements.spring.base.services.comment.CommentDto;
import com.openelements.spring.base.services.user.UserDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Service-layer tests for {@link OpportunityService} exercising the business logic directly (status
 * default, reference validation, tag counting, comment handling, and delete cleanup) independently
 * of the REST controller.
 */
class OpportunityServiceTest extends AbstractDbTest {

    @Autowired
    private OpportunityService opportunityService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seed() {
        seedSystemUser();
        // Some service operations (comment authoring) resolve the current user; provide a JWT auth
        // context so the user is auto-provisioned, mirroring an authenticated request.
        final Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("svc-user")
            .claim("preferred_username", "svc-user")
            .claim("name", "Svc User")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private CompanyEntity newCompany() {
        final CompanyEntity company = new CompanyEntity();
        company.setName("Acme");
        return companyRepository.saveAndFlush(company);
    }

    private ContactEntity newContact(final String first) {
        final ContactEntity contact = new ContactEntity();
        contact.setFirstName(first);
        contact.setLastName("Muster");
        return contactRepository.saveAndFlush(contact);
    }

    private UUID newUser() {
        final UUID id = UUID.randomUUID();
        final String sub = "owner-" + id;
        jdbcTemplate.update(
            "INSERT INTO oe_spring_services.users (id, sub, user_name, name, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            id, sub, sub, "Owner");
        return id;
    }

    private OpportunityDto dto(final UUID companyId, final UUID mainContactId, final UUID ownerId,
                              final OpportunityStatus status, final List<UUID> additionalContactIds,
                              final List<UUID> tagIds) {
        return new OpportunityDto(null, "Deal", null, status, null, null,
            companyId, null, mainContactId, null, additionalContactIds,
            new UserDto(ownerId, null, null, null, null, null), tagIds, 0, null, null);
    }

    @Test
    void saveDefaultsNullStatusToOpen() {
        final UUID id = opportunityService.save(
            dto(newCompany().getId(), newContact("Max").getId(), newUser(), null, List.of(), List.of())).id();
        assertEquals(OpportunityStatus.OPEN, opportunityService.findById(id).orElseThrow().status());
    }

    @Test
    void saveRejectsMainContactListedAsAdditionalContact() {
        final ContactEntity main = newContact("Max");
        final ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            opportunityService.save(dto(newCompany().getId(), main.getId(), newUser(),
                OpportunityStatus.OPEN, List.of(main.getId()), List.of())));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void saveRejectsUnknownCompanyReference() {
        final ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            opportunityService.save(dto(UUID.randomUUID(), newContact("Max").getId(), newUser(),
                OpportunityStatus.OPEN, List.of(), List.of())));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void countWithTagCountsOnlyMatchingOpportunities() {
        final UUID tagId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO oe_spring_services.tags (id, name, color, created_at, updated_at) "
                + "VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            tagId, "Shared", "#112233");
        final UUID company = newCompany().getId();
        final UUID contact = newContact("Max").getId();
        final UUID owner = newUser();
        opportunityService.save(dto(company, contact, owner, OpportunityStatus.OPEN, List.of(), List.of(tagId)));
        opportunityService.save(dto(company, contact, owner, OpportunityStatus.OPEN, List.of(), List.of()));

        assertEquals(1, opportunityService.countWithTag(tagId));
    }

    @Test
    void deleteRemovesTheOpportunityAndItsComments() {
        final UUID id = opportunityService.save(
            dto(newCompany().getId(), newContact("Max").getId(), newUser(),
                OpportunityStatus.OPEN, List.of(), List.of())).id();
        final CommentDto comment = opportunityService.addCommentToOpportunity(id, new CommentCreateDto("note"));

        opportunityService.delete(id);

        assertTrue(opportunityService.findById(id).isEmpty());
        // the comment join owner is gone; the standalone comment row was deleted too
        assertEquals(0, jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM oe_spring_services.comments WHERE id = ?", Integer.class, comment.id()));
    }

    @Test
    void updatingCommentOfWrongOpportunityIsNotFound() {
        final UUID a = opportunityService.save(
            dto(newCompany().getId(), newContact("A").getId(), newUser(),
                OpportunityStatus.OPEN, List.of(), List.of())).id();
        final UUID b = opportunityService.save(
            dto(newCompany().getId(), newContact("B").getId(), newUser(),
                OpportunityStatus.OPEN, List.of(), List.of())).id();
        final CommentDto comment = opportunityService.addCommentToOpportunity(a, new CommentCreateDto("hi"));

        final ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            opportunityService.updateCommentOfOpportunity(b, comment.id(), new CommentCreateDto("x")));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void existenceChecksReflectPersistedReferences() {
        final CompanyEntity company = newCompany();
        final ContactEntity main = newContact("Max");
        opportunityService.save(dto(company.getId(), main.getId(), newUser(),
            OpportunityStatus.OPEN, List.of(), List.of()));

        assertTrue(opportunityService.existsByCompany(company.getId()));
        assertTrue(opportunityService.existsByMainContact(main.getId()));
    }
}
