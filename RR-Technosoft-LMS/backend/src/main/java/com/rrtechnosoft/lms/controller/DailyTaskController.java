package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.CreateDailyTaskRequest;
import com.rrtechnosoft.lms.dto.request.ToggleDailyTaskRequest;
import com.rrtechnosoft.lms.dto.response.DailyTaskResponse;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.DailyTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Backs the student "Daily Tasks" screen (src/app/(student)/student/daily-tasks/page.tsx
 * -> lib/api/assignments.ts dailyTasksApi -> /daily-tasks) — DailyTaskService,
 * DailyTaskRepository/DailyTaskCompletionRepository and both request/response DTOs already
 * existed and were complete; only this controller was missing.
 *
 * GET is open to any authenticated user (mirrors AssignmentController's list endpoint);
 * completion state is resolved per the calling student. Only SUPER_ADMIN/ADMIN can create tasks.
 */
@RestController
@RequestMapping("/daily-tasks")
@RequiredArgsConstructor
public class DailyTaskController {

    private final DailyTaskService dailyTaskService;

    @GetMapping
    public ResponseEntity<List<DailyTaskResponse>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dailyTaskService.list(date, principal.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<DailyTaskResponse> create(@Valid @RequestBody CreateDailyTaskRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        DailyTaskResponse created = dailyTaskService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<DailyTaskResponse> toggle(@PathVariable UUID id,
                                                      @Valid @RequestBody ToggleDailyTaskRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dailyTaskService.toggle(id, request.completed(), principal.getId()));
    }
}
