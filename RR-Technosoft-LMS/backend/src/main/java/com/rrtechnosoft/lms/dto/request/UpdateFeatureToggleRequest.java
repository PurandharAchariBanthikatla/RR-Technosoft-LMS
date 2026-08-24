package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateFeatureToggleRequest(@NotNull Boolean enabled) {}
