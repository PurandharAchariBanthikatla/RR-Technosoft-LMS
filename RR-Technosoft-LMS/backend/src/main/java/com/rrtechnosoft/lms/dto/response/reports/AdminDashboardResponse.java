package com.rrtechnosoft.lms.dto.response.reports;

import java.math.BigDecimal;
import java.util.List;

/**
 * Exact shape of the frontend's `AdminDashboardStats` type (src/types/index.ts)
 * — GET /dashboard/admin. Field names/shape are pinned to that contract, which
 * predates this backend implementation, so this stays separate from the
 * richer {@link DashboardAnalyticsResponse} used by the new Reports & Analytics
 * dashboard.
 */
public record AdminDashboardResponse(
        long totalStudents,
        long totalCourses,
        long activeEnrollments,
        BigDecimal totalRevenue,
        long upcomingLiveClasses,
        long pendingAssignments,
        List<MonthlyStudents> studentGrowth
) {
    public record MonthlyStudents(String month, long students) {}
}
