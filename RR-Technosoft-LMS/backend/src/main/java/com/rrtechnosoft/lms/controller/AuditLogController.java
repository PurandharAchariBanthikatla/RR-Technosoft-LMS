package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.response.AuditLogResponse;
import com.rrtechnosoft.lms.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Searchable audit history for SUPER_ADMIN. Restricted via SecurityConfig's
 * blanket "/administration/**" -> SUPER_ADMIN rule (audit logs are sensitive
 * enough to keep off the ADMIN role, unlike most of the Administration module's
 * read endpoints).
 */
@RestController
@RequestMapping("/administration/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> search(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 25) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.search(actorId, action, entityType, entityId, from, to, pageable));
    }
}
