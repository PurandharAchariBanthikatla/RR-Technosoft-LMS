package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Assigns a fee to a student. Either feeStructureId (copies the template's
 * amount/installments) or a fully custom totalAmount + installments list
 * must be supplied.
 */
public record AssignFeeStructureRequest(
        @NotNull UUID studentId,
        UUID courseId,
        UUID feeStructureId,
        BigDecimal totalAmount,
        @NotNull LocalDate startDate,
        @Valid List<FeeInstallmentInput> installmentOverrides
) {}
