package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.StudentFee;
import com.rrtechnosoft.lms.entity.enums.FeeStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StudentFeeResponse(
        UUID id,
        UUID studentId,
        String studentName,
        String studentIdNumber,
        UUID courseId,
        String courseTitle,
        UUID feeStructureId,
        String feeStructureName,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal fineAmount,
        BigDecimal netPayable,
        BigDecimal amountPaid,
        BigDecimal balanceDue,
        String currency,
        FeeStatus status,
        List<StudentFeeInstallmentResponse> installments,
        OffsetDateTime createdAt
) {
    public static StudentFeeResponse from(StudentFee sf) {
        return new StudentFeeResponse(
                sf.getId(),
                sf.getStudent().getId(),
                sf.getStudent().getFullName(),
                sf.getStudent().getStudentId(),
                sf.getCourse() != null ? sf.getCourse().getId() : null,
                sf.getCourse() != null ? sf.getCourse().getTitle() : null,
                sf.getFeeStructure() != null ? sf.getFeeStructure().getId() : null,
                sf.getFeeStructure() != null ? sf.getFeeStructure().getName() : null,
                sf.getTotalAmount(), sf.getDiscountAmount(), sf.getFineAmount(),
                sf.getNetPayable(), sf.getAmountPaid(), sf.getBalanceDue(), sf.getCurrency(),
                sf.getStatus(),
                sf.getInstallments().stream().map(StudentFeeInstallmentResponse::from).toList(),
                sf.getCreatedAt()
        );
    }
}
