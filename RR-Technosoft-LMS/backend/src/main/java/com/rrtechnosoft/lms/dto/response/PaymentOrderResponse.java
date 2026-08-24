package com.rrtechnosoft.lms.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/** Returned to the frontend so it can open the gateway checkout widget. */
public record PaymentOrderResponse(
        UUID paymentId,
        String gatewayOrderId,
        BigDecimal amount,
        String currency,
        String keyId,
        String provider
) {}
