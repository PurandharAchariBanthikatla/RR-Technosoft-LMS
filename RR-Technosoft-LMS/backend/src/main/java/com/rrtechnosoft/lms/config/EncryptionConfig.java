package com.rrtechnosoft.lms.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * Symmetric encryption for secrets we must store but never display back
 * in full (SMTP password on the Notification Settings screen). Backed by
 * app.security.encryption-secret / -salt, which must be set in production
 * env vars — the defaults here are dev-only, same pattern as JwtProperties.
 */
@Configuration
@RequiredArgsConstructor
public class EncryptionConfig {

    private final EncryptionProperties encryptionProperties;

    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.text(encryptionProperties.getSecret(), encryptionProperties.getSalt());
    }
}
