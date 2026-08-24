package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.entity.BackupConfig;
import com.rrtechnosoft.lms.entity.BackupRun;
import com.rrtechnosoft.lms.repository.BackupConfigRepository;
import com.rrtechnosoft.lms.repository.BackupRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupSchedulerTest {

    @Mock private BackupConfigRepository backupConfigRepository;
    @Mock private BackupRunRepository backupRunRepository;
    @Mock private BackupService backupService;

    private BackupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new BackupScheduler(backupConfigRepository, backupRunRepository, backupService);
    }

    @Test
    void maybeRunScheduledBackup_doesNothingWhenAutoBackupDisabled() {
        BackupConfig config = BackupConfig.builder().autoBackupEnabled(false).scheduleCron("0 0 2 * * *").build();
        when(backupConfigRepository.findBySingletonGuardTrue()).thenReturn(Optional.of(config));

        scheduler.maybeRunScheduledBackup();

        verifyNoInteractions(backupRunRepository, backupService);
    }

    @Test
    void maybeRunScheduledBackup_triggersWhenNoPriorRunExists() {
        // "every minute" cron with no prior run at all -> immediately due.
        BackupConfig config = BackupConfig.builder().autoBackupEnabled(true).scheduleCron("* * * * * *").build();
        when(backupConfigRepository.findBySingletonGuardTrue()).thenReturn(Optional.of(config));
        when(backupRunRepository.findFirstByOrderByStartedAtDesc()).thenReturn(Optional.empty());

        scheduler.maybeRunScheduledBackup();

        verify(backupService).triggerBackup(isNull());
    }

    @Test
    void maybeRunScheduledBackup_skipsWhenNotYetDue() {
        // Daily-at-2am cron, last run was seconds ago -> not due yet.
        BackupConfig config = BackupConfig.builder().autoBackupEnabled(true).scheduleCron("0 0 2 * * *").build();
        when(backupConfigRepository.findBySingletonGuardTrue()).thenReturn(Optional.of(config));
        when(backupRunRepository.findFirstByOrderByStartedAtDesc())
                .thenReturn(Optional.of(BackupRun.builder().startedAt(OffsetDateTime.now()).build()));

        scheduler.maybeRunScheduledBackup();

        verify(backupService, never()).triggerBackup(any());
    }

    @Test
    void maybeRunScheduledBackup_skipsSilentlyOnInvalidCron() {
        BackupConfig config = BackupConfig.builder().autoBackupEnabled(true).scheduleCron("not a cron").build();
        when(backupConfigRepository.findBySingletonGuardTrue()).thenReturn(Optional.of(config));

        scheduler.maybeRunScheduledBackup();

        verifyNoInteractions(backupService);
    }

    @Test
    void purgeExpiredBackups_deletesRunsOlderThanRetentionWindow() {
        BackupConfig config = BackupConfig.builder().retentionDays(30).build();
        BackupRun expired = BackupRun.builder().id(UUID.randomUUID()).fileLocation(null).build();
        when(backupConfigRepository.findBySingletonGuardTrue()).thenReturn(Optional.of(config));
        when(backupRunRepository.findAllByStartedAtBefore(any())).thenReturn(List.of(expired));

        scheduler.purgeExpiredBackups();

        verify(backupRunRepository).deleteAll(eq(List.of(expired)));
    }

    @Test
    void purgeExpiredBackups_noOpWhenRetentionDaysNotSet() {
        BackupConfig config = BackupConfig.builder().retentionDays(null).build();
        when(backupConfigRepository.findBySingletonGuardTrue()).thenReturn(Optional.of(config));

        scheduler.purgeExpiredBackups();

        verify(backupRunRepository, never()).findAllByStartedAtBefore(any());
        verify(backupRunRepository, never()).deleteAll(anyList());
    }
}
