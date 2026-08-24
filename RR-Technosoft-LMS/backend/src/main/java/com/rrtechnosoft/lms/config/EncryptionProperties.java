package com.rrtechnosoft.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class EncryptionProperties {
    private String encryptionSecret = "CHANGE_THIS_ENCRYPTION_SECRET_IN_PRODUCTION_ENV_VARS";
    private String encryptionSalt = "a1b2c3d4e5f6";

    public String getSecret() {
        return encryptionSecret;
    }

    public String getSalt() {
        return encryptionSalt;
    }
}
