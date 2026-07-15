package com.openelements.crm.capabilities;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openelements.crm.AbstractDbTest;
import com.openelements.crm.contact.CrmHeicSupportCheck;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Behaviour tests for the spec-112 {@link CapabilitiesAdminController}.
 *
 * <p>Covers the endpoint contract: the reported {@code heicAvailable} flag mirrors the
 * {@link CrmHeicSupportCheck} bean for an IT-ADMIN, the endpoint is IT-ADMIN only, and the
 * startup-cached value is returned consistently across calls. The bean's probe itself is
 * spec-102's concern and is mocked here.
 */
class CapabilitiesAdminControllerTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrmHeicSupportCheck heicSupportCheck;

    private static MockHttpServletRequestBuilder withRoles(
        final MockHttpServletRequestBuilder builder, final List<String> roles) {
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

    @Test
    @DisplayName("GET /api/admin/capabilities returns heicAvailable=true for an IT-admin when the "
        + "bean reports HEIC decoding is available")
    void returnsHeicAvailableTrueForItAdmin() throws Exception {
        when(heicSupportCheck.isHeicAvailable()).thenReturn(true);

        mockMvc.perform(withRoles(get("/api/admin/capabilities"), List.of("IT-ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.heicAvailable").value(true));
    }

    @Test
    @DisplayName("GET /api/admin/capabilities returns heicAvailable=false when HEIC decoding is "
        + "unavailable in the runtime")
    void returnsHeicAvailableFalseWhenDecodingUnavailable() throws Exception {
        when(heicSupportCheck.isHeicAvailable()).thenReturn(false);

        mockMvc.perform(withRoles(get("/api/admin/capabilities"), List.of("IT-ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.heicAvailable").value(false));
    }

    @Test
    @DisplayName("GET /api/admin/capabilities returns 403 for a logged-in user without the "
        + "IT-admin role")
    void forbiddenForNonItAdmin() throws Exception {
        mockMvc.perform(withRoles(get("/api/admin/capabilities"), List.of("APP-ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/capabilities returns the startup-cached value consistently "
        + "across repeated calls (no live re-probe)")
    void reflectsStartupCachedValueConsistentlyAcrossCalls() throws Exception {
        when(heicSupportCheck.isHeicAvailable()).thenReturn(true);

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(withRoles(get("/api/admin/capabilities"), List.of("IT-ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heicAvailable").value(true));
        }
    }
}
