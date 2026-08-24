package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.UpdateSecuritySettingsRequest;
import com.rrtechnosoft.lms.dto.response.SecuritySettingsResponse;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.SecuritySettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/administration/security-settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SecuritySettingsController {

    private final SecuritySettingsService securitySettingsService;

    @GetMapping
    public ResponseEntity<SecuritySettingsResponse> get() {
        return ResponseEntity.ok(securitySettingsService.get());
    }

    @PutMapping
    public ResponseEntity<SecuritySettingsResponse> update(@Valid @RequestBody UpdateSecuritySettingsRequest request,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(securitySettingsService.update(request, principal.getId()));
    }
}
