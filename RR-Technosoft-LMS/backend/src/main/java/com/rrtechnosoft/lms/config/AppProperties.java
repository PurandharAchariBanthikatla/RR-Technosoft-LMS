package com.rrtechnosoft.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** General app-level settings that don't warrant their own properties class. */
@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {
    /** Public base URL of the deployed frontend — used to build the link a certificate's QR code points to. */
    private String frontendBaseUrl;

    /** From: address/name for outbound notification emails. */
    private String mailFromAddress;
    private String mailFromName;
}
