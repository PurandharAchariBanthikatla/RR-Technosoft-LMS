package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.CreateAssignmentRequest;
import com.rrtechnosoft.lms.dto.request.GradeSubmissionRequest;
import com.rrtechnosoft.lms.dto.request.SubmitAssignmentRequest;
import com.rrtechnosoft.lms.dto.response.AssignmentResponse;
import com.rrtechnosoft.lms.entity.enums.UserRole;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.AssignmentService;
import com.rrtechnosoft.lms.service.AssignmentService.UserPrincipalView;
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

import java.util.UUID;

/**
 * GET / is viewer-aware: students see their own submission status/score
 * per assignment; admins see submittedCount/totalStudents instead (see
 * AssignmentResponse#forStudent / #forAdmin).
 */
@RestController
@RequestMapping("/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping
    public ResponseEntity<Page<AssignmentResponse>> list(@RequestParam(required = false) UUID courseId,
                                                           @AuthenticationPrincipal UserPrincipal principal,
                                                           @PageableDefault(size = 20) Pageable pageable) {
        boolean isStudent = principal.getUser().getRole() == UserRole.STUDENT;
        Page<AssignmentResponse> page = isStudent
                ? assignmentService.listForStudent(courseId, principal.getId(), pageable)
                : assignmentService.listForAdmin(courseId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssignmentResponse> get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        boolean isStudent = principal.getUser().getRole() == UserRole.STUDENT;
        return ResponseEntity.ok(assignmentService.get(id, new UserPrincipalView(principal.getId(), isStudent)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<AssignmentResponse> create(@Valid @RequestBody CreateAssignmentRequest request,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        AssignmentResponse created = assignmentService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AssignmentResponse> submit(@PathVariable UUID id,
                                                       @RequestBody SubmitAssignmentRequest request,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(assignmentService.submit(id, request, principal.getId()));
    }

    @PatchMapping("/{id}/submissions/{submissionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<AssignmentResponse> grade(@PathVariable UUID id,
                                                      @PathVariable UUID submissionId,
                                                      @Valid @RequestBody GradeSubmissionRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(assignmentService.grade(id, submissionId, request, principal.getId()));
    }
}
