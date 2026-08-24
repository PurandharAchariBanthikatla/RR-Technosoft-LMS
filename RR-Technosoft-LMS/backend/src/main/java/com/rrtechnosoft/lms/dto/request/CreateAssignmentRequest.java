package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateAssignmentRequest(
        @NotNull UUID courseId,
        UUID moduleId,
        @NotBlank @Size(min = 3, max = 200) String title,
        @NotBlank String description,
        String attachmentUrl,
        @NotNull @Min(1) Integer maxScore,
        @NotNull OffsetDateTime dueDate
) {}
