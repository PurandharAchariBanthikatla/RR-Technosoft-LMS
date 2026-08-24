package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.response.FeeSummaryReportResponse;
import com.rrtechnosoft.lms.dto.response.StudentFeeResponse;
import com.rrtechnosoft.lms.entity.enums.FeeStatus;
import com.rrtechnosoft.lms.service.FinanceReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/finance/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class FinanceReportController {

    private final FinanceReportService financeReportService;

    @GetMapping("/summary")
    public ResponseEntity<FeeSummaryReportResponse> summary(@RequestParam(required = false) UUID courseId) {
        return ResponseEntity.ok(financeReportService.summary(courseId));
    }

    @GetMapping("/student-fees")
    public ResponseEntity<Page<StudentFeeResponse>> studentFees(@RequestParam(required = false) UUID courseId,
                                                                  @RequestParam(required = false) FeeStatus status,
                                                                  @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(financeReportService.studentFeeReport(courseId, status, pageable));
    }
}
