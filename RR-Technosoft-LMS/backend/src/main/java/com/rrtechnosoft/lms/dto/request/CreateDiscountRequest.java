package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateDiscountRequest(
        @NotNull DiscountType type,
        @NotNull @Positive BigDecimal value,
        @NotBlank String reason
) {}
