package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GradeAssignmentRequest(
        @NotNull @Min(0) Integer score,
        String feedback
) {}
