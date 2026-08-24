package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.CreateEnrollmentRequest;
import com.rrtechnosoft.lms.dto.request.UpdateEnrollmentStatusRequest;
import com.rrtechnosoft.lms.dto.response.EnrollmentResponse;
import com.rrtechnosoft.lms.entity.enums.EnrollmentStatus;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.EnrollmentService;
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
 * Admin-facing list/status-change is restricted; students can only enroll
 * themselves and read their own enrollments via /me — enforced here via
 * @PreAuthorize plus deriving studentId from the authenticated principal
 * rather than trusting a path/body value.
 */
@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Page<EnrollmentResponse>> list(@RequestParam(required = false) EnrollmentStatus status,
                                                           @RequestParam(required = false) UUID courseId,
                                                           @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(enrollmentService.listEnrollments(status, courseId, pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<List<EnrollmentResponse>> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(enrollmentService.myEnrollments(principal.getId()));
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EnrollmentResponse> enroll(@Valid @RequestBody CreateEnrollmentRequest request,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        EnrollmentResponse created = enrollmentService.enroll(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<EnrollmentResponse> updateStatus(@PathVariable UUID id,
                                                             @Valid @RequestBody UpdateEnrollmentStatusRequest request,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(enrollmentService.updateStatus(id, request.status(), principal.getId()));
    }
}
