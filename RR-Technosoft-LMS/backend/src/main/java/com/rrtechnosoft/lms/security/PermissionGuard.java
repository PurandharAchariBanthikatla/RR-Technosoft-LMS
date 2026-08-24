package com.rrtechnosoft.lms.security;

import com.rrtechnosoft.lms.service.PermissionMatrixService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * SpEL-callable bean for dynamic, matrix-driven checks:
 * {@code @PreAuthorize("@permissionGuard.has('COURSE_MANAGE')")}.
 * Existing hasRole(...) checks elsewhere in the codebase are untouched —
 * this is additive, for new Administration-module endpoints and for any
 * controller that opts in going forward.
 */
@Component("permissionGuard")
@RequiredArgsConstructor
public class PermissionGuard {

    private final PermissionMatrixService permissionMatrixService;

    public boolean has(String permissionCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return permissionMatrixService.isAllowed(principal.getUser().getRole(), permissionCode);
    }
}
