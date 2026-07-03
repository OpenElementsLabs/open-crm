package com.openelements.crm.enrich;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openelements.crm.AbstractDbTest;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.contact.SocialLinkEntity;
import com.openelements.crm.contact.SocialNetworkType;
import com.openelements.spring.base.events.OnObjectUpdate;
import com.openelements.spring.base.security.roles.Roles;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Integration tests for the enrichment search/apply actions. The concrete external clients are
 * mocked so the tests exercise the real matching-input selection, fill-empty preview, company
 * resolution, apply semantics, event publishing, and authorization against a real database.
 */
@RecordApplicationEvents
class EnrichmentActionIntegrationTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ContactRepository contactRepository;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private ApplicationEvents events;

    @MockitoBean
    private GravatarClient gravatarClient;
    @MockitoBean
    private DropcontactClient dropcontactClient;
    @MockitoBean
    private CognismClient cognismClient;

    @BeforeEach
    void seed() {
        seedSystemUser();
    }

    // --- Permissions & visibility ---

    @Test
    void nonAdminIsRejected() throws Exception {
        final ContactEntity contact = newContact("Max", "Müller", "max@oe.com");
        mockMvc.perform(asRoles(post(search("gravatar", contact)), List.of()))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanSearch() throws Exception {
        final ContactEntity contact = newContact("Max", "Müller", "max@oe.com");
        Mockito.when(gravatarClient.lookupByEmail(Mockito.anyString()))
            .thenReturn(Optional.of(candidate("h", "Max", payload(null, "CTO", null, Map.of(), null, null, null))));

        mockMvc.perform(asRoles(post(search("gravatar", contact)), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("MATCH"));
    }

    // --- Matching input selection ---

    @Test
    void gravatarSkippedWithoutEmail() throws Exception {
        final ContactEntity contact = newContact("Max", "Müller", null);
        mockMvc.perform(asRoles(post(search("gravatar", contact)), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NO_MATCH"));
        Mockito.verifyNoInteractions(gravatarClient);
    }

    @Test
    void dropcontactFallsBackToNameAndCompanyWithoutEmail() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final ContactEntity contact = newContact("Jane", "Doe", null);
        contact.setCompany(company);
        contactRepository.saveAndFlush(contact);
        Mockito.when(dropcontactClient.search(Mockito.isNull(), Mockito.eq("Jane"), Mockito.eq("Doe"), Mockito.eq("Acme")))
            .thenReturn(List.of(candidate("d", "Jane @ Acme", payload(null, "Dev", null, Map.of(), null, null, null))));

        mockMvc.perform(asRoles(post(search("dropcontact", contact)), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("MATCH"));
        Mockito.verify(dropcontactClient).search(Mockito.isNull(), Mockito.eq("Jane"), Mockito.eq("Doe"), Mockito.eq("Acme"));
    }

    // --- Candidate outcomes ---

    @Test
    void singleCandidateReturnsPreviewDirectly() throws Exception {
        final ContactEntity contact = newContact("Max", "Müller", "max@oe.com");
        Mockito.when(gravatarClient.lookupByEmail(Mockito.anyString()))
            .thenReturn(Optional.of(candidate("h", "Max", payload(null, "CTO", null, Map.of(), null, null, null))));

        mockMvc.perform(asRoles(post(search("gravatar", contact)), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.candidates", hasSize(1)))
            .andExpect(jsonPath("$.candidates[0].changes[*].field", hasItem("position")));
    }

    @Test
    void multipleCandidatesAreReturnedForSelection() throws Exception {
        final ContactEntity contact = newContact("Max", "Müller", null);
        Mockito.when(cognismClient.search(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(List.of(
                candidate("c0", "Max @ Acme", payload(null, "CTO", null, Map.of(), "Acme", null, null)),
                candidate("c1", "Max @ Beta", payload(null, "CEO", null, Map.of(), "Beta", null, null))));

        mockMvc.perform(asRoles(post(search("cognism", contact)), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.candidates", hasSize(2)))
            .andExpect(jsonPath("$.candidates[0].label").value("Max @ Acme"));
    }

    @Test
    void zeroCandidatesIsNoMatch() throws Exception {
        final ContactEntity contact = newContact("Max", "Müller", "max@oe.com");
        Mockito.when(gravatarClient.lookupByEmail(Mockito.anyString())).thenReturn(Optional.empty());

        mockMvc.perform(asRoles(post(search("gravatar", contact)), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NO_MATCH"))
            .andExpect(jsonPath("$.candidates", hasSize(0)));
    }

    // --- Fill-empty rule ---

    @Test
    void onlyEmptyFieldsAreProposed() throws Exception {
        final ContactEntity contact = newContact("Max", "Müller", "max@oe.com");
        contact.setPosition("CTO");
        contactRepository.saveAndFlush(contact);
        Mockito.when(gravatarClient.lookupByEmail(Mockito.anyString()))
            .thenReturn(Optional.of(candidate("h", "Max",
                payload(null, "Chief Technology Officer", "+49 123", Map.of(), null, null, null))));

        mockMvc.perform(asRoles(post(search("gravatar", contact)), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.candidates[0].changes[*].field", hasItem("phoneNumber")))
            .andExpect(jsonPath("$.candidates[0].changes[*].field", not(hasItem("position"))));
    }

    @Test
    void socialLinksAreEvaluatedPerNetwork() throws Exception {
        final ContactEntity contact = newContact("Max", "Müller", "max@oe.com");
        contact.getSocialLinks().add(socialLink(SocialNetworkType.GITHUB, "maxm"));
        contactRepository.saveAndFlush(contact);
        Mockito.when(gravatarClient.lookupByEmail(Mockito.anyString()))
            .thenReturn(Optional.of(candidate("h", "Max", payload(null, null, null,
                Map.of("GITHUB", "https://github.com/other", "LINKEDIN", "https://linkedin.com/in/maxm"),
                null, null, null))));

        mockMvc.perform(asRoles(post(search("gravatar", contact)), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.candidates[0].changes[*].field", hasItem("socialLinks.LINKEDIN")))
            .andExpect(jsonPath("$.candidates[0].changes[*].field", not(hasItem("socialLinks.GITHUB"))));
    }

    @Test
    void photoProposedOnlyWhenContactHasNone() throws Exception {
        final ContactEntity contact = newContact("Max", "Müller", "max@oe.com");
        contact.setPhoto("existing".getBytes());
        contact.setPhotoContentType("image/jpeg");
        contactRepository.saveAndFlush(contact);
        Mockito.when(gravatarClient.lookupByEmail(Mockito.anyString()))
            .thenReturn(Optional.of(candidate("h", "Max", payload(null, "CTO", null, Map.of(), null, "abc", "image/png"))));

        mockMvc.perform(asRoles(post(search("gravatar", contact)), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.candidates[0].changes[*].field", not(hasItem("photo"))));
    }

    @Test
    void nothingToEnrichWhenAllFilled() throws Exception {
        final ContactEntity contact = newContact("Max", "Müller", "max@oe.com");
        contact.setPosition("CTO");
        contactRepository.saveAndFlush(contact);
        Mockito.when(gravatarClient.lookupByEmail(Mockito.anyString()))
            .thenReturn(Optional.of(candidate("h", "Max", payload(null, "CTO", null, Map.of(), null, null, null))));

        mockMvc.perform(asRoles(post(search("gravatar", contact)), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.candidates[0].nothingToEnrich").value(true));
    }

    // --- Company resolution ---

    @Test
    void existingCompanyResolvesToMatched() throws Exception {
        newCompany("Open Elements GmbH");
        final ContactEntity contact = newContact("Max", "Müller", "max@oe.com");
        Mockito.when(gravatarClient.lookupByEmail(Mockito.anyString()))
            .thenReturn(Optional.of(candidate("h", "Max", payload(null, null, null, Map.of(), "open elements gmbh", null, null))));

        mockMvc.perform(asRoles(post(search("gravatar", contact)), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.candidates[0].companyResolution.kind").value("MATCHED"));
    }

    @Test
    void unknownCompanyResolvesToNew() throws Exception {
        final ContactEntity contact = newContact("Max", "Müller", "max@oe.com");
        Mockito.when(gravatarClient.lookupByEmail(Mockito.anyString()))
            .thenReturn(Optional.of(candidate("h", "Max", payload(null, null, null, Map.of(), "Brand New Co", null, null))));

        mockMvc.perform(asRoles(post(search("gravatar", contact)), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.candidates[0].companyResolution.kind").value("NEW"));
    }

    @Test
    void noCompanyDataResolvesToNone() throws Exception {
        final ContactEntity contact = newContact("Max", "Müller", "max@oe.com");
        Mockito.when(gravatarClient.lookupByEmail(Mockito.anyString()))
            .thenReturn(Optional.of(candidate("h", "Max", payload(null, "CTO", null, Map.of(), null, null, null))));

        mockMvc.perform(asRoles(post(search("gravatar", contact)), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.candidates[0].companyResolution.kind").value("NONE"));
    }

    // --- Apply ---

    @Test
    void applyWritesAllProposedFieldsAndPublishesEvent() throws Exception {
        final ContactEntity contact = newContact("Max", "Müller", "max@oe.com");
        final EnrichmentApplyDto request = new EnrichmentApplyDto(
            payload(null, "CTO", "+49 123", Map.of("LINKEDIN", "https://linkedin.com/in/maxm"), null, null, null), false);

        mockMvc.perform(asRoles(post(apply("gravatar", contact))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)),
                List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contact.position").value("CTO"))
            .andExpect(jsonPath("$.contact.phoneNumber").value("+49 123"))
            .andExpect(jsonPath("$.contact.socialLinks", hasSize(1)))
            .andExpect(jsonPath("$.gdprNotice").exists());

        assertEquals("CTO", contactRepository.findByIdOrThrow(contact.getId()).getPosition());
        assertTrue(events.stream(OnObjectUpdate.class).findAny().isPresent(),
            "apply must publish an OnObjectUpdate event");
    }

    @Test
    void applyReEnforcesFillEmptyAgainstCurrentState() throws Exception {
        final ContactEntity contact = newContact("Max", "Müller", "max@oe.com");
        contact.setPosition("CTO");
        contactRepository.saveAndFlush(contact);
        final EnrichmentApplyDto request = new EnrichmentApplyDto(
            payload(null, "Overwritten", null, Map.of(), null, null, null), false);

        mockMvc.perform(asRoles(post(apply("gravatar", contact))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)),
                List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contact.position").value("CTO"));

        assertEquals("CTO", contactRepository.findByIdOrThrow(contact.getId()).getPosition());
    }

    @Test
    void newCompanyCreatedOnlyWhenCheckboxChecked() throws Exception {
        final ContactEntity contactA = newContact("A", "A", "a@oe.com");
        mockMvc.perform(asRoles(post(apply("gravatar", contactA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EnrichmentApplyDto(
                    payload(null, null, null, Map.of(), "Fresh Company", null, null), false))),
                List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk());
        assertNull(contactRepository.findByIdOrThrow(contactA.getId()).getCompany(),
            "no company must be created when the checkbox is unchecked");
        assertTrue(companyRepository.findByNameIgnoreCase("Fresh Company").isEmpty());

        final ContactEntity contactB = newContact("B", "B", "b@oe.com");
        mockMvc.perform(asRoles(post(apply("gravatar", contactB))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EnrichmentApplyDto(
                    payload(null, null, null, Map.of(), "Fresh Company", null, null), true))),
                List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk());
        assertNotNull(contactRepository.findByIdOrThrow(contactB.getId()).getCompany());
        assertTrue(companyRepository.findByNameIgnoreCase("Fresh Company").isPresent());
    }

    @Test
    void applyTranscodesAndStoresPhotoWhenContactHasNone() throws Exception {
        final ContactEntity contact = newContact("Max", "Müller", "max@oe.com");
        final EnrichmentApplyDto request = new EnrichmentApplyDto(
            payload(null, null, null, Map.of(), null, tinyPngBase64(), "image/png"), false);

        mockMvc.perform(asRoles(post(apply("gravatar", contact))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)),
                List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contact.hasPhoto").value(true));

        assertNotNull(contactRepository.findByIdOrThrow(contact.getId()).getPhoto());
    }

    // --- Error handling ---

    @Test
    void downstreamFailureReturnsGenericError() throws Exception {
        final ContactEntity contact = newContact("Max", "Müller", "max@oe.com");
        Mockito.when(dropcontactClient.search(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_GATEWAY, "Enrichment currently unavailable"));

        mockMvc.perform(asRoles(post(search("dropcontact", contact)), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isBadGateway());
    }

    // --- helpers ---

    private static String search(final String service, final ContactEntity contact) {
        return "/api/contacts/" + contact.getId() + "/enrich/" + service + "/search";
    }

    private static String apply(final String service, final ContactEntity contact) {
        return "/api/contacts/" + contact.getId() + "/enrich/" + service + "/apply";
    }

    private static RawCandidate candidate(final String id, final String label, final EnrichmentPayloadDto payload) {
        return new RawCandidate(id, label, payload);
    }

    private static EnrichmentPayloadDto payload(final String email, final String position, final String phone,
                                                final Map<String, String> social, final String company,
                                                final String photoBase64, final String photoContentType) {
        return new EnrichmentPayloadDto(email, position, phone, social, company, photoBase64, photoContentType);
    }

    private ContactEntity newContact(final String firstName, final String lastName, final String email) {
        final ContactEntity c = new ContactEntity();
        c.setFirstName(firstName);
        c.setLastName(lastName);
        c.setEmail(email);
        return contactRepository.saveAndFlush(c);
    }

    private CompanyEntity newCompany(final String name) {
        final CompanyEntity c = new CompanyEntity();
        c.setName(name);
        return companyRepository.saveAndFlush(c);
    }

    private static SocialLinkEntity socialLink(final SocialNetworkType type, final String value) {
        final SocialNetworkType.ResolvedLink resolved = type.resolve(value);
        final SocialLinkEntity link = new SocialLinkEntity();
        link.setNetworkType(type);
        link.setValue(resolved.value());
        link.setUrl(resolved.url());
        return link;
    }

    private static String tinyPngBase64() throws Exception {
        final BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static MockHttpServletRequestBuilder asRoles(final MockHttpServletRequestBuilder builder,
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
}
