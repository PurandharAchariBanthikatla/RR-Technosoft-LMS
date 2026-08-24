package com.rrtechnosoft.lms.dto.response.reports;

import java.math.BigDecimal;

/**
 * One row of the Faculty Report, grouped by Course.instructorName — there is
 * no dedicated Faculty entity/role (see ReportsService javadoc), so
 * "faculty" here means a distinct instructor name found on Course.
 */
public record FacultyReportRowResponse(
        String instructorName,
        long coursesHandled,
        long totalStudents,
        double avgCourseRating,
        double avgCompletionPercentage,
        BigDecimal revenueGenerated
) {}
