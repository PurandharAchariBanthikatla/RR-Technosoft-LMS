package com.rrtechnosoft.lms.entity;

import com.rrtechnosoft.lms.entity.enums.BackupRunStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "backup_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupRun {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backup_config_id", nullable = false)
    private BackupConfig backupConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BackupRunStatus status = BackupRunStatus.PENDING;

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @Column(name = "file_location", columnDefinition = "TEXT")
    private String fileLocation;

    @Column(name = "size_mb", precision = 12, scale = 2)
    private BigDecimal sizeMb;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Builder.Default
    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
