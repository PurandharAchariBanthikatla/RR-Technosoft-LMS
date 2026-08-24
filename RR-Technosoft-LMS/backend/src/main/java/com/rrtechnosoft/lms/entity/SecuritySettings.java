package com.rrtechnosoft.lms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Singleton row (enforced by a unique index on {@link #singletonGuard}). */
@Entity
@Table(name = "security_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecuritySettings {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "singleton_guard", nullable = false)
    @Builder.Default
    private Boolean singletonGuard = true;

    @Column(name = "password_min_length", nullable = false)
    @Builder.Default
    private Integer passwordMinLength = 8;

    @Column(name = "password_require_uppercase", nullable = false)
    @Builder.Default
    private Boolean passwordRequireUppercase = true;

    @Column(name = "password_require_number", nullable = false)
    @Builder.Default
    private Boolean passwordRequireNumber = true;

    @Column(name = "password_require_special_char", nullable = false)
    @Builder.Default
    private Boolean passwordRequireSpecialChar = true;

    @Column(name = "password_expiry_days", nullable = false)
    @Builder.Default
    private Integer passwordExpiryDays = 90;

    @Column(name = "max_login_attempts", nullable = false)
    @Builder.Default
    private Integer maxLoginAttempts = 5;

    @Column(name = "lockout_duration_minutes", nullable = false)
    @Builder.Default
    private Integer lockoutDurationMinutes = 15;

    @Column(name = "session_timeout_minutes", nullable = false)
    @Builder.Default
    private Integer sessionTimeoutMinutes = 60;

    @Column(name = "mfa_required_for_admins", nullable = false)
    @Builder.Default
    private Boolean mfaRequiredForAdmins = false;

    @Column(name = "allowed_ip_ranges", columnDefinition = "TEXT")
    private String allowedIpRanges;

    @Column(name = "force_logout_on_password_change", nullable = false)
    @Builder.Default
    private Boolean forceLogoutOnPasswordChange = true;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
