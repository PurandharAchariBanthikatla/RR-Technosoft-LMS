package com.rrtechnosoft.lms.dto.response.reports;

import java.time.OffsetDateTime;
import java.util.UUID;

/** One row of the Assignment Report — submission/grading stats for one assignment. */
public record AssignmentReportRowResponse(
        UUID id,
        String assignmentTitle,
        String courseTitle,
        OffsetDateTime dueAt,
        long totalStudents,
        long submittedCount,
        long gradedCount,
        long lateCount,
        long pendingCount,
        double avgScore,
        double submissionRatePercentage
) {}
