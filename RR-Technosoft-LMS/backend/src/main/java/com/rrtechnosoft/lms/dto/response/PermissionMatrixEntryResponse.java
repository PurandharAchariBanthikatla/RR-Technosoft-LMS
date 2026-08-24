package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.enums.UserRole;

import java.util.UUID;

public record PermissionMatrixEntryResponse(
        UUID permissionId,
        String permissionCode,
        String permissionName,
        String category,
        UserRole role,
        boolean allowed
) {}
