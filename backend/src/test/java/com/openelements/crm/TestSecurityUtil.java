package com.openelements.crm;

import java.util.List;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class TestSecurityUtil {

    public static final String TEST_USER_NAME = "Test User";
    public static final String TEST_USER_EMAIL = "test@example.com";
    public static final List<String> DEFAULT_ROLES = List.of("CRM-ADMIN");

    private TestSecurityUtil() {
    }

    public static RequestPostProcessor testJwt() {
        return testJwt(TEST_USER_NAME, TEST_USER_EMAIL, DEFAULT_ROLES);
    }

    public static RequestPostProcessor testJwt(final String name, final String email) {
        return testJwt(name, email, DEFAULT_ROLES);
    }

    public static RequestPostProcessor testJwt(final String name, final String email, final List<String> roles) {
        return jwt().jwt(builder -> builder
                .claim("name", name)
                .claim("email", email)
                .claim("roles", roles));
    }

    public static void setSecurityContext() {
        setSecurityContext(TEST_USER_NAME, TEST_USER_EMAIL, DEFAULT_ROLES);
    }

    public static void setSecurityContext(final String name, final String email) {
        setSecurityContext(name, email, DEFAULT_ROLES);
    }

    public static void setSecurityContext(final String name, final String email, final List<String> roles) {
        final var builder = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256");
        if (name != null) {
            builder.claim("name", name);
        }
        if (email != null) {
            builder.claim("email", email);
        }
        builder.claim("roles", roles != null ? roles : List.of());
        // Ensure at least one claim exists (sub is standard)
        builder.claim("sub", "test-subject");
        final Jwt jwt = builder.build();
        final SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(jwt));
        SecurityContextHolder.setContext(context);
    }

    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
