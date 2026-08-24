package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateFineRequest(
        UUID installmentId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String reason
) {}
