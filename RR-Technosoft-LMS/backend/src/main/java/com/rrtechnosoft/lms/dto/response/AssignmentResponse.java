package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.Assignment;
import com.rrtechnosoft.lms.entity.AssignmentSubmission;
import com.rrtechnosoft.lms.entity.enums.SubmissionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Field names pinned to the frontend `Assignment` type in src/types/index.ts.
 * This is a per-viewer projection: for a STUDENT, status/score reflect their
 * own submission (or NOT_SUBMITTED if none exists); for ADMIN/SUPER_ADMIN,
 * submittedCount/totalStudents are populated instead and status is null.
 */
public record AssignmentResponse(
        UUID id,
        UUID courseId,
        String courseTitle,
        String title,
        String description,
        OffsetDateTime dueDate,
        Integer maxScore,
        SubmissionStatus status,
        Integer score,
        Long submittedCount,
        Long totalStudents
) {
    public static AssignmentResponse forAdmin(Assignment a, long submittedCount, Long totalStudents) {
        return new AssignmentResponse(
                a.getId(),
                a.getCourse() != null ? a.getCourse().getId() : null,
                a.getCourse() != null ? a.getCourse().getTitle() : null,
                a.getTitle(), a.getInstructions(), a.getDueAt(), a.getMaxScore(),
                null, null, submittedCount, totalStudents
        );
    }

    public static AssignmentResponse forStudent(Assignment a, AssignmentSubmission submission) {
        return new AssignmentResponse(
                a.getId(),
                a.getCourse() != null ? a.getCourse().getId() : null,
                a.getCourse() != null ? a.getCourse().getTitle() : null,
                a.getTitle(), a.getInstructions(), a.getDueAt(), a.getMaxScore(),
                submission != null ? submission.getStatus() : SubmissionStatus.NOT_SUBMITTED,
                submission != null ? submission.getScore() : null,
                null, null
        );
    }
}
