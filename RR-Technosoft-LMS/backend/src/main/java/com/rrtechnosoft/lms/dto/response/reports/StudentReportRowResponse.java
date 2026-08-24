package com.rrtechnosoft.lms.dto.response.reports;

import java.util.UUID;

/**
 * One row of the Student Report. batch/branch/college come from the
 * StudentProfile side table and are null if the student never onboarded one.
 */
public record StudentReportRowResponse(
        UUID id,
        String studentCode,
        String fullName,
        String email,
        String batch,
        String branch,
        String college,
        long coursesEnrolled,
        double avgProgressPercentage,
        double attendancePercentage,
        double avgAssignmentScore,
        long assignmentsSubmitted,
        long assignmentsPending
) {}
