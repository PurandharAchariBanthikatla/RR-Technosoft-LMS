package com.rrtechnosoft.lms.config;

import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.AccountStatus;
import com.rrtechnosoft.lms.entity.enums.UserRole;
import com.rrtechnosoft.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Bootstraps the single SUPER_ADMIN account on first run so there's always a
 * way in. Controlled by app.seed.enabled — set to false in production once
 * the real Super Admin has changed their password. Runs before
 * DemoDataSeeder (@Order 1 vs 2).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.seed.super-admin-email}")
    private String superAdminEmail;

    @Value("${app.seed.super-admin-password}")
    private String superAdminPassword;

    @Override
    public void run(String... args) {
        if (!seedEnabled) return;

        boolean superAdminExists = userRepository.countByRole(UserRole.SUPER_ADMIN) > 0;
        if (superAdminExists) return;

        User superAdmin = User.builder()
                .role(UserRole.SUPER_ADMIN)
                .email(superAdminEmail)
                .passwordHash(passwordEncoder.encode(superAdminPassword))
                .fullName("RR TECHNOSOFT Super Admin")
                .status(AccountStatus.ACTIVE)
                .build();
        userRepository.save(superAdmin);

        log.warn("Seeded Super Admin account [{}]. Log in and change the password immediately.", superAdminEmail);
    }
}
