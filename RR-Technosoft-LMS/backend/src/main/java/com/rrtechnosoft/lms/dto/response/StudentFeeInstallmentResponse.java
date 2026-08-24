package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.StudentFeeInstallment;
import com.rrtechnosoft.lms.entity.enums.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentFeeInstallmentResponse(
        UUID id,
        Integer installmentNumber,
        BigDecimal amount,
        LocalDate dueDate,
        BigDecimal paidAmount,
        BigDecimal balanceDue,
        InstallmentStatus status,
        OffsetDateTime paidAt
) {
    public static StudentFeeInstallmentResponse from(StudentFeeInstallment i) {
        return new StudentFeeInstallmentResponse(
                i.getId(), i.getInstallmentNumber(), i.getAmount(), i.getDueDate(),
                i.getPaidAmount(), i.getBalanceDue(), i.getStatus(), i.getPaidAt()
        );
    }
}
