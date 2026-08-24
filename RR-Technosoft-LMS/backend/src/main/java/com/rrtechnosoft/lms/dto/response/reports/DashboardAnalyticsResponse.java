package com.rrtechnosoft.lms.dto.response.reports;

import java.math.BigDecimal;
import java.util.List;

/**
 * Backs both the existing admin dashboard widgets (totalStudents..studentGrowth,
 * field names pinned to the frontend AdminDashboardStats type) and the richer
 * Reports & Analytics dashboard (everything from revenueTrend onward).
 */
public record DashboardAnalyticsResponse(
        long totalStudents,
        long totalCourses,
        long activeEnrollments,
        BigDecimal totalRevenue,
        long upcomingLiveClasses,
        long pendingAssignments,
        List<TrendPointResponse> studentGrowth,

        // Extended KPIs for the dedicated Reports & Analytics dashboard.
        long totalFaculty,
        double avgAttendancePercentage,
        double avgCourseCompletionPercentage,
        BigDecimal averageRevenuePerStudent,
        List<TrendPointResponse> revenueTrend,
        List<TrendPointResponse> attendanceTrend,
        List<CourseDistributionResponse> courseDistribution
) {}
