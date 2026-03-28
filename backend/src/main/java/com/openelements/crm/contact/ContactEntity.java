package com.openelements.crm.contact;

import com.openelements.crm.company.CompanyEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * JPA entity representing a contact person in the CRM system.
 */
@Entity
@Table(name = "contacts")
public class ContactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 255)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 255)
    private String lastName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "position", length = 255)
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "linkedin_url", length = 500)
    private String linkedInUrl;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private CompanyEntity company;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "photo")
    private byte[] photo;

    @Column(name = "photo_content_type", length = 50)
    private String photoContentType;

    @Column(name = "synced_to_brevo", nullable = false)
    private boolean syncedToBrevo = false;

    @Column(name = "double_opt_in", nullable = false)
    private boolean doubleOptIn = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", length = 5)
    private Language language;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Default constructor required by JPA.
     */
    protected ContactEntity() {
    }

    /**
     * Returns the unique identifier of this contact.
     *
     * @return the contact ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Returns the first name of this contact.
     *
     * @return the first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the first name of this contact.
     *
     * @param firstName the first name
     */
    public void setFirstName(final String firstName) {
        this.firstName = Objects.requireNonNull(firstName, "firstName must not be null");
    }

    /**
     * Returns the last name of this contact.
     *
     * @return the last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the last name of this contact.
     *
     * @param lastName the last name
     */
    public void setLastName(final String lastName) {
        this.lastName = Objects.requireNonNull(lastName, "lastName must not be null");
    }

    /**
     * Returns the email address of this contact.
     *
     * @return the email address, or null
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address of this contact.
     *
     * @param email the email address
     */
    public void setEmail(final String email) {
        this.email = email;
    }

    /**
     * Returns the job position of this contact.
     *
     * @return the position, or null
     */
    public String getPosition() {
        return position;
    }

    /**
     * Sets the job position of this contact.
     *
     * @param position the position
     */
    public void setPosition(final String position) {
        this.position = position;
    }

    /**
     * Returns the gender of this contact.
     *
     * @return the gender, or null if unknown
     */
    public Gender getGender() {
        return gender;
    }

    /**
     * Sets the gender of this contact.
     *
     * @param gender the gender, or null if unknown
     */
    public void setGender(final Gender gender) {
        this.gender = gender;
    }

    /**
     * Returns the LinkedIn profile URL of this contact.
     *
     * @return the LinkedIn URL, or null
     */
    public String getLinkedInUrl() {
        return linkedInUrl;
    }

    /**
     * Sets the LinkedIn profile URL of this contact.
     *
     * @param linkedInUrl the LinkedIn URL
     */
    public void setLinkedInUrl(final String linkedInUrl) {
        this.linkedInUrl = linkedInUrl;
    }

    /**
     * Returns the phone number of this contact.
     *
     * @return the phone number, or null
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Sets the phone number of this contact.
     *
     * @param phoneNumber the phone number
     */
    public void setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Returns the company this contact belongs to.
     *
     * @return the company entity, or null if the contact is independent
     */
    public CompanyEntity getCompany() {
        return company;
    }

    /**
     * Sets the company this contact belongs to.
     *
     * @param company the company entity, or null
     */
    public void setCompany(final CompanyEntity company) {
        this.company = company;
    }

    /**
     * Returns the birthday of this contact.
     *
     * @return the birthday, or null
     */
    public LocalDate getBirthday() {
        return birthday;
    }

    /**
     * Sets the birthday of this contact.
     *
     * @param birthday the birthday, or null
     */
    public void setBirthday(final LocalDate birthday) {
        this.birthday = birthday;
    }

    /**
     * Returns the photo image data.
     *
     * @return the photo bytes, or null if no photo is set
     */
    public byte[] getPhoto() {
        return photo;
    }

    /**
     * Sets the photo image data.
     *
     * @param photo the photo bytes, or null to remove
     */
    public void setPhoto(final byte[] photo) {
        this.photo = photo;
    }

    /**
     * Returns the MIME content type of the photo.
     *
     * @return the content type, or null if no photo is set
     */
    public String getPhotoContentType() {
        return photoContentType;
    }

    /**
     * Sets the MIME content type of the photo.
     *
     * @param photoContentType the content type
     */
    public void setPhotoContentType(final String photoContentType) {
        this.photoContentType = photoContentType;
    }

    /**
     * Returns whether this contact is synced to Brevo.
     *
     * @return true if synced to Brevo
     */
    public boolean isSyncedToBrevo() {
        return syncedToBrevo;
    }

    /**
     * Sets the Brevo sync status. This should only be called by the Brevo synchronization service.
     *
     * @param syncedToBrevo the sync status
     */
    public void setSyncedToBrevo(final boolean syncedToBrevo) {
        this.syncedToBrevo = syncedToBrevo;
    }

    /**
     * Returns whether this contact has completed double opt-in via Brevo.
     *
     * @return true if double opt-in is confirmed
     */
    public boolean isDoubleOptIn() {
        return doubleOptIn;
    }

    /**
     * Sets the double opt-in status. This should only be called by the Brevo synchronization service.
     *
     * @param doubleOptIn the double opt-in status
     */
    public void setDoubleOptIn(final boolean doubleOptIn) {
        this.doubleOptIn = doubleOptIn;
    }

    /**
     * Returns the preferred language of this contact.
     *
     * @return the language
     */
    public Language getLanguage() {
        return language;
    }

    /**
     * Sets the preferred language of this contact.
     *
     * @param language the language, or null if unknown
     */
    public void setLanguage(final Language language) {
        this.language = language;
    }

    /**
     * Returns the creation timestamp.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the last update timestamp.
     *
     * @return the update timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ContactEntity that = (ContactEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ContactEntity[id=" + id + ", lastName=" + lastName + "]";
    }
}
