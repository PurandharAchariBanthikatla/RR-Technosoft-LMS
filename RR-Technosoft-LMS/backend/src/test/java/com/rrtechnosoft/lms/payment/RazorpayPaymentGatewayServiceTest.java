package com.rrtechnosoft.lms.payment;

import com.rrtechnosoft.lms.config.RazorpayProperties;
import com.rrtechnosoft.lms.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Covers the two halves of RazorpayPaymentGatewayService separately:
 * signature verification (verifyPaymentSignature / verifyWebhookSignature)
 * is pure HMAC-SHA256 logic and is tested directly against known
 * input/secret/signature triples with no mocking needed — this is the
 * security-critical path (a bug here would mean forged payment
 * confirmations or webhook events are accepted), so it's worth pinning
 * down precisely. createOrder/refund need a mocked RestTemplate since they
 * make real HTTP calls to Razorpay when enabled.
 */
@ExtendWith(MockitoExtension.class)
class RazorpayPaymentGatewayServiceTest {

    private static final String KEY_SECRET = "test_key_secret";
    private static final String WEBHOOK_SECRET = "test_webhook_secret";

    @Mock
    private RestTemplate restTemplate;

    private RazorpayProperties properties;
    private RazorpayPaymentGatewayService gatewayService;

    @BeforeEach
    void setUp() {
        properties = new RazorpayProperties();
        properties.setEnabled(true);
        properties.setKeyId("test_key_id");
        properties.setKeySecret(KEY_SECRET);
        properties.setWebhookSecret(WEBHOOK_SECRET);
        properties.setBaseUrl("https://api.razorpay.com/v1");
        gatewayService = new RazorpayPaymentGatewayService(properties, restTemplate);
    }

    // --- signature verification -------------------------------------------------

    @Test
    void verifyPaymentSignature_acceptsACorrectlyComputedSignature() {
        String orderId = "order_ABC123";
        String paymentId = "pay_XYZ789";
        String validSignature = hmacSha256Hex(orderId + "|" + paymentId, KEY_SECRET);

        assertThat(gatewayService.verifyPaymentSignature(orderId, paymentId, validSignature)).isTrue();
    }

    @Test
    void verifyPaymentSignature_rejectsATamperedSignature() {
        String orderId = "order_ABC123";
        String paymentId = "pay_XYZ789";

        assertThat(gatewayService.verifyPaymentSignature(orderId, paymentId, "not-the-real-signature")).isFalse();
    }

    @Test
    void verifyPaymentSignature_rejectsASignatureComputedWithTheWrongSecret() {
        String orderId = "order_ABC123";
        String paymentId = "pay_XYZ789";
        String signatureFromWrongSecret = hmacSha256Hex(orderId + "|" + paymentId, "someone_elses_secret");

        assertThat(gatewayService.verifyPaymentSignature(orderId, paymentId, signatureFromWrongSecret)).isFalse();
    }

    @Test
    void verifyWebhookSignature_acceptsACorrectlyComputedSignature() {
        String rawBody = "{\"event\":\"payment.captured\"}";
        String validSignature = hmacSha256Hex(rawBody, WEBHOOK_SECRET);

        assertThat(gatewayService.verifyWebhookSignature(rawBody, validSignature)).isTrue();
    }

    @Test
    void verifyWebhookSignature_rejectsANullOrBlankHeader() {
        assertThat(gatewayService.verifyWebhookSignature("{}", null)).isFalse();
        assertThat(gatewayService.verifyWebhookSignature("{}", "")).isFalse();
        assertThat(gatewayService.verifyWebhookSignature("{}", "   ")).isFalse();
    }

    @Test
    void verifyWebhookSignature_rejectsATamperedBody() {
        String signature = hmacSha256Hex("{\"event\":\"payment.captured\"}", WEBHOOK_SECRET);

        assertThat(gatewayService.verifyWebhookSignature("{\"event\":\"payment.failed\"}", signature)).isFalse();
    }

    // --- createOrder --------------------------------------------------------

    @Test
    void createOrder_throwsWhenTheGatewayIsNotEnabled() {
        properties.setEnabled(false);

        assertThatThrownBy(() -> gatewayService.createOrder(BigDecimal.TEN, "INR", "receipt-1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void createOrder_convertsRupeesToPaiseAndReturnsTheGatewayOrderId() {
        String responseJson = "{\"id\":\"order_N4X9y2\",\"amount\":150000,\"currency\":\"INR\"}";
        when(restTemplate.exchange(
                eq("https://api.razorpay.com/v1/orders"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(responseJson));

        PaymentGatewayService.GatewayOrder order = gatewayService.createOrder(new BigDecimal("1500.00"), "INR", "receipt-1");

        assertThat(order.orderId()).isEqualTo("order_N4X9y2");
        assertThat(order.amountInMinorUnits()).isEqualTo(150000L);
        assertThat(order.currency()).isEqualTo("INR");
        assertThat(order.keyId()).isEqualTo("test_key_id");
    }

    @Test
    void createOrder_wrapsAGatewayHttpFailureInAFriendlyApiException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("connection refused"));

        assertThatThrownBy(() -> gatewayService.createOrder(BigDecimal.TEN, "INR", "receipt-1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unable to create payment order");
    }

    // --- refund ---------------------------------------------------------------

    @Test
    void refund_throwsWhenTheGatewayIsNotEnabled() {
        properties.setEnabled(false);

        assertThatThrownBy(() -> gatewayService.refund("pay_XYZ789", BigDecimal.TEN, "student request"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("manually");
    }

    @Test
    void refund_returnsTheGatewayRefundIdAndStatus() {
        String responseJson = "{\"id\":\"rfnd_ABC123\",\"status\":\"processed\"}";
        when(restTemplate.exchange(
                eq("https://api.razorpay.com/v1/payments/pay_XYZ789/refund"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(responseJson));

        PaymentGatewayService.GatewayRefund refund = gatewayService.refund("pay_XYZ789", new BigDecimal("250.00"), "student request");

        assertThat(refund.refundId()).isEqualTo("rfnd_ABC123");
        assertThat(refund.status()).isEqualTo("processed");
    }

    @Test
    void keyId_returnsTheConfiguredPublicKeyId() {
        assertThat(gatewayService.keyId()).isEqualTo("test_key_id");
    }

    private static String hmacSha256Hex(String data, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
