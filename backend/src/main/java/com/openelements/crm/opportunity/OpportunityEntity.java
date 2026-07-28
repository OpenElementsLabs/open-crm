package com.openelements.crm.opportunity;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.spring.base.data.AbstractEntity;
import com.openelements.spring.base.services.comment.CommentEntity;
import com.openelements.spring.base.services.tag.TagEntity;
import com.openelements.spring.base.services.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * JPA entity representing a sales opportunity ("Deal") in the CRM system. An opportunity is linked
 * to exactly one company, exactly one main contact, and one responsible owner (user); it may
 * additionally reference 0–N further contacts and carry tags and comments like companies and
 * contacts.
 */
@Entity
@Table(name = "opportunities")
public class OpportunityEntity extends AbstractEntity {

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "stage", columnDefinition = "TEXT")
    private String stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OpportunityStatus status = OpportunityStatus.OPEN;

    @Column(name = "product", columnDefinition = "TEXT")
    private String product;

    @Column(name = "estimated_value", precision = 12, scale = 2)
    private BigDecimal estimatedValue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "main_contact_id", nullable = false)
    private ContactEntity mainContact;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @ManyToMany
    @JoinTable(
        name = "opportunity_contacts",
        joinColumns = @JoinColumn(name = "opportunity_id"),
        inverseJoinColumns = @JoinColumn(name = "contact_id")
    )
    private Set<ContactEntity> additionalContacts = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "opportunity_tags",
        joinColumns = @JoinColumn(name = "opportunity_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<TagEntity> tags = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "opportunity_comments",
        joinColumns = @JoinColumn(name = "opportunity_id"),
        inverseJoinColumns = @JoinColumn(name = "comment_id", unique = true)
    )
    private Set<CommentEntity> comments = new HashSet<>();

    /**
     * Default constructor required by JPA.
     */
    public OpportunityEntity() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = Objects.requireNonNull(title, "title must not be null");
    }

    public String getStage() {
        return stage;
    }

    public void setStage(final String stage) {
        this.stage = stage;
    }

    public OpportunityStatus getStatus() {
        return status;
    }

    public void setStatus(final OpportunityStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(final String product) {
        this.product = product;
    }

    public BigDecimal getEstimatedValue() {
        return estimatedValue;
    }

    public void setEstimatedValue(final BigDecimal estimatedValue) {
        this.estimatedValue = estimatedValue;
    }

    public CompanyEntity getCompany() {
        return company;
    }

    public void setCompany(final CompanyEntity company) {
        this.company = Objects.requireNonNull(company, "company must not be null");
    }

    public ContactEntity getMainContact() {
        return mainContact;
    }

    public void setMainContact(final ContactEntity mainContact) {
        this.mainContact = Objects.requireNonNull(mainContact, "mainContact must not be null");
    }

    public UserEntity getOwner() {
        return owner;
    }

    public void setOwner(final UserEntity owner) {
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
    }

    public Set<ContactEntity> getAdditionalContacts() {
        return additionalContacts;
    }

    public void setAdditionalContacts(final Set<ContactEntity> additionalContacts) {
        this.additionalContacts = additionalContacts;
    }

    public Set<TagEntity> getTags() {
        return tags;
    }

    public void setTags(final Set<TagEntity> tags) {
        this.tags = tags;
    }

    public Set<CommentEntity> getComments() {
        return comments;
    }

    public void setComments(final Set<CommentEntity> comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "OpportunityEntity[id=" + id() + ", title=" + title + "]";
    }
}
