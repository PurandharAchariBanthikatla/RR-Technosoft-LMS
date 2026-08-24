package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiatePaymentRequest(
        @NotNull UUID studentFeeId,
        UUID installmentId,
        @NotNull @Positive BigDecimal amount
) {}
