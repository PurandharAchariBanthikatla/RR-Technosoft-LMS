package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.BackupStorageType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateBackupConfigRequest(
        @NotBlank String scheduleCron,
        @Min(1) @NotNull Integer retentionDays,
        @NotNull BackupStorageType storageType,
        @NotBlank String storageLocation,
        @NotNull Boolean autoBackupEnabled
) {}
