package com.openelements.crm.contact;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.parameter.ImageType;
import ezvcard.property.Birthday;
import ezvcard.property.Photo;
import ezvcard.property.StructuredName;
import ezvcard.property.Uid;

import java.util.List;
import java.util.Objects;

/**
 * Maps {@link ContactEntity} instances to vCard documents.
 *
 * <p>Output is vCard <strong>3.0</strong>: it is the interchange format Apple
 * Contacts exports by default and imports most reliably (including embedded
 * photos), and it is equally well understood by Outlook and Google Contacts.
 * vCard-4.0-only properties (e.g. {@code GENDER}, {@code LANG}) are therefore
 * intentionally not emitted — those fields are not part of an address-book card
 * and remain available through the CSV export.
 *
 * <p>This mapper must be invoked inside the owning service's read-only
 * transaction: {@link ContactEntity#getPhoto()} is lazily fetched, so photo
 * bytes are only available while the persistence context is open.
 */
final class ContactVCardMapper {

    private ContactVCardMapper() {
    }

    /**
     * Serializes the given contacts to a single vCard 3.0 document (one card per
     * contact).
     *
     * @param contacts the contacts to export; must not be {@code null}
     * @return the vCard document as a string
     */
    static String toVCardString(final List<ContactEntity> contacts) {
        Objects.requireNonNull(contacts, "contacts must not be null");
        final List<VCard> cards = contacts.stream()
            .map(ContactVCardMapper::toVCard)
            .toList();
        return Ezvcard.write(cards).version(VCardVersion.V3_0).go();
    }

    /**
     * Converts a single contact to a vCard.
     *
     * @param contact the contact to convert; must not be {@code null}
     * @return the vCard
     */
    static VCard toVCard(final ContactEntity contact) {
        Objects.requireNonNull(contact, "contact must not be null");
        final VCard vcard = new VCard();

        final StructuredName name = new StructuredName();
        name.setFamily(contact.getLastName());
        name.setGiven(contact.getFirstName());
        if (hasText(contact.getTitle())) {
            name.getPrefixes().add(contact.getTitle().trim());
        }
        vcard.setStructuredName(name);
        // FN is mandatory in vCard; derive a human-readable display name.
        vcard.setFormattedName(formattedName(contact));

        if (hasText(contact.getPosition())) {
            vcard.addTitle(contact.getPosition());
        }
        if (contact.getCompany() != null && hasText(contact.getCompany().getName())) {
            vcard.setOrganization(contact.getCompany().getName());
        }
        if (hasText(contact.getEmail())) {
            vcard.addEmail(contact.getEmail());
        }
        if (hasText(contact.getPhoneNumber())) {
            vcard.addTelephoneNumber(contact.getPhoneNumber());
        }
        if (contact.getBirthday() != null) {
            vcard.setBirthday(new Birthday(contact.getBirthday()));
        }
        contact.getSocialLinks().forEach(link -> {
            if (hasText(link.getUrl())) {
                vcard.addUrl(link.getUrl());
            }
        });
        if (hasText(contact.getDescription())) {
            vcard.addNote(contact.getDescription());
        }
        final byte[] photo = contact.getPhoto();
        if (photo != null && photo.length > 0) {
            vcard.addPhoto(new Photo(photo, imageType(contact.getPhotoContentType())));
        }
        // Stable identity across re-exports so address-book clients can match
        // an existing card instead of creating a duplicate on re-import.
        if (contact.getId() != null) {
            vcard.setUid(new Uid("urn:uuid:" + contact.getId()));
        }
        return vcard;
    }

    private static String formattedName(final ContactEntity contact) {
        final StringBuilder sb = new StringBuilder();
        if (hasText(contact.getTitle())) {
            sb.append(contact.getTitle().trim()).append(' ');
        }
        if (hasText(contact.getFirstName())) {
            sb.append(contact.getFirstName().trim()).append(' ');
        }
        if (hasText(contact.getLastName())) {
            sb.append(contact.getLastName().trim());
        }
        final String fn = sb.toString().trim();
        return fn.isEmpty() ? "Unknown" : fn;
    }

    private static ImageType imageType(final String contentType) {
        if (contentType == null) {
            return ImageType.JPEG;
        }
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ImageType.PNG;
            case "image/gif" -> ImageType.GIF;
            default -> ImageType.JPEG;
        };
    }

    private static boolean hasText(final String value) {
        return value != null && !value.isBlank();
    }
}
