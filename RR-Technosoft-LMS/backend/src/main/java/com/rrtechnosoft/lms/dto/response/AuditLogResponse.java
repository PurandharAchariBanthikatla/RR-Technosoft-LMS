package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.AuditLog;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String actorName,
        String actorEmail,
        String action,
        String entityType,
        UUID entityId,
        String metadata,
        String ipAddress,
        OffsetDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog log, String actorName, String actorEmail) {
        return new AuditLogResponse(
                log.getId(),
                log.getActorId(),
                actorName,
                actorEmail,
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getMetadata(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}
