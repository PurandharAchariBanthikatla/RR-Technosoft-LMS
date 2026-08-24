package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.UpdateNotificationSettingsRequest;
import com.rrtechnosoft.lms.dto.response.NotificationSettingsResponse;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.NotificationSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/administration/notification-settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class NotificationSettingsController {

    private final NotificationSettingsService notificationSettingsService;

    @GetMapping
    public ResponseEntity<NotificationSettingsResponse> get() {
        return ResponseEntity.ok(notificationSettingsService.get());
    }

    @PutMapping
    public ResponseEntity<NotificationSettingsResponse> update(@Valid @RequestBody UpdateNotificationSettingsRequest request,
                                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationSettingsService.update(request, principal.getId()));
    }
}
