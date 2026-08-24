package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.InterviewMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

import java.time.OffsetDateTime;

public record CreateInterviewRequest(
        @Min(1) Integer roundNumber,
        @NotBlank String roundName,
        @NotNull OffsetDateTime scheduledAt,
        @NotNull InterviewMode mode,
        String venueOrLink,
        String interviewerName
) {}
