package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePracticeSubmissionRequest(
        @NotNull java.util.UUID problemId,
        @NotBlank String language,
        @NotBlank String code
) {}
