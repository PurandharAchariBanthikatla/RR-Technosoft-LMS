package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.Permission;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String code,
        String name,
        String description,
        String category,
        Boolean isSystem,
        OffsetDateTime createdAt
) {
    public static PermissionResponse from(Permission p) {
        return new PermissionResponse(p.getId(), p.getCode(), p.getName(), p.getDescription(),
                p.getCategory(), p.getIsSystem(), p.getCreatedAt());
    }
}
