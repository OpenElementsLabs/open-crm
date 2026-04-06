package com.openelements.crm;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("JWT Role Mapping")
class SecurityConfigRoleTest {

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    private Jwt buildJwt(List<String> roles) {
        final var builder = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .claim("sub", "test-subject");
        if (roles != null) {
            builder.claim("roles", roles);
        }
        return builder.build();
    }

    private Set<String> authorityStrings(Collection<GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("Single role is mapped to ROLE_ authority")
    void singleRoleMapped() {
        final var auth = jwtAuthenticationConverter.convert(buildJwt(List.of("CRM-ADMIN")));
        final Set<String> authorities = authorityStrings(auth.getAuthorities());
        assertTrue(authorities.contains("ROLE_CRM-ADMIN"));
    }

    @Test
    @DisplayName("Multiple roles are mapped to multiple authorities")
    void multipleRolesMapped() {
        final var auth = jwtAuthenticationConverter.convert(buildJwt(List.of("CRM-ADMIN", "CRM-READONLY")));
        final Set<String> authorities = authorityStrings(auth.getAuthorities());
        assertTrue(authorities.contains("ROLE_CRM-ADMIN"));
        assertTrue(authorities.contains("ROLE_CRM-READONLY"));
    }

    @Test
    @DisplayName("Missing roles claim results in no ROLE_ authorities")
    void missingRolesClaimNoAuthorities() {
        final var auth = jwtAuthenticationConverter.convert(buildJwt(null));
        final Set<String> authorities = authorityStrings(auth.getAuthorities());
        assertFalse(authorities.stream().anyMatch(a -> a.startsWith("ROLE_")));
    }

    @Test
    @DisplayName("Empty roles array results in no ROLE_ authorities")
    void emptyRolesNoAuthorities() {
        final var auth = jwtAuthenticationConverter.convert(buildJwt(List.of()));
        final Set<String> authorities = authorityStrings(auth.getAuthorities());
        assertFalse(authorities.stream().anyMatch(a -> a.startsWith("ROLE_")));
    }

    @Test
    @DisplayName("Scope-based authorities are preserved alongside role authorities")
    void scopeAuthoritiesPreserved() {
        final Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .claim("sub", "test-subject")
                .claim("scope", "openid profile")
                .claim("roles", List.of("CRM-ADMIN"))
                .build();
        final var auth = jwtAuthenticationConverter.convert(jwt);
        final Set<String> authorities = authorityStrings(auth.getAuthorities());
        assertTrue(authorities.contains("SCOPE_openid"));
        assertTrue(authorities.contains("SCOPE_profile"));
        assertTrue(authorities.contains("ROLE_CRM-ADMIN"));
    }
}
