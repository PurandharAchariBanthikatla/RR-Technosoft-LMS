package com.rrtechnosoft.lms.entity;

import com.rrtechnosoft.lms.entity.enums.BackupStorageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Singleton row (enforced by a unique index on {@link #singletonGuard}). */
@Entity
@Table(name = "backup_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupConfig {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "singleton_guard", nullable = false)
    @Builder.Default
    private Boolean singletonGuard = true;

    @Column(name = "schedule_cron", nullable = false, length = 100)
    @Builder.Default
    private String scheduleCron = "0 0 2 * * *";

    @Column(name = "retention_days", nullable = false)
    @Builder.Default
    private Integer retentionDays = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false)
    @Builder.Default
    private BackupStorageType storageType = BackupStorageType.LOCAL;

    @Column(name = "storage_location", nullable = false, length = 500)
    @Builder.Default
    private String storageLocation = "/var/backups/rr-lms";

    @Column(name = "auto_backup_enabled", nullable = false)
    @Builder.Default
    private Boolean autoBackupEnabled = true;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
