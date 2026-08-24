package com.rrtechnosoft.lms.dto.response;

import java.math.BigDecimal;

public record FeeSummaryReportResponse(
        BigDecimal totalBilled,
        BigDecimal totalCollected,
        BigDecimal totalOutstanding,
        long totalStudentFees,
        long overdueCount
) {}
