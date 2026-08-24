package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.PaymentRefund;
import com.rrtechnosoft.lms.entity.enums.RefundStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RefundResponse(
        UUID id,
        UUID paymentId,
        BigDecimal amount,
        String reason,
        RefundStatus status,
        String gatewayRefundId,
        OffsetDateTime requestedAt,
        OffsetDateTime processedAt
) {
    public static RefundResponse from(PaymentRefund r) {
        return new RefundResponse(
                r.getId(), r.getPayment().getId(), r.getAmount(), r.getReason(), r.getStatus(),
                r.getGatewayRefundId(), r.getRequestedAt(), r.getProcessedAt()
        );
    }
}
