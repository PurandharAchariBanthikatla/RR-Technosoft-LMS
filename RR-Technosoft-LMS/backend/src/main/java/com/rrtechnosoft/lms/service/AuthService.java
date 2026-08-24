package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.LoginRequest;
import com.rrtechnosoft.lms.dto.response.AuthResponse;
import com.rrtechnosoft.lms.entity.RefreshToken;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.AccountStatus;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.RefreshTokenRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import com.rrtechnosoft.lms.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    private final SecuritySettingsService securitySettingsService;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.lockout-duration-minutes:15}")
    private int lockoutMinutes;

    /**
     * Reads the live Administration > Security Settings row when present so a
     * Super Admin's policy change applies immediately; falls back to the
     * static app.security.* properties if that row is ever missing (e.g. a
     * fresh DB where V7 hasn't run yet).
     */
    private int resolveMaxLoginAttempts() {
        try {
            return securitySettingsService.getEntity().getMaxLoginAttempts();
        } catch (ApiException ex) {
            return maxLoginAttempts;
        }
    }

    private int resolveLockoutMinutes() {
        try {
            return securitySettingsService.getEntity().getLockoutDurationMinutes();
        } catch (ApiException ex) {
            return lockoutMinutes;
        }
    }

    /**
     * Admins log in with email; students log in with Student ID. We detect which
     * by checking for '@' in the identifier rather than forcing the client to
     * specify a login type.
     */
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        boolean looksLikeEmail = request.identifier().contains("@");

        Optional<User> userOpt = looksLikeEmail
                ? userRepository.findByEmailIgnoreCase(request.identifier())
                : userRepository.findByStudentId(request.identifier().trim().toUpperCase());

        User user = userOpt.orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));

        if (user.isLocked()) {
            throw ApiException.locked("Account locked due to repeated failed attempts. Try again later.");
        }
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw ApiException.forbidden("Account is " + user.getStatus().name().toLowerCase() + ". Contact your administrator.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            throw ApiException.unauthorized("Invalid credentials");
        }

        // successful login — reset failure counters
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateOpaqueRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(jwtService.hashToken(rawRefreshToken))
                .expiresAt(OffsetDateTime.now().plusSeconds(jwtService.getRefreshTokenExpiryMs() / 1000))
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();
        refreshTokenRepository.save(refreshToken);

        auditLogService.log(user.getId(), "LOGIN_SUCCESS", "User", user.getId(), ipAddress);

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                user.getId(),
                user.getRole(),
                user.getFullName(),
                user.getEmail(),
                user.getStudentId()
        );
    }

    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(attempts);
        if (attempts >= resolveMaxLoginAttempts()) {
            user.setLockedUntil(OffsetDateTime.now().plusMinutes(resolveLockoutMinutes()));
        }
        userRepository.save(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken, String ipAddress, String userAgent) {
        String hash = jwtService.hashToken(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> ApiException.unauthorized("Invalid or expired refresh token"));

        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw ApiException.unauthorized("Refresh token expired, please log in again");
        }

        User user = stored.getUser();

        // rotate: revoke old, issue new
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRawRefreshToken = jwtService.generateOpaqueRefreshToken();

        RefreshToken newToken = RefreshToken.builder()
                .user(user)
                .tokenHash(jwtService.hashToken(newRawRefreshToken))
                .expiresAt(OffsetDateTime.now().plusSeconds(jwtService.getRefreshTokenExpiryMs() / 1000))
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();
        refreshTokenRepository.save(newToken);

        return new AuthResponse(
                newAccessToken, newRawRefreshToken, user.getId(), user.getRole(),
                user.getFullName(), user.getEmail(), user.getStudentId()
        );
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId);
        auditLogService.log(userId, "LOGOUT", "User", userId, null);
    }
}
