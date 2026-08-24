package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.Receipt;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReceiptResponse(
        UUID id,
        UUID paymentId,
        UUID studentFeeId,
        String receiptNumber,
        BigDecimal amount,
        OffsetDateTime issuedAt
) {
    public static ReceiptResponse from(Receipt r) {
        return new ReceiptResponse(
                r.getId(), r.getPayment().getId(), r.getStudentFee().getId(),
                r.getReceiptNumber(), r.getAmount(), r.getIssuedAt()
        );
    }
}
