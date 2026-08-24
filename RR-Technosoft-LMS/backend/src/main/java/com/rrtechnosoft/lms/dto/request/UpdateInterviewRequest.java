package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.InterviewMode;
import com.rrtechnosoft.lms.entity.enums.InterviewResult;
import com.rrtechnosoft.lms.entity.enums.InterviewStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record UpdateInterviewRequest(
        @NotBlank String roundName,
        @NotNull OffsetDateTime scheduledAt,
        @NotNull InterviewMode mode,
        String venueOrLink,
        String interviewerName,
        @NotNull InterviewStatus status,
        @NotNull InterviewResult result,
        String feedback
) {}
