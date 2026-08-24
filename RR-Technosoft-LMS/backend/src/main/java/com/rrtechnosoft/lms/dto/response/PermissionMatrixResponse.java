package com.rrtechnosoft.lms.dto.response;

import java.util.List;

/** Full matrix: one row per permission, one column per role. */
public record PermissionMatrixResponse(
        List<PermissionResponse> permissions,
        List<PermissionMatrixEntryResponse> entries
) {}
