package com.rrtechnosoft.lms.dto.response.reports;

import java.util.UUID;

/** One row of the Attendance Report — session/status counts for one course over the requested date range. */
public record AttendanceReportRowResponse(
        UUID courseId,
        String courseTitle,
        long sessionsHeld,
        long presentCount,
        long absentCount,
        long lateCount,
        long excusedCount,
        double attendancePercentage
) {}
