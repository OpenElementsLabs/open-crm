package com.openelements.crm.opportunity;

import com.openelements.crm.AbstractDbTest;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.spring.base.services.user.UserEntity;
import com.openelements.spring.base.services.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Repository-layer tests for {@link OpportunityRepository}: persistence round-trip (including the
 * status enum and the {@code NUMERIC(12,2)} value) and the derived existence queries backing the
 * delete-blocking rules.
 */
class OpportunityRepositoryTest extends AbstractDbTest {

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    private UserEntity newOwner() {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO oe_spring_services.users (id, sub, user_name, name, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            id, "owner", "owner", "Owner");
        return userRepository.findById(id).orElseThrow();
    }

    private OpportunityEntity persistOpportunity(final CompanyEntity company, final ContactEntity mainContact) {
        final OpportunityEntity opportunity = new OpportunityEntity();
        opportunity.setTitle("Deal");
        opportunity.setStatus(OpportunityStatus.WON);
        opportunity.setEstimatedValue(new BigDecimal("1234.56"));
        opportunity.setCompany(company);
        opportunity.setMainContact(mainContact);
        opportunity.setOwner(newOwner());
        return opportunityRepository.saveAndFlush(opportunity);
    }

    @Test
    @Transactional
    void persistsAndReadsBackAllScalarFields() {
        final CompanyEntity company = newCompany();
        final ContactEntity main = newContact("Max");
        final ContactEntity additional = newContact("Erika");
        final OpportunityEntity opportunity = new OpportunityEntity();
        opportunity.setTitle("Deal");
        opportunity.setStage("Angebot");
        opportunity.setStatus(OpportunityStatus.WON);
        opportunity.setProduct("Support & Care");
        opportunity.setEstimatedValue(new BigDecimal("25000.00"));
        opportunity.setCompany(company);
        opportunity.setMainContact(main);
        opportunity.setOwner(newOwner());
        opportunity.setAdditionalContacts(Set.of(additional));
        final UUID id = opportunityRepository.saveAndFlush(opportunity).getId();

        final OpportunityEntity loaded = opportunityRepository.findById(id).orElseThrow();
        assertEquals("Deal", loaded.getTitle());
        assertEquals("Angebot", loaded.getStage());
        assertEquals(OpportunityStatus.WON, loaded.getStatus());
        assertEquals("Support & Care", loaded.getProduct());
        assertEquals(0, new BigDecimal("25000.00").compareTo(loaded.getEstimatedValue()));
        assertEquals(1, loaded.getAdditionalContacts().size());
    }

    @Test
    void existenceQueriesBackTheDeleteBlockingRules() {
        final CompanyEntity company = newCompany();
        final ContactEntity main = newContact("Max");
        final ContactEntity unrelated = newContact("Erika");
        persistOpportunity(company, main);

        assertTrue(opportunityRepository.existsByCompanyId(company.getId()));
        assertFalse(opportunityRepository.existsByCompanyId(UUID.randomUUID()));
        assertTrue(opportunityRepository.existsByMainContactId(main.getId()));
        assertFalse(opportunityRepository.existsByMainContactId(unrelated.getId()));
    }
}
