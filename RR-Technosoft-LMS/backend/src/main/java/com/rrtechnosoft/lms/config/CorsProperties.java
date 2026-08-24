package com.rrtechnosoft.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Binds app.cors.allowed-origins (comma-separated in application.yml / CORS_ORIGINS env var).
 * Previously SecurityConfig hardcoded "http://localhost:3000" and silently ignored this
 * property, so CORS_ORIGINS in prod had no effect and the prod domain was never actually
 * allowed to call the API from a browser.
 */
@Component
@ConfigurationProperties(prefix = "app.cors")
@Getter
@Setter
public class CorsProperties {
    private List<String> allowedOrigins = List.of("http://localhost:3000");
}
