package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.FeeStructureInstallment;

import java.math.BigDecimal;
import java.util.UUID;

public record FeeStructureInstallmentResponse(
        UUID id,
        Integer installmentNumber,
        BigDecimal amount,
        Integer dueAfterDays
) {
    public static FeeStructureInstallmentResponse from(FeeStructureInstallment i) {
        return new FeeStructureInstallmentResponse(i.getId(), i.getInstallmentNumber(), i.getAmount(), i.getDueAfterDays());
    }
}
