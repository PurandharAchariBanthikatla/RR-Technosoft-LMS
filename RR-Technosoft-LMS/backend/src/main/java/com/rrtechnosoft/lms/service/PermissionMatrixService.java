package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.UpdatePermissionMatrixRequest;
import com.rrtechnosoft.lms.dto.response.PermissionMatrixEntryResponse;
import com.rrtechnosoft.lms.dto.response.PermissionMatrixResponse;
import com.rrtechnosoft.lms.dto.response.PermissionResponse;
import com.rrtechnosoft.lms.entity.Permission;
import com.rrtechnosoft.lms.entity.RolePermission;
import com.rrtechnosoft.lms.entity.enums.UserRole;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.PermissionRepository;
import com.rrtechnosoft.lms.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Backs the Permission Matrix UI: Super Admins toggle which permissions
 * each role holds and the change takes effect immediately, with no
 * code change or redeploy required. SUPER_ADMIN itself is not editable
 * here — it always holds every permission, enforced by
 * {@link #guardEditableRole}, so the platform can never be left without
 * a fully-privileged role.
 */
@Service
@RequiredArgsConstructor
public class PermissionMatrixService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public PermissionMatrixResponse getMatrix() {
        List<Permission> permissions = permissionRepository.findAllByOrderByCategoryAscNameAsc();
        List<RolePermission> entries = rolePermissionRepository.findAllByOrderByRoleAsc();

        List<PermissionResponse> permissionResponses = permissions.stream()
                .map(PermissionResponse::from)
                .toList();

        List<PermissionMatrixEntryResponse> entryResponses = entries.stream()
                .map(e -> new PermissionMatrixEntryResponse(
                        e.getPermission().getId(), e.getPermission().getCode(), e.getPermission().getName(),
                        e.getPermission().getCategory(), e.getRole(), Boolean.TRUE.equals(e.getAllowed())))
                .toList();

        return new PermissionMatrixResponse(permissionResponses, entryResponses);
    }

    @Transactional
    public PermissionMatrixResponse updateMatrix(UpdatePermissionMatrixRequest request, UUID actorId) {
        for (UpdatePermissionMatrixRequest.Entry entry : request.entries()) {
            guardEditableRole(entry.role());

            Permission permission = permissionRepository.findById(entry.permissionId())
                    .orElseThrow(() -> ApiException.notFound("Permission not found: " + entry.permissionId()));

            RolePermission rolePermission = rolePermissionRepository
                    .findByRoleAndPermissionId(entry.role(), permission.getId())
                    .orElseGet(() -> RolePermission.builder()
                            .role(entry.role())
                            .permission(permission)
                            .allowed(false)
                            .build());

            rolePermission.setAllowed(entry.allowed());
            rolePermission.setUpdatedBy(actorId);
            rolePermissionRepository.save(rolePermission);

            auditLogService.log(actorId,
                    entry.allowed() ? "GRANT_PERMISSION" : "REVOKE_PERMISSION",
                    "RolePermission", rolePermission.getId(), null);
        }
        return getMatrix();
    }

    /** SUPER_ADMIN is always fully privileged and cannot be edited from the matrix. */
    private void guardEditableRole(UserRole role) {
        if (role == UserRole.SUPER_ADMIN) {
            throw ApiException.badRequest("SUPER_ADMIN always holds every permission and cannot be edited");
        }
    }

    /** Used by {@link com.rrtechnosoft.lms.security.PermissionGuard} for runtime checks. */
    @Transactional(readOnly = true)
    public boolean isAllowed(UserRole role, String permissionCode) {
        if (role == UserRole.SUPER_ADMIN) {
            return true;
        }
        return rolePermissionRepository.existsByRoleAndPermission_CodeAndAllowedTrue(role, permissionCode);
    }
}
