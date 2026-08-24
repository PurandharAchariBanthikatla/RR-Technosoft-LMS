package com.rrtechnosoft.lms.config;

import com.rrtechnosoft.lms.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // enables @PreAuthorize("hasRole('SUPER_ADMIN')") on controllers/services
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtProperties jwtProperties;
    private final CorsProperties corsProperties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // Org branding (name/logo) needs to render on the public login screen.
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/administration/organization-profile").permitAll()

                // Public — certificate authenticity check via QR code scan or the
                // public /verify/[code] frontend page; must work unauthenticated.
                .requestMatchers("/certificates/verify/**").permitAll()

                // Public — Razorpay payment-gateway webhook. The gateway can't
                // authenticate as a user, so every event is instead verified via
                // HMAC signature against the raw body in PaymentController/
                // PaymentService before anything is trusted (see PaymentController).
                .requestMatchers("/finance/payments/webhook").permitAll()

                // Administration module — read-only lookups any authenticated user needs
                // (feature flags gate UI, master data backs dropdowns across the app).
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                  "/administration/feature-toggles/**", "/administration/master-data/**")
                    .authenticated()

                // Super Admin only
                .requestMatchers("/admins/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/system-settings/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/administration/**").hasRole("SUPER_ADMIN")

                // Admin + Super Admin
                // NOTE: "/courses/manage/**" was dead — the frontend and CourseController
                // both use plain "/courses" for writes, not "/courses/manage". Course
                // write access (POST/PUT/PATCH/DELETE) is enforced with method-level
                // @PreAuthorize on CourseController/LessonController instead, since GET
                // on the same paths must stay open to students (published catalog only).
                //
                // "/live-classes/**", "/attendance/**", and "/certificates/issue/**" were
                // removed from here for the same reason, but the other direction: they used
                // to blanket-restrict the whole path tree to ADMIN/SUPER_ADMIN, which — since
                // URL-level rules are matched before method-level @PreAuthorize — silently
                // 403'd every student-facing GET on those controllers (/attendance/me,
                // /attendance/me/summary, /live-classes/upcoming, the list endpoints) despite
                // those methods having no admin restriction of their own. Write access on both
                // is already enforced correctly via @PreAuthorize on the controllers; "issue"
                // was also just the wrong path (real one is POST /certificates).
                //
                // Same story for Reports & Analytics ("/reports/**") and Finance
                // ("/finance/**"): every endpoint in ReportsController, DashboardController,
                // FeeStructureController, FinanceReportController, PaymentController,
                // ReceiptController and StudentFeeController already carries its own
                // @PreAuthorize (admin-only, student-only, or a per-method mix), so no
                // blanket URL matcher is added for either — it would only risk the same
                // silent-403 bug for their student-facing endpoints (/finance/payments/me,
                // /finance/receipts/me, /finance/student-fees/me).
                .requestMatchers("/placements/manage/**",
                                  "/announcements/**", "/students/manage/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")

                // Students
                .requestMatchers("/student/**", "/practice/**", "/chatbot/**")
                    .hasRole("STUDENT")

                // Shared read endpoints (courses catalog, notifications) - any authenticated user
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.getAllowedOrigins()); // driven by app.cors.allowed-origins / CORS_ORIGINS env var
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
