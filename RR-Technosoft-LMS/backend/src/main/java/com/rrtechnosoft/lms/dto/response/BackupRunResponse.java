package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.BackupRun;
import com.rrtechnosoft.lms.entity.enums.BackupRunStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BackupRunResponse(
        UUID id,
        BackupRunStatus status,
        UUID triggeredBy,
        String fileLocation,
        BigDecimal sizeMb,
        String errorMessage,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt
) {
    public static BackupRunResponse from(BackupRun r) {
        return new BackupRunResponse(r.getId(), r.getStatus(), r.getTriggeredBy(), r.getFileLocation(),
                r.getSizeMb(), r.getErrorMessage(), r.getStartedAt(), r.getCompletedAt());
    }
}
