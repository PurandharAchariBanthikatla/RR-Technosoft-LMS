package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** One installment line used both in fee-structure templates and per-student overrides. */
public record FeeInstallmentInput(
        @NotNull @Min(1) Integer installmentNumber,
        @NotNull BigDecimal amount,
        @NotNull @Min(0) Integer dueAfterDays
) {}
