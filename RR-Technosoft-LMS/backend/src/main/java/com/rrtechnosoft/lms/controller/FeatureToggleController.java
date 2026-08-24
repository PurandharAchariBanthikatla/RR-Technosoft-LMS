package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.UpdateFeatureToggleRequest;
import com.rrtechnosoft.lms.dto.response.FeatureToggleResponse;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.FeatureToggleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GET is open to any authenticated user (SecurityConfig) so both portals
 * can gate UI on flags; writes are SUPER_ADMIN only.
 */
@RestController
@RequestMapping("/administration/feature-toggles")
@RequiredArgsConstructor
public class FeatureToggleController {

    private final FeatureToggleService featureToggleService;

    @GetMapping
    public ResponseEntity<List<FeatureToggleResponse>> list() {
        return ResponseEntity.ok(featureToggleService.list());
    }

    @PatchMapping("/{featureKey}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<FeatureToggleResponse> update(@PathVariable String featureKey,
                                                          @Valid @RequestBody UpdateFeatureToggleRequest request,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(featureToggleService.update(featureKey, request, principal.getId()));
    }
}
