package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.CreateAdminRequest;
import com.rrtechnosoft.lms.dto.response.UserSummaryResponse;
import com.rrtechnosoft.lms.entity.enums.AccountStatus;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.AdminManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Everything here is restricted to SUPER_ADMIN at both the URL layer
 * (SecurityConfig: /admins/** -> hasRole('SUPER_ADMIN')) and the method
 * layer, as defense in depth.
 */
@RestController
@RequestMapping("/admins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    @PostMapping
    public ResponseEntity<UserSummaryResponse> create(@Valid @RequestBody CreateAdminRequest request,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        UserSummaryResponse created = adminManagementService.createAdmin(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<UserSummaryResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(adminManagementService.listAdmins(pageable));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserSummaryResponse> setStatus(@PathVariable UUID id,
                                                           @RequestParam AccountStatus status,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adminManagementService.setAdminStatus(id, status, principal.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        adminManagementService.deleteAdmin(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
