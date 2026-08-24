package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.UpdateBackupConfigRequest;
import com.rrtechnosoft.lms.dto.response.BackupConfigResponse;
import com.rrtechnosoft.lms.dto.response.BackupRunResponse;
import com.rrtechnosoft.lms.entity.BackupConfig;
import com.rrtechnosoft.lms.entity.BackupRun;
import com.rrtechnosoft.lms.entity.enums.BackupRunStatus;
import com.rrtechnosoft.lms.entity.enums.BackupStorageType;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.BackupConfigRepository;
import com.rrtechnosoft.lms.repository.BackupRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Configuration lives in {@code backup_configs}; each backup attempt
 * (scheduled or manually triggered) is logged to {@code backup_runs} so
 * the Administration UI can show real history and status, not a static
 * placeholder. Execution shells out to {@code pg_dump}, which must be on
 * the application server's PATH — the same operational prerequisite any
 * Postgres-backed backup feature has.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final BackupConfigRepository backupConfigRepository;
    private final BackupRunRepository backupRunRepository;
    private final AuditLogService auditLogService;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Transactional(readOnly = true)
    public BackupConfigResponse getConfig() {
        return BackupConfigResponse.from(loadSingleton());
    }

    @Transactional
    public BackupConfigResponse updateConfig(UpdateBackupConfigRequest request, UUID actorId) {
        BackupConfig config = loadSingleton();
        config.setScheduleCron(request.scheduleCron());
        config.setRetentionDays(request.retentionDays());
        config.setStorageType(request.storageType());
        config.setStorageLocation(request.storageLocation());
        config.setAutoBackupEnabled(request.autoBackupEnabled());
        config.setUpdatedBy(actorId);
        backupConfigRepository.save(config);
        auditLogService.log(actorId, "UPDATE_BACKUP_CONFIG", "BackupConfig", config.getId(), null);
        return BackupConfigResponse.from(config);
    }

    @Transactional(readOnly = true)
    public Page<BackupRunResponse> listRuns(Pageable pageable) {
        return backupRunRepository.findAllByOrderByStartedAtDesc(pageable).map(BackupRunResponse::from);
    }

    /**
     * Records a PENDING run synchronously (so the caller gets an id back
     * immediately) then executes the dump asynchronously.
     */
    @Transactional
    public BackupRunResponse triggerBackup(UUID actorId) {
        BackupConfig config = loadSingleton();
        BackupRun run = BackupRun.builder()
                .backupConfig(config)
                .status(BackupRunStatus.PENDING)
                .triggeredBy(actorId)
                .build();
        run = backupRunRepository.save(run);
        auditLogService.log(actorId, "TRIGGER_BACKUP", "BackupRun", run.getId(), null);
        runBackupAsync(run.getId(), config.getStorageLocation(), config.getStorageType());
        return BackupRunResponse.from(run);
    }

    @Async
    public void runBackupAsync(UUID runId, String storageLocation, BackupStorageType storageType) {
        BackupRun run = backupRunRepository.findById(runId).orElse(null);
        if (run == null) return;

        run.setStatus(BackupRunStatus.RUNNING);
        backupRunRepository.save(run);

        String fileName = "rr-lms-backup-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(OffsetDateTime.now()) + ".sql";
        try {
            Path targetDir = Path.of(storageType == BackupStorageType.S3
                    ? System.getProperty("java.io.tmpdir")
                    : storageLocation);
            Files.createDirectories(targetDir);
            Path dumpFile = targetDir.resolve(fileName);

            DbConnectionInfo db = DbConnectionInfo.parse(datasourceUrl);

            ProcessBuilder pb = new ProcessBuilder(
                    "pg_dump",
                    "--host=" + db.host(),
                    "--port=" + db.port(),
                    "--username=" + datasourceUsername,
                    "--format=plain",
                    "--file=" + dumpFile,
                    db.database()
            );
            pb.environment().put("PGPASSWORD", datasourcePassword);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.MINUTES);
            if (!finished || process.exitValue() != 0) {
                throw new IllegalStateException("pg_dump exited with code " + (finished ? process.exitValue() : "timeout"));
            }

            long sizeBytes = Files.size(dumpFile);
            run.setFileLocation(dumpFile.toString());
            run.setSizeMb(BigDecimal.valueOf(sizeBytes / 1024.0 / 1024.0).setScale(2, java.math.RoundingMode.HALF_UP));
            run.setStatus(BackupRunStatus.SUCCESS);
            run.setCompletedAt(OffsetDateTime.now());
        } catch (Exception ex) {
            log.error("Backup run {} failed", runId, ex);
            run.setStatus(BackupRunStatus.FAILED);
            run.setErrorMessage(ex.getMessage());
            run.setCompletedAt(OffsetDateTime.now());
        }
        backupRunRepository.save(run);
    }

    private BackupConfig loadSingleton() {
        return backupConfigRepository.findBySingletonGuardTrue()
                .orElseThrow(() -> ApiException.notFound("Backup configuration not found"));
    }

    /** Minimal parser for jdbc:postgresql://host:port/database URLs. */
    private record DbConnectionInfo(String host, String port, String database) {
        static DbConnectionInfo parse(String jdbcUrl) {
            // jdbc:postgresql://localhost:5432/rr_lms?params...
            String stripped = jdbcUrl.replaceFirst("^jdbc:postgresql://", "");
            String[] hostPortAndRest = stripped.split("/", 2);
            String[] hostPort = hostPortAndRest[0].split(":");
            String host = hostPort[0];
            String port = hostPort.length > 1 ? hostPort[1] : "5432";
            String database = hostPortAndRest.length > 1 ? hostPortAndRest[1].split("\\?")[0] : "postgres";
            return new DbConnectionInfo(host, port, database);
        }
    }
}
