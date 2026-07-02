package com.openelements.crm.enrich;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openelements.crm.AbstractDbTest;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.spring.base.security.roles.Roles;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Integration tests for the Dropcontact/Cognism settings endpoints, using real clients whose base
 * URLs point at a mock server so key validation is exercised end to end.
 */
class EnrichmentSettingsIntegrationTest extends AbstractDbTest {

    private static final MockWebServer SERVER = new MockWebServer();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ContactRepository contactRepository;

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) throws IOException {
        SERVER.start();
        final String base = SERVER.url("/").toString().replaceAll("/$", "");
        registry.add("enrichment.dropcontact.base-url", () -> base);
        registry.add("enrichment.cognism.base-url", () -> base);
        registry.add("enrichment.dropcontact.poll-interval-ms", () -> "1");
    }

    @AfterAll
    static void stopServer() throws IOException {
        SERVER.shutdown();
    }

    @Test
    void itAdminStoresValidKeyAndStatusReflectsIt() throws Exception {
        SERVER.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        mockMvc.perform(asRoles(put("/api/dropcontact/settings")
                .contentType(MediaType.APPLICATION_JSON).content("{\"apiKey\":\"secret\"}"),
                List.of(Roles.ROLE_IT_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(true))
            .andExpect(jsonPath("$.apiKey").doesNotExist());

        mockMvc.perform(asRoles(get("/api/dropcontact/settings"), List.of(Roles.ROLE_IT_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(true));
    }

    @Test
    void invalidKeyIsRejected() throws Exception {
        SERVER.enqueue(new MockResponse().setResponseCode(401));

        mockMvc.perform(asRoles(put("/api/dropcontact/settings")
                .contentType(MediaType.APPLICATION_JSON).content("{\"apiKey\":\"bad\"}"),
                List.of(Roles.ROLE_IT_ADMIN)))
            .andExpect(status().isBadRequest());

        mockMvc.perform(asRoles(get("/api/dropcontact/settings"), List.of(Roles.ROLE_IT_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(false));
    }

    @Test
    void nonItAdminCannotManageKeys() throws Exception {
        mockMvc.perform(asRoles(put("/api/dropcontact/settings")
                .contentType(MediaType.APPLICATION_JSON).content("{\"apiKey\":\"secret\"}"),
                List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isForbidden());

        mockMvc.perform(asRoles(delete("/api/dropcontact/settings"), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isForbidden());
    }

    @Test
    void settingsStatusIsReadableByAppAdminForMenuGating() throws Exception {
        mockMvc.perform(asRoles(get("/api/cognism/settings"), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(false));
    }

    @Test
    void removingKeyDisablesService() throws Exception {
        SERVER.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        mockMvc.perform(asRoles(put("/api/dropcontact/settings")
                .contentType(MediaType.APPLICATION_JSON).content("{\"apiKey\":\"secret\"}"),
                List.of(Roles.ROLE_IT_ADMIN)))
            .andExpect(status().isOk());

        mockMvc.perform(asRoles(delete("/api/dropcontact/settings"), List.of(Roles.ROLE_IT_ADMIN)))
            .andExpect(status().isNoContent());

        mockMvc.perform(asRoles(get("/api/dropcontact/settings"), List.of(Roles.ROLE_IT_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(false));
    }

    @Test
    void searchOnUnconfiguredServiceReturns503() throws Exception {
        final ContactEntity contact = new ContactEntity();
        contact.setFirstName("Max");
        contact.setLastName("Müller");
        contact.setEmail("max@oe.com");
        contactRepository.saveAndFlush(contact);

        mockMvc.perform(asRoles(post("/api/contacts/" + contact.getId() + "/enrich/dropcontact/search"),
                List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isServiceUnavailable());
    }

    private static MockHttpServletRequestBuilder asRoles(final MockHttpServletRequestBuilder builder,
                                                         final List<String> roles) {
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
}
