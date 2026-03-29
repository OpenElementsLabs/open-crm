package com.openelements.crm;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class TestSecurityUtil {

    public static final String TEST_USER_NAME = "Test User";
    public static final String TEST_USER_EMAIL = "test@example.com";

    private TestSecurityUtil() {
    }

    public static RequestPostProcessor testJwt() {
        return jwt().jwt(builder -> builder
                .claim("name", TEST_USER_NAME)
                .claim("email", TEST_USER_EMAIL));
    }

    public static RequestPostProcessor testJwt(final String name, final String email) {
        return jwt().jwt(builder -> builder
                .claim("name", name)
                .claim("email", email));
    }

    public static void setSecurityContext() {
        setSecurityContext(TEST_USER_NAME, TEST_USER_EMAIL);
    }

    public static void setSecurityContext(final String name, final String email) {
        final var builder = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256");
        if (name != null) {
            builder.claim("name", name);
        }
        if (email != null) {
            builder.claim("email", email);
        }
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
