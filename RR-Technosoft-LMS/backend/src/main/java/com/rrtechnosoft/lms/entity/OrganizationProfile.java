package com.rrtechnosoft.lms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Singleton row (enforced by a unique index on {@link #singletonGuard}). */
@Entity
@Table(name = "organization_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationProfile {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "singleton_guard", nullable = false)
    @Builder.Default
    private Boolean singletonGuard = true;

    @Column(name = "org_name", nullable = false, length = 200)
    private String orgName;

    @Column(name = "legal_name", length = 200)
    private String legalName;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "favicon_url", columnDefinition = "TEXT")
    private String faviconUrl;

    private String website;

    @Column(name = "support_email")
    private String supportEmail;

    @Column(name = "support_phone", length = 20)
    private String supportPhone;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    private String city;
    private String state;

    @Builder.Default
    private String country = "India";

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Builder.Default
    private String timezone = "Asia/Kolkata";

    @Column(name = "date_format", length = 20)
    @Builder.Default
    private String dateFormat = "dd-MM-yyyy";

    @Column(name = "updated_by")
    private UUID updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
