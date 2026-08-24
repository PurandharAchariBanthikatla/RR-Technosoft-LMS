package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.response.AuditLogResponse;
import com.rrtechnosoft.lms.entity.AuditLog;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.repository.AuditLogRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import com.rrtechnosoft.lms.repository.spec.AuditLogSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void log(UUID actorId, String action, String entityType, UUID entityId, String ipAddress) {
        AuditLog log = AuditLog.builder()
                .actorId(actorId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(log);
    }

    /**
     * Searchable audit history for the admin "Audit Logs" screen. Every filter
     * is optional; actor display names/emails are resolved in a single batch
     * query per page rather than N+1 per row.
     */
    public Page<AuditLogResponse> search(UUID actorId, String action, String entityType, UUID entityId,
                                          OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findAll(
                AuditLogSpecification.filter(actorId, action, entityType, entityId, from, to), pageable);

        var actorIds = page.getContent().stream()
                .map(AuditLog::getActorId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, User> actors = userRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return page.map(log -> {
            User actor = log.getActorId() != null ? actors.get(log.getActorId()) : null;
            String name = actor != null ? actor.getFullName() : "System";
            String email = actor != null ? actor.getEmail() : null;
            return AuditLogResponse.from(log, name, email);
        });
    }
}
