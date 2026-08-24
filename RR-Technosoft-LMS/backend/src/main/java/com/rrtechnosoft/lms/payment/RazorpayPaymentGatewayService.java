package com.rrtechnosoft.lms.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rrtechnosoft.lms.config.RazorpayProperties;
import com.rrtechnosoft.lms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Razorpay Orders API integration (https://razorpay.com/docs/api/orders/).
 * Order creation and refunds are real HTTP calls signed with Basic Auth
 * (key_id:key_secret); signature verification uses HMAC-SHA256 exactly as
 * Razorpay's checkout.js and webhook docs specify. When app.payment.razorpay.enabled
 * is false (no live credentials configured), order creation fails fast with a
 * clear error instead of silently returning fake data — callers should steer
 * students to a manual/offline payment in that environment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayPaymentGatewayService implements PaymentGatewayService {

    private final RazorpayProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public GatewayOrder createOrder(BigDecimal amount, String currency, String receiptRef) {
        if (!properties.isEnabled()) {
            throw ApiException.badRequest("Online payment gateway is not configured. Use a manual payment instead.");
        }
        long minorUnits = amount.setScale(2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).longValueExact();

        HttpHeaders headers = authHeaders();
        Map<String, Object> body = Map.of(
                "amount", minorUnits,
                "currency", currency,
                "receipt", receiptRef,
                "payment_capture", 1
        );

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getBaseUrl() + "/orders",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );
            JsonNode json = objectMapper.readTree(response.getBody());
            String orderId = json.get("id").asText();
            return new GatewayOrder(orderId, currency, minorUnits, properties.getKeyId());
        } catch (RestClientException | java.io.IOException e) {
            log.error("Razorpay order creation failed", e);
            throw ApiException.badRequest("Unable to create payment order with the gateway. Please try again.");
        }
    }

    @Override
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        String payload = orderId + "|" + paymentId;
        String expected = hmacSha256Hex(payload, properties.getKeySecret());
        return constantTimeEquals(expected, signature);
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) return false;
        String expected = hmacSha256Hex(rawBody, properties.getWebhookSecret());
        return constantTimeEquals(expected, signatureHeader);
    }

    @Override
    public GatewayRefund refund(String gatewayPaymentId, BigDecimal amount, String reason) {
        if (!properties.isEnabled()) {
            throw ApiException.badRequest("Online payment gateway is not configured; refund must be processed manually.");
        }
        long minorUnits = amount.setScale(2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).longValueExact();
        HttpHeaders headers = authHeaders();
        Map<String, Object> body = Map.of(
                "amount", minorUnits,
                "notes", Map.of("reason", reason == null ? "" : reason)
        );
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getBaseUrl() + "/payments/" + gatewayPaymentId + "/refund",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );
            JsonNode json = objectMapper.readTree(response.getBody());
            return new GatewayRefund(json.get("id").asText(), json.path("status").asText("processed"));
        } catch (RestClientException | java.io.IOException e) {
            log.error("Razorpay refund failed for payment {}", gatewayPaymentId, e);
            throw ApiException.badRequest("Unable to process refund with the gateway. Please try again.");
        }
    }

    @Override
    public String keyId() {
        return properties.getKeyId();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String credentials = properties.getKeyId() + ":" + properties.getKeySecret();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        return headers;
    }

    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute HMAC signature", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    /** Utility for callers that need an idempotency/receipt reference. */
    public static String newReceiptRef() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }
}
