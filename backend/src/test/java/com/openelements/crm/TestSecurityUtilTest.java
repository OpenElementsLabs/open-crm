package com.openelements.crm;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("TestSecurityUtil")
class TestSecurityUtilTest {

    @AfterEach
    void tearDown() {
        TestSecurityUtil.clearSecurityContext();
    }

    @Test
    @DisplayName("setSecurityContext() includes default CRM-ADMIN role")
    void defaultSecurityContextHasRoles() {
        TestSecurityUtil.setSecurityContext();
        final Jwt jwt = extractJwt();
        final List<String> roles = jwt.getClaimAsStringList("roles");
        assertNotNull(roles);
        assertEquals(List.of("CRM-ADMIN"), roles);
    }

    @Test
    @DisplayName("setSecurityContext(name, email) includes default CRM-ADMIN role")
    void namedSecurityContextHasRoles() {
        TestSecurityUtil.setSecurityContext("Alice", "alice@example.com");
        final Jwt jwt = extractJwt();
        assertEquals(List.of("CRM-ADMIN"), jwt.getClaimAsStringList("roles"));
    }

    @Test
    @DisplayName("setSecurityContext with custom roles uses provided roles")
    void customRolesInSecurityContext() {
        TestSecurityUtil.setSecurityContext("Bob", "bob@example.com", List.of("VIEWER", "EDITOR"));
        final Jwt jwt = extractJwt();
        assertEquals(List.of("VIEWER", "EDITOR"), jwt.getClaimAsStringList("roles"));
    }

    @Test
    @DisplayName("setSecurityContext with empty roles list produces empty roles claim")
    void emptyRolesInSecurityContext() {
        TestSecurityUtil.setSecurityContext("Bob", "bob@example.com", List.of());
        final Jwt jwt = extractJwt();
        assertEquals(List.of(), jwt.getClaimAsStringList("roles"));
    }

    private Jwt extractJwt() {
        final var auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return auth.getToken();
    }
}
