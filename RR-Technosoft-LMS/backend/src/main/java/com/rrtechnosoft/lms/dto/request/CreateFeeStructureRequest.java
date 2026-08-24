package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateFeeStructureRequest(
        UUID courseId,
        @NotBlank @Size(min = 3, max = 150) String name,
        String description,
        @NotNull BigDecimal totalAmount,
        String currency,
        @NotNull @Valid List<FeeInstallmentInput> installments
) {}
