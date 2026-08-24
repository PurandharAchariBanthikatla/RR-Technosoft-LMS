package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.*;
import com.rrtechnosoft.lms.dto.response.*;
import com.rrtechnosoft.lms.entity.enums.ApplicationStatus;
import com.rrtechnosoft.lms.entity.enums.PlacementStatus;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.InterviewScheduleService;
import com.rrtechnosoft.lms.service.PlacementApplicationService;
import com.rrtechnosoft.lms.service.PlacementDashboardService;
import com.rrtechnosoft.lms.service.PlacementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Placement module: Job Drives (this controller's primary resource),
 * Student Applications and Interview Tracking (nested under a drive/
 * application), and the Placement Dashboard aggregate. Kept as one
 * controller — matching the frontend's single `/placements` API surface
 * (see src/lib/api/placements.ts and API_ROUTES.placements) — rather than
 * splitting into separate top-level routes that don't exist in that
 * contract.
 */
@RestController
@RequestMapping("/placements")
@RequiredArgsConstructor
public class PlacementController {

    private final PlacementService placementService;
    private final PlacementApplicationService applicationService;
    private final InterviewScheduleService interviewScheduleService;
    private final PlacementDashboardService dashboardService;

    // ------------------------------------------------------------ Job Drives

    @GetMapping
    public ResponseEntity<Page<PlacementResponse>> list(@RequestParam(required = false) String search,
                                                          @RequestParam(required = false) PlacementStatus status,
                                                          @RequestParam(required = false) UUID companyId,
                                                          @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(placementService.list(search, status, companyId, pageable));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<PlacementDashboardResponse> dashboard() {
        return ResponseEntity.ok(dashboardService.summary());
    }

    @GetMapping("/my-applications")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<PlacementApplicationResponse>> myApplications(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(applicationService.listForStudent(principal.getId(), pageable));
    }

    @GetMapping("/my-interviews")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<InterviewScheduleResponse>> myInterviews(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(interviewScheduleService.listForStudent(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlacementResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(placementService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<PlacementResponse> create(@Valid @RequestBody CreatePlacementRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(placementService.create(request, principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<PlacementResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody UpdatePlacementRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(placementService.update(id, request, principal.getId()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<PlacementResponse> setStatus(@PathVariable UUID id,
                                                          @RequestParam PlacementStatus status,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(placementService.setStatus(id, status, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        placementService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    // --------------------------------------------------------- Applications

    @PostMapping("/{id}/apply")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<PlacementApplicationResponse> apply(@PathVariable UUID id,
                                                                 @RequestBody(required = false) ApplyPlacementRequest request,
                                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.apply(id, request, principal.getId()));
    }

    @GetMapping("/{id}/applications")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Page<PlacementApplicationResponse>> listApplications(
            @PathVariable UUID id,
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(applicationService.listForPlacement(id, status, pageable));
    }

    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<PlacementApplicationResponse> getApplication(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(applicationService.get(applicationId));
    }

    @PatchMapping("/applications/{applicationId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<PlacementApplicationResponse> updateApplicationStatus(
            @PathVariable UUID applicationId,
            @Valid @RequestBody UpdateApplicationStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(applicationService.updateStatus(applicationId, request, principal.getId()));
    }

    @PostMapping("/applications/{applicationId}/withdraw")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> withdraw(@PathVariable UUID applicationId, @AuthenticationPrincipal UserPrincipal principal) {
        applicationService.withdraw(applicationId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/applications/{applicationId}/resume", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<PlacementApplicationResponse> attachResume(
            @PathVariable UUID applicationId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(applicationService.attachResume(applicationId, file, principal.getId()));
    }

    // ----------------------------------------------------------- Interviews

    @GetMapping("/applications/{applicationId}/interviews")
    public ResponseEntity<List<InterviewScheduleResponse>> listInterviews(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(interviewScheduleService.listForApplication(applicationId));
    }

    @PostMapping("/applications/{applicationId}/interviews")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<InterviewScheduleResponse> scheduleInterview(
            @PathVariable UUID applicationId,
            @Valid @RequestBody CreateInterviewRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewScheduleService.schedule(applicationId, request, principal.getId()));
    }

    @PutMapping("/interviews/{interviewId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<InterviewScheduleResponse> updateInterview(
            @PathVariable UUID interviewId,
            @Valid @RequestBody UpdateInterviewRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(interviewScheduleService.update(interviewId, request, principal.getId()));
    }

    @DeleteMapping("/interviews/{interviewId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> deleteInterview(@PathVariable UUID interviewId, @AuthenticationPrincipal UserPrincipal principal) {
        interviewScheduleService.delete(interviewId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
