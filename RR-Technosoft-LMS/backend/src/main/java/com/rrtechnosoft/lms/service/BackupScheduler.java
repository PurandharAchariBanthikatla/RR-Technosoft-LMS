package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.entity.BackupConfig;
import com.rrtechnosoft.lms.entity.BackupRun;
import com.rrtechnosoft.lms.repository.BackupConfigRepository;
import com.rrtechnosoft.lms.repository.BackupRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Two independent, minute-resolution jobs:
 *
 *  1. Auto-backup — {@link BackupConfig#getScheduleCron()} is a user-editable
 *     cron expression (Administration > Backup & Restore), but nothing ever
 *     read it before this class existed. Every minute we compute whether the
 *     cron's next fire time after the last recorded run has passed; if so
 *     (and auto-backup is enabled) we trigger a run the same way the manual
 *     "Run Backup Now" button does.
 *
 *  2. Retention cleanup — {@link BackupConfig#getRetentionDays()} was stored
 *     but never enforced. Once daily, delete backup_run rows (and their
 *     on-disk dump files, for LOCAL storage) older than the retention window.
 *
 * Both are best-effort: a misconfigured cron string or a missing file on
 * disk logs a warning rather than throwing, so one bad run doesn't wedge
 * every subsequent scheduler tick.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    private final BackupConfigRepository backupConfigRepository;
    private final BackupRunRepository backupRunRepository;
    private final BackupService backupService;

    @Scheduled(cron = "0 * * * * *") // every minute, on the minute
    public void maybeRunScheduledBackup() {
        BackupConfig config = backupConfigRepository.findBySingletonGuardTrue().orElse(null);
        if (config == null || !Boolean.TRUE.equals(config.getAutoBackupEnabled())) {
            return;
        }

        CronExpression cron;
        try {
            cron = CronExpression.parse(config.getScheduleCron());
        } catch (IllegalArgumentException ex) {
            log.warn("Backup schedule cron '{}' is invalid — skipping auto-backup check", config.getScheduleCron());
            return;
        }

        OffsetDateTime lastRunStartedAt = backupRunRepository.findFirstByOrderByStartedAtDesc()
                .map(BackupRun::getStartedAt)
                .orElse(OffsetDateTime.now().minusYears(1)); // no runs yet -> always due once the schedule allows

        var nextDue = cron.next(lastRunStartedAt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
        if (nextDue != null && !nextDue.isAfter(java.time.LocalDateTime.now())) {
            log.info("Scheduled backup is due (cron '{}') — triggering", config.getScheduleCron());
            // actorId = null -> AuditLogService/AuditLogResponse render this as "System" rather than a real user.
            backupService.triggerBackup(null);
        }
    }

    @Scheduled(cron = "0 30 3 * * *") // 03:30 daily — after the typical backup window
    public void purgeExpiredBackups() {
        BackupConfig config = backupConfigRepository.findBySingletonGuardTrue().orElse(null);
        if (config == null || config.getRetentionDays() == null || config.getRetentionDays() <= 0) {
            return;
        }

        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(config.getRetentionDays());
        var expired = backupRunRepository.findAllByStartedAtBefore(cutoff);
        if (expired.isEmpty()) {
            return;
        }

        int deletedFiles = 0;
        for (BackupRun run : expired) {
            if (run.getFileLocation() != null) {
                try {
                    Files.deleteIfExists(Path.of(run.getFileLocation()));
                    deletedFiles++;
                } catch (Exception ex) {
                    log.warn("Could not delete expired backup file {}: {}", run.getFileLocation(), ex.getMessage());
                }
            }
        }
        backupRunRepository.deleteAll(expired);
        log.info("Retention cleanup: removed {} backup_run record(s), {} file(s), older than {} day(s)",
                expired.size(), deletedFiles, config.getRetentionDays());
    }
}
