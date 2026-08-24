package com.rrtechnosoft.lms.payment;

import java.math.BigDecimal;

/**
 * Abstraction over the payment gateway so PaymentService never talks HTTP
 * directly. RAZORPAY is the only live implementation today; MANUAL payments
 * (cash/bank transfer/cheque recorded by an admin) never go through here.
 */
public interface PaymentGatewayService {

    GatewayOrder createOrder(BigDecimal amount, String currency, String receiptRef);

    /** Verifies the client-side checkout callback signature (order+payment id pair). */
    boolean verifyPaymentSignature(String orderId, String paymentId, String signature);

    /** Verifies the raw-body signature on an async webhook call. */
    boolean verifyWebhookSignature(String rawBody, String signatureHeader);

    GatewayRefund refund(String gatewayPaymentId, BigDecimal amount, String reason);

    String keyId();

    record GatewayOrder(String orderId, String currency, long amountInMinorUnits, String keyId) {}

    record GatewayRefund(String refundId, String status) {}
}
