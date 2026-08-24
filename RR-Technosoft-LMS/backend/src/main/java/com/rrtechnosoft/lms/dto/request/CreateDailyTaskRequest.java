package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateDailyTaskRequest(
        UUID courseId,
        @NotBlank @Size(min = 3, max = 200) String title,
        @NotBlank String description,
        @NotNull LocalDate date
) {}
