package com.openelements.crm.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openelements.crm.AbstractDbTest;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Integration tests for GET /api/contacts/export/vcard and
 * GET /api/contacts/{id}/vcard covering field mapping, embedded photos,
 * multi-card export, and the 404 contract for a missing contact.
 */
class ContactVCardExportIntegrationTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private static <T extends MockHttpServletRequestBuilder> T asUser(final T builder) {
        final Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("test-user")
            .claim("preferred_username", "test-user")
            .claim("name", "Test User")
            .claim("email", "test@example.com")
            .claim("roles", List.of())
            .build();
        builder.with(jwt().jwt(jwt));
        return builder;
    }

    private ContactEntity newContact(final String first, final String last) {
        final ContactEntity contact = new ContactEntity();
        contact.setFirstName(first);
        contact.setLastName(last);
        return contactRepository.saveAndFlush(contact);
    }

    private static byte[] solidJpeg(final Color color) throws IOException {
        final BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 16, 16);
        g.dispose();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpeg", out);
        return out.toByteArray();
    }

    @Test
    void singleContactExportMapsCoreFields() throws Exception {
        final CompanyEntity company = new CompanyEntity();
        company.setName("Open Elements");
        final CompanyEntity savedCompany = companyRepository.saveAndFlush(company);

        final ContactEntity contact = new ContactEntity();
        contact.setTitle("Dr.");
        contact.setFirstName("Jane");
        contact.setLastName("Doe");
        contact.setEmail("jane.doe@example.com");
        contact.setPosition("CTO");
        contact.setPhoneNumber("+49 123 456");
        contact.setBirthday(LocalDate.of(1985, 7, 14));
        contact.setDescription("Key contact");
        contact.setCompany(savedCompany);
        final SocialLinkEntity link = new SocialLinkEntity();
        link.setNetworkType(SocialNetworkType.GITHUB);
        link.setValue("janedoe");
        link.setUrl("https://github.com/janedoe");
        contact.getSocialLinks().add(link);
        final ContactEntity saved = contactRepository.saveAndFlush(contact);

        final String body = mockMvc.perform(asUser(get("/api/contacts/" + saved.getId() + "/vcard")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",
                "attachment; filename=\"contact-" + saved.getId() + ".vcf\""))
            .andReturn().getResponse().getContentAsString();

        final VCard card = Ezvcard.parse(body).first();
        assertNotNull(card);
        assertEquals("Doe", card.getStructuredName().getFamily());
        assertEquals("Jane", card.getStructuredName().getGiven());
        assertTrue(card.getStructuredName().getPrefixes().contains("Dr."));
        assertEquals("Dr. Jane Doe", card.getFormattedName().getValue());
        assertEquals("CTO", card.getTitles().get(0).getValue());
        assertEquals("Open Elements", card.getOrganization().getValues().get(0));
        assertEquals("jane.doe@example.com", card.getEmails().get(0).getValue());
        assertEquals("+49 123 456", card.getTelephoneNumbers().get(0).getText());
        assertEquals("https://github.com/janedoe", card.getUrls().get(0).getValue());
        assertEquals("Key contact", card.getNotes().get(0).getValue());
        assertEquals("urn:uuid:" + saved.getId(), card.getUid().getValue());
        assertNotNull(card.getBirthday());
    }

    @Test
    void singleContactExportEmbedsPhoto() throws Exception {
        final ContactEntity contact = newContact("Foto", "Person");
        contact.setPhoto(solidJpeg(Color.BLUE));
        contact.setPhotoContentType("image/jpeg");
        final ContactEntity saved = contactRepository.saveAndFlush(contact);

        final String body = mockMvc.perform(asUser(get("/api/contacts/" + saved.getId() + "/vcard")))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        final VCard card = Ezvcard.parse(body).first();
        assertEquals(1, card.getPhotos().size(), "vCard must carry the embedded photo");
        assertNotNull(card.getPhotos().get(0).getData(), "Photo must be embedded as binary data");
    }

    @Test
    void exportAllReturnsOneCardPerContact() throws Exception {
        newContact("Anna", "Alpha");
        newContact("Bert", "Beta");

        final String body = mockMvc.perform(asUser(get("/api/contacts/export/vcard")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"contacts.vcf\""))
            .andReturn().getResponse().getContentAsString();

        final List<VCard> cards = Ezvcard.parse(body).all();
        assertEquals(2, cards.size());
    }

    @Test
    void singleContactExportReturns404ForMissingContact() throws Exception {
        mockMvc.perform(asUser(get("/api/contacts/" + java.util.UUID.randomUUID() + "/vcard")))
            .andExpect(status().isNotFound());
    }
}
