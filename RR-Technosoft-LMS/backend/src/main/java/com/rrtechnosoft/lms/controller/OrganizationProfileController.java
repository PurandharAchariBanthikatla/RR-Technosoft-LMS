package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.UpdateOrganizationProfileRequest;
import com.rrtechnosoft.lms.dto.response.OrganizationProfileResponse;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.OrganizationProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * GET is public (SecurityConfig permits it) so the login screen can render
 * org branding before authentication; writes are SUPER_ADMIN only, enforced
 * both at the URL layer and here.
 */
@RestController
@RequestMapping("/administration/organization-profile")
@RequiredArgsConstructor
public class OrganizationProfileController {

    private final OrganizationProfileService organizationProfileService;

    @GetMapping
    public ResponseEntity<OrganizationProfileResponse> get() {
        return ResponseEntity.ok(organizationProfileService.get());
    }

    @PutMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<OrganizationProfileResponse> update(@Valid @RequestBody UpdateOrganizationProfileRequest request,
                                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(organizationProfileService.update(request, principal.getId()));
    }
}
