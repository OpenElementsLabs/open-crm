package com.openelements.crm.contact.csvimport;

import com.openelements.crm.contact.ContactDto;
import com.openelements.crm.contact.ContactService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Persists imported contacts one row at a time with partial-success semantics.
 */
@Service
public class ContactImportRowSaver {

    private final ContactService contactService;

    public ContactImportRowSaver(final ContactService contactService) {
        this.contactService = Objects.requireNonNull(contactService, "contactService must not be null");
    }

    /**
     * Saves a single contact in a new transaction so one failed row does not roll back rows that
     * were already imported. {@link Propagation#REQUIRES_NEW} suspends any outer transaction and
     * commits each save independently.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ContactDto save(final ContactDto contact) {
        return contactService.save(contact);
    }
}
