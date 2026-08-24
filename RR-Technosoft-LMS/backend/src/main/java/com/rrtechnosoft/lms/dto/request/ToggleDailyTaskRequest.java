package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.NotNull;

public record ToggleDailyTaskRequest(
        @NotNull Boolean completed
) {}
