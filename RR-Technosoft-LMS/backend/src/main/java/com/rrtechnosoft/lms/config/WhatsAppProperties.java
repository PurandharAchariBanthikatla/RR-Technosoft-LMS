package com.rrtechnosoft.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Twilio WhatsApp Business API credentials. Unset by default — the channel
 * no-ops (logs + skips) rather than failing when these are blank, since a
 * missing third-party credential shouldn't break in-app notifications or
 * email delivery. Populate via env vars to actually send.
 */
@Component
@ConfigurationProperties(prefix = "app.whatsapp")
@Getter
@Setter
public class WhatsAppProperties {
    private boolean enabled;
    private String accountSid;
    private String authToken;
    /** Twilio WhatsApp sender number, e.g. "+14155238886" (no "whatsapp:" prefix). */
    private String fromNumber;
}
