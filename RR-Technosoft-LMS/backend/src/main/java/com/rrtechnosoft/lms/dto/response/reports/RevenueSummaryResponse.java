package com.rrtechnosoft.lms.dto.response.reports;

import java.math.BigDecimal;
import java.util.List;

/** Summary KPIs shown above the Revenue Report table (GET /reports/revenue/summary). */
public record RevenueSummaryResponse(
        BigDecimal totalRevenue,
        long totalPaidEnrollments,
        BigDecimal averageOrderValue,
        List<TrendPointResponse> revenueTrend
) {}
