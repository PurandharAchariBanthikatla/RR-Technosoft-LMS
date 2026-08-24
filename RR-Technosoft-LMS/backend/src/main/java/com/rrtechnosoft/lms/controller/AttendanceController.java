package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.BulkMarkAttendanceRequest;
import com.rrtechnosoft.lms.dto.request.MarkAttendanceRequest;
import com.rrtechnosoft.lms.dto.response.AttendanceResponse;
import com.rrtechnosoft.lms.dto.response.AttendanceSummaryResponse;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/me")
    public ResponseEntity<List<AttendanceResponse>> mine(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(attendanceService.mine(principal.getId(), from, to));
    }

    @GetMapping("/me/summary")
    public ResponseEntity<AttendanceSummaryResponse> mySummary(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(attendanceService.summary(principal.getId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Page<AttendanceResponse>> list(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(attendanceService.list(courseId, date, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<AttendanceResponse> mark(@Valid @RequestBody MarkAttendanceRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        AttendanceResponse marked = attendanceService.mark(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(marked);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<List<AttendanceResponse>> bulkMark(@Valid @RequestBody BulkMarkAttendanceRequest request,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.bulkMark(request, principal.getId()));
    }
}
