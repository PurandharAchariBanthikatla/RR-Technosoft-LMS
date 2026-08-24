package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.UpdateBackupConfigRequest;
import com.rrtechnosoft.lms.dto.response.BackupConfigResponse;
import com.rrtechnosoft.lms.dto.response.BackupRunResponse;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.BackupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/administration/backup")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class BackupController {

    private final BackupService backupService;

    @GetMapping("/config")
    public ResponseEntity<BackupConfigResponse> getConfig() {
        return ResponseEntity.ok(backupService.getConfig());
    }

    @PutMapping("/config")
    public ResponseEntity<BackupConfigResponse> updateConfig(@Valid @RequestBody UpdateBackupConfigRequest request,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(backupService.updateConfig(request, principal.getId()));
    }

    @GetMapping("/runs")
    public ResponseEntity<Page<BackupRunResponse>> listRuns(Pageable pageable) {
        return ResponseEntity.ok(backupService.listRuns(pageable));
    }

    @PostMapping("/runs")
    public ResponseEntity<BackupRunResponse> trigger(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(backupService.triggerBackup(principal.getId()));
    }
}
