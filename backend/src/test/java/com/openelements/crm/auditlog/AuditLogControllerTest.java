package com.openelements.crm.auditlog;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openelements.spring.base.services.audit.AuditAction;
import com.openelements.spring.base.services.audit.AuditLogDataService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Behaviour tests for the GET /api/audit-logs and /entity-types endpoints
 * added in spec 090.
 *
 * <p>Role-based 401/403/200 coverage lives in
 * {@link com.openelements.crm.security.SecurityRoleIntegrationTest}; this class
 * focuses on the API contract — pagination, filter combinations, and the
 * entity-types listing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogDataService auditLogDataService;

    @Autowired
    private CrmAuditLogRepository auditLogRepository;

    private static MockHttpServletRequestBuilder asItAdmin(MockHttpServletRequestBuilder builder) {
        final List<String> roles = List.of("IT-ADMIN");
        final Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("test-user")
            .claim("preferred_username", "test-user")
            .claim("email", "test@example.com")
            .claim("roles", roles)
            .build();
        final Collection<GrantedAuthority> authorities = new ArrayList<>();
        for (final String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return builder.with(jwt().jwt(jwt).authorities(authorities));
    }

    @BeforeEach
    void cleanAuditLog() {
        auditLogRepository.deleteAll();
    }

    @Test
    void listAuditLogsReturnsPagedResponseShape() throws Exception {
        mockMvc.perform(asItAdmin(get("/api/audit-logs")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page.size").exists())
            .andExpect(jsonPath("$.page.number").exists())
            .andExpect(jsonPath("$.page.totalElements").exists())
            .andExpect(jsonPath("$.page.totalPages").exists());
    }

    @Test
    void listAuditLogsUsesDefaultPageSize20WithoutParams() throws Exception {
        mockMvc.perform(asItAdmin(get("/api/audit-logs")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size").value(20))
            .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    void listAuditLogsHonoursExplicitPageAndSize() throws Exception {
        mockMvc.perform(asItAdmin(get("/api/audit-logs")
                .param("page", "0").param("size", "10")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size").value(10))
            .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    void listAuditLogsFiltersByEntityType() throws Exception {
        auditLogDataService.createEntry("CompanyDto", UUID.randomUUID(), AuditAction.INSERT, "Alice");
        auditLogDataService.createEntry("ContactDto", UUID.randomUUID(), AuditAction.INSERT, "Alice");
        auditLogDataService.createEntry("CompanyDto", UUID.randomUUID(), AuditAction.UPDATE, "Bob");

        mockMvc.perform(asItAdmin(get("/api/audit-logs").param("entityType", "CompanyDto")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].entityType").value("CompanyDto"))
            .andExpect(jsonPath("$.content[1].entityType").value("CompanyDto"));
    }

    @Test
    void listAuditLogsFiltersByUser() throws Exception {
        auditLogDataService.createEntry("CompanyDto", UUID.randomUUID(), AuditAction.INSERT, "Alice");
        auditLogDataService.createEntry("ContactDto", UUID.randomUUID(), AuditAction.INSERT, "Alice");
        auditLogDataService.createEntry("CompanyDto", UUID.randomUUID(), AuditAction.UPDATE, "Bob");

        mockMvc.perform(asItAdmin(get("/api/audit-logs").param("user", "Alice")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].user").value("Alice"))
            .andExpect(jsonPath("$.content[1].user").value("Alice"));
    }

    @Test
    void listAuditLogsFiltersByEntityTypeAndUser() throws Exception {
        auditLogDataService.createEntry("CompanyDto", UUID.randomUUID(), AuditAction.INSERT, "Alice");
        auditLogDataService.createEntry("ContactDto", UUID.randomUUID(), AuditAction.INSERT, "Alice");
        auditLogDataService.createEntry("CompanyDto", UUID.randomUUID(), AuditAction.UPDATE, "Bob");

        mockMvc.perform(asItAdmin(get("/api/audit-logs")
                .param("entityType", "CompanyDto")
                .param("user", "Alice")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].entityType").value("CompanyDto"))
            .andExpect(jsonPath("$.content[0].user").value("Alice"));
    }

    @Test
    void listAuditLogsSortsByCreatedAtDescending() throws Exception {
        auditLogDataService.createEntry("CompanyDto", UUID.randomUUID(), AuditAction.INSERT, "Alice");
        Thread.sleep(5L);
        auditLogDataService.createEntry("CompanyDto", UUID.randomUUID(), AuditAction.UPDATE, "Bob");
        Thread.sleep(5L);
        final UUID newestId = auditLogDataService
            .createEntry("CompanyDto", UUID.randomUUID(), AuditAction.DELETE, "Charlie")
            .id();

        mockMvc.perform(asItAdmin(get("/api/audit-logs")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(newestId.toString()));
    }

    @Test
    void entityTypesReturnsDistinctSortedValues() throws Exception {
        auditLogDataService.createEntry("ContactDto", UUID.randomUUID(), AuditAction.INSERT, "Alice");
        auditLogDataService.createEntry("CompanyDto", UUID.randomUUID(), AuditAction.INSERT, "Alice");
        auditLogDataService.createEntry("ContactDto", UUID.randomUUID(), AuditAction.UPDATE, "Bob");

        mockMvc.perform(asItAdmin(get("/api/audit-logs/entity-types")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0]").value("CompanyDto"))
            .andExpect(jsonPath("$[1]").value("ContactDto"));
    }

    @Test
    void entityTypesReturnsEmptyArrayWhenNoEntries() throws Exception {
        mockMvc.perform(asItAdmin(get("/api/audit-logs/entity-types")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }
}
