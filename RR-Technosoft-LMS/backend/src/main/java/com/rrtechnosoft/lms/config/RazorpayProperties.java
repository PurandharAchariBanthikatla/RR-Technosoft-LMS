package com.rrtechnosoft.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds app.payment.razorpay.* (see application.yml / RAZORPAY_* env vars).
 * When `enabled` is false the gateway calls are short-circuited so local/dev
 * environments without live credentials can still exercise the manual-payment
 * and reporting flows.
 */
@Component
@ConfigurationProperties(prefix = "app.payment.razorpay")
@Getter
@Setter
public class RazorpayProperties {
    private boolean enabled = false;
    private String keyId = "";
    private String keySecret = "";
    private String webhookSecret = "";
    private String baseUrl = "https://api.razorpay.com/v1";
}
