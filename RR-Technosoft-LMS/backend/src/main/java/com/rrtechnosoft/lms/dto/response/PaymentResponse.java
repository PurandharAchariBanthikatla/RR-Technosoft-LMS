package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.Payment;
import com.rrtechnosoft.lms.entity.enums.PaymentGatewayProvider;
import com.rrtechnosoft.lms.entity.enums.PaymentMethod;
import com.rrtechnosoft.lms.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID studentFeeId,
        UUID installmentId,
        UUID studentId,
        String studentName,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        PaymentGatewayProvider gatewayProvider,
        String gatewayOrderId,
        String gatewayPaymentId,
        PaymentStatus status,
        String failureReason,
        BigDecimal refundedAmount,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getStudentFee().getId(),
                p.getInstallment() != null ? p.getInstallment().getId() : null,
                p.getStudent().getId(),
                p.getStudent().getFullName(),
                p.getAmount(), p.getCurrency(), p.getMethod(), p.getGatewayProvider(),
                p.getGatewayOrderId(), p.getGatewayPaymentId(), p.getStatus(), p.getFailureReason(),
                p.getRefundedAmount(), p.getPaidAt(), p.getCreatedAt()
        );
    }
}
