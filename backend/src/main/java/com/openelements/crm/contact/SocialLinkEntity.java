package com.openelements.crm.contact;

import com.openelements.spring.base.data.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * JPA entity representing a social network link belonging to a contact.
 */
@Entity
@Table(name = "contact_social_links")
public class SocialLinkEntity extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "network_type", nullable = false, length = 20)
    private SocialNetworkType networkType;

    @Column(name = "\"value\"", nullable = false, length = 500)
    private String value;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    public SocialLinkEntity() {
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

}
