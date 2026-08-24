package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.UpdateSystemSettingValueRequest;
import com.rrtechnosoft.lms.dto.request.UpsertSystemSettingRequest;
import com.rrtechnosoft.lms.dto.response.SystemSettingResponse;
import com.rrtechnosoft.lms.entity.enums.SettingCategory;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.SystemSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Restricted to SUPER_ADMIN at both the URL layer
 * (SecurityConfig: /system-settings/** -> hasRole('SUPER_ADMIN')) and here.
 */
@RestController
@RequestMapping("/system-settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    public ResponseEntity<List<SystemSettingResponse>> list(@RequestParam(required = false) SettingCategory category) {
        return ResponseEntity.ok(systemSettingService.list(category));
    }

    @PostMapping
    public ResponseEntity<SystemSettingResponse> create(@Valid @RequestBody UpsertSystemSettingRequest request,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(systemSettingService.create(request, principal.getId()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SystemSettingResponse> updateValue(@PathVariable UUID id,
                                                               @Valid @RequestBody UpdateSystemSettingValueRequest request,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(systemSettingService.updateValue(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        systemSettingService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
