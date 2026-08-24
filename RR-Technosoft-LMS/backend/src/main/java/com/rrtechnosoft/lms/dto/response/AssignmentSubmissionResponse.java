package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.AssignmentSubmission;
import com.rrtechnosoft.lms.entity.enums.SubmissionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AssignmentSubmissionResponse(
        UUID id,
        UUID assignmentId,
        UUID studentId,
        String studentName,
        String submissionUrl,
        String submissionText,
        SubmissionStatus status,
        Integer score,
        String feedback,
        OffsetDateTime submittedAt
) {
    public static AssignmentSubmissionResponse from(AssignmentSubmission s) {
        return new AssignmentSubmissionResponse(
                s.getId(), s.getAssignment().getId(), s.getStudent().getId(), s.getStudent().getFullName(),
                s.getSubmissionUrl(), s.getSubmissionText(), s.getStatus(), s.getScore(), s.getFeedback(), s.getSubmittedAt()
        );
    }
}
