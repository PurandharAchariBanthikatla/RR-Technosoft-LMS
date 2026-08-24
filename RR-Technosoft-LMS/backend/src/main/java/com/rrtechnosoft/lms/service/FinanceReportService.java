package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.response.FeeSummaryReportResponse;
import com.rrtechnosoft.lms.dto.response.StudentFeeResponse;
import com.rrtechnosoft.lms.entity.enums.FeeStatus;
import com.rrtechnosoft.lms.repository.StudentFeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinanceReportService {

    private final StudentFeeRepository studentFeeRepository;

    @Transactional(readOnly = true)
    public FeeSummaryReportResponse summary(UUID courseId) {
        var billed = studentFeeRepository.totalBilled(courseId);
        var collected = studentFeeRepository.totalCollected(courseId);
        return new FeeSummaryReportResponse(
                billed,
                collected,
                billed.subtract(collected).max(java.math.BigDecimal.ZERO),
                studentFeeRepository.countTotal(courseId),
                studentFeeRepository.countOverdue(courseId)
        );
    }

    /** Detailed, filterable per-student fee report — the backbone of the Student Fee Reports screen and CSV export. */
    @Transactional(readOnly = true)
    public Page<StudentFeeResponse> studentFeeReport(UUID courseId, FeeStatus status, Pageable pageable) {
        return studentFeeRepository.search(null, courseId, status, pageable).map(StudentFeeResponse::from);
    }
}
