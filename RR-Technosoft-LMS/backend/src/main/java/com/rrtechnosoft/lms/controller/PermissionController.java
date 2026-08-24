package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.UpdatePermissionMatrixRequest;
import com.rrtechnosoft.lms.dto.response.PermissionMatrixResponse;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.PermissionMatrixService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Backs the Permission Matrix UI. Restricted to SUPER_ADMIN at both the
 * URL layer (SecurityConfig: /administration/** -> hasRole('SUPER_ADMIN'))
 * and here, as defense in depth.
 */
@RestController
@RequestMapping("/administration/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PermissionController {

    private final PermissionMatrixService permissionMatrixService;

    @GetMapping("/matrix")
    public ResponseEntity<PermissionMatrixResponse> getMatrix() {
        return ResponseEntity.ok(permissionMatrixService.getMatrix());
    }

    @PutMapping("/matrix")
    public ResponseEntity<PermissionMatrixResponse> updateMatrix(@Valid @RequestBody UpdatePermissionMatrixRequest request,
                                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(permissionMatrixService.updateMatrix(request, principal.getId()));
    }
}
