package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record RecordManualPaymentRequest(
        @NotNull UUID studentFeeId,
        UUID installmentId,
        @NotNull @Positive BigDecimal amount,
        @NotNull PaymentMethod method,
        String note
) {}
