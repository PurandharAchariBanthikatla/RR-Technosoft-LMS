package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.BackupConfig;
import com.rrtechnosoft.lms.entity.enums.BackupStorageType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BackupConfigResponse(
        UUID id,
        String scheduleCron,
        Integer retentionDays,
        BackupStorageType storageType,
        String storageLocation,
        Boolean autoBackupEnabled,
        OffsetDateTime updatedAt
) {
    public static BackupConfigResponse from(BackupConfig b) {
        return new BackupConfigResponse(b.getId(), b.getScheduleCron(), b.getRetentionDays(), b.getStorageType(),
                b.getStorageLocation(), b.getAutoBackupEnabled(), b.getUpdatedAt());
    }
}
