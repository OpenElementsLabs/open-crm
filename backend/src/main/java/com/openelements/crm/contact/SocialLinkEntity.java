package com.openelements.crm.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * JPA entity representing a social network link belonging to a contact.
 */
@Entity
@Table(name = "contact_social_links")
public class SocialLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "network_type", nullable = false, length = 20)
    private SocialNetworkType networkType;

    @Column(name = "\"value\"", nullable = false, length = 500)
    private String value;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SocialLinkEntity() {
    }

    public UUID getId() {
        return id;
    }

    public SocialNetworkType getNetworkType() {
        return networkType;
    }

    public void setNetworkType(final SocialNetworkType networkType) {
        this.networkType = networkType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
