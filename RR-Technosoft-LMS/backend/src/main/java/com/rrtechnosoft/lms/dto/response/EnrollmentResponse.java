package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.Enrollment;
import com.rrtechnosoft.lms.entity.enums.EnrollmentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Field names/shape are pinned to the frontend `Enrollment` type in
 * src/types/index.ts — keep them in sync if either side changes.
 */
public record EnrollmentResponse(
        UUID id,
        UUID courseId,
        String courseTitle,
        UUID studentId,
        String studentName,
        EnrollmentStatus status,
        double progress,
        OffsetDateTime enrolledAt
) {
    public static EnrollmentResponse from(Enrollment e) {
        return new EnrollmentResponse(
                e.getId(),
                e.getCourse().getId(),
                e.getCourse().getTitle(),
                e.getStudent().getId(),
                e.getStudent().getFullName(),
                e.getStatus(),
                e.getProgressPct() == null ? 0.0 : e.getProgressPct().doubleValue(),
                e.getEnrolledAt()
        );
    }
}
