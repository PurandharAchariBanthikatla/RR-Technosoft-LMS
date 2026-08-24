package com.rrtechnosoft.lms.entity;

import com.rrtechnosoft.lms.entity.enums.DigestFrequency;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Singleton row (enforced by a unique index on {@link #singletonGuard}). */
@Entity
@Table(name = "notification_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettings {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "singleton_guard", nullable = false)
    @Builder.Default
    private Boolean singletonGuard = true;

    @Column(name = "smtp_host")
    private String smtpHost;

    @Column(name = "smtp_port", nullable = false)
    @Builder.Default
    private Integer smtpPort = 587;

    @Column(name = "smtp_username")
    private String smtpUsername;

    @Column(name = "smtp_password_encrypted", columnDefinition = "TEXT")
    private String smtpPasswordEncrypted;

    @Column(name = "smtp_use_tls", nullable = false)
    @Builder.Default
    private Boolean smtpUseTls = true;

    @Column(name = "from_name", nullable = false, length = 150)
    @Builder.Default
    private String fromName = "RR TECHNOSOFT LMS";

    @Column(name = "from_email")
    private String fromEmail;

    @Column(name = "email_notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean emailNotificationsEnabled = true;

    @Column(name = "sms_notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean smsNotificationsEnabled = false;

    @Column(name = "push_notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean pushNotificationsEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "digest_frequency", nullable = false)
    @Builder.Default
    private DigestFrequency digestFrequency = DigestFrequency.DAILY;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
