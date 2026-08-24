package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.PracticeSubmission;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PracticeSubmissionResponse(
        UUID id,
        UUID problemId,
        String language,
        boolean isCorrect,
        Integer runtimeMs,
        OffsetDateTime submittedAt
) {
    public static PracticeSubmissionResponse from(PracticeSubmission s) {
        return new PracticeSubmissionResponse(
                s.getId(),
                s.getProblem().getId(),
                s.getLanguage(),
                Boolean.TRUE.equals(s.getIsCorrect()),
                s.getRuntimeMs(),
                s.getSubmittedAt()
        );
    }
}
