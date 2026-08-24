package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GradeSubmissionRequest(
        @NotNull @Min(0) Integer score,
        String feedback
) {}
