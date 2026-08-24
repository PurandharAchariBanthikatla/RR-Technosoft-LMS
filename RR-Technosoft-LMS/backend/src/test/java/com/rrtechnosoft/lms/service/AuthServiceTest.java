package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.LoginRequest;
import com.rrtechnosoft.lms.dto.response.AuthResponse;
import com.rrtechnosoft.lms.entity.RefreshToken;
import com.rrtechnosoft.lms.entity.SecuritySettings;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.AccountStatus;
import com.rrtechnosoft.lms.entity.enums.UserRole;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.RefreshTokenRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import com.rrtechnosoft.lms.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AuthService is the single most security-sensitive class in the backend —
 * it's the only place login attempts, account lockout, and refresh-token
 * rotation happen. Covers: email vs Student ID login-identifier detection,
 * wrong-password handling and the failed-attempt counter, account lockout
 * (both "already locked" and "this failure just crossed the threshold"),
 * disabled/suspended accounts, successful login resetting the counters, and
 * refresh-token rotation (old token revoked, new one issued, expired token
 * rejected).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuditLogService auditLogService;
    @Mock private SecuritySettingsService securitySettingsService;

    @InjectMocks
    private AuthService authService;

    private static final String IP = "203.0.113.10";
    private static final String USER_AGENT = "JUnit";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "maxLoginAttempts", 5);
        ReflectionTestUtils.setField(authService, "lockoutMinutes", 15);
    }

    private User activeStudent() {
        return User.builder()
                .id(UUID.randomUUID())
                .role(UserRole.STUDENT)
                .studentId("RRT2026S0001")
                .fullName("Asha Rao")
                .passwordHash("hashed")
                .status(AccountStatus.ACTIVE)
                .failedLoginCount(0)
                .build();
    }

    // --- identifier detection -------------------------------------------------

    @Test
    void login_looksUpByEmailWhenIdentifierContainsAtSign() {
        User admin = User.builder().id(UUID.randomUUID()).role(UserRole.ADMIN).email("admin@rrtechnosoft.com")
                .fullName("Admin One").passwordHash("hashed").status(AccountStatus.ACTIVE).failedLoginCount(0).build();
        when(userRepository.findByEmailIgnoreCase("admin@rrtechnosoft.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(admin)).thenReturn("access-token");
        when(jwtService.generateOpaqueRefreshToken()).thenReturn("raw-refresh-token");
        when(jwtService.hashToken("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(jwtService.getRefreshTokenExpiryMs()).thenReturn(604_800_000L);

        AuthResponse response = authService.login(new LoginRequest("admin@rrtechnosoft.com", "secret"), IP, USER_AGENT);

        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        verify(userRepository).findByEmailIgnoreCase("admin@rrtechnosoft.com");
        verify(userRepository, never()).findByStudentId(anyString());
    }

    @Test
    void login_looksUpByStudentIdWhenIdentifierHasNoAtSign() {
        User student = activeStudent();
        when(userRepository.findByStudentId("RRT2026S0001")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(student)).thenReturn("access-token");
        when(jwtService.generateOpaqueRefreshToken()).thenReturn("raw-refresh-token");
        when(jwtService.hashToken("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(jwtService.getRefreshTokenExpiryMs()).thenReturn(604_800_000L);

        // lowercase on the way in — the service uppercases before lookup
        authService.login(new LoginRequest("rrt2026s0001", "secret"), IP, USER_AGENT);

        verify(userRepository).findByStudentId("RRT2026S0001");
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void login_throwsUnauthorizedWhenNoUserMatchesTheIdentifier() {
        when(userRepository.findByStudentId("NOBODY")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("NOBODY", "secret"), IP, USER_AGENT))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid credentials");
    }

    // --- lockout / account status ----------------------------------------------

    @Test
    void login_throwsLockedWhenTheAccountIsCurrentlyLocked() {
        User student = activeStudent();
        student.setLockedUntil(OffsetDateTime.now().plusMinutes(10));
        when(userRepository.findByStudentId("RRT2026S0001")).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> authService.login(new LoginRequest("RRT2026S0001", "secret"), IP, USER_AGENT))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("locked");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_treatsAnExpiredLockAsUnlocked() {
        User student = activeStudent();
        student.setLockedUntil(OffsetDateTime.now().minusMinutes(1)); // lock window already passed
        when(userRepository.findByStudentId("RRT2026S0001")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(student)).thenReturn("access-token");
        when(jwtService.generateOpaqueRefreshToken()).thenReturn("raw-refresh-token");
        when(jwtService.hashToken("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(jwtService.getRefreshTokenExpiryMs()).thenReturn(604_800_000L);

        AuthResponse response = authService.login(new LoginRequest("RRT2026S0001", "secret"), IP, USER_AGENT);

        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void login_throwsForbiddenForANonActiveAccount() {
        User student = activeStudent();
        student.setStatus(AccountStatus.SUSPENDED);
        when(userRepository.findByStudentId("RRT2026S0001")).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> authService.login(new LoginRequest("RRT2026S0001", "secret"), IP, USER_AGENT))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("suspended");
    }

    // --- wrong password / failed-attempt counter --------------------------------

    @Test
    void login_incrementsTheFailedAttemptCounterOnAWrongPassword() {
        User student = activeStudent();
        when(userRepository.findByStudentId("RRT2026S0001")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        when(securitySettingsService.getEntity()).thenReturn(securitySettingsWith(5, 15));

        assertThatThrownBy(() -> authService.login(new LoginRequest("RRT2026S0001", "wrong"), IP, USER_AGENT))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid credentials");

        assertThat(student.getFailedLoginCount()).isEqualTo(1);
        assertThat(student.getLockedUntil()).isNull();
        verify(userRepository).save(student);
    }

    @Test
    void login_locksTheAccountOnceFailedAttemptsReachTheConfiguredMax() {
        User student = activeStudent();
        student.setFailedLoginCount(4); // one more failure will hit maxLoginAttempts=5
        when(userRepository.findByStudentId("RRT2026S0001")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        when(securitySettingsService.getEntity()).thenReturn(securitySettingsWith(5, 15));

        assertThatThrownBy(() -> authService.login(new LoginRequest("RRT2026S0001", "wrong"), IP, USER_AGENT))
                .isInstanceOf(ApiException.class);

        assertThat(student.getFailedLoginCount()).isEqualTo(5);
        assertThat(student.getLockedUntil()).isAfter(OffsetDateTime.now());
    }

    @Test
    void login_fallsBackToTheStaticPropertyWhenSecuritySettingsRowIsMissing() {
        User student = activeStudent();
        student.setFailedLoginCount(4);
        when(userRepository.findByStudentId("RRT2026S0001")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        when(securitySettingsService.getEntity()).thenThrow(ApiException.notFound("Security settings not configured"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("RRT2026S0001", "wrong"), IP, USER_AGENT))
                .isInstanceOf(ApiException.class);

        // falls back to the @Value-injected maxLoginAttempts=5 set in setUp()
        assertThat(student.getLockedUntil()).isNotNull();
    }

    // --- successful login side-effects ------------------------------------------

    @Test
    void login_resetsFailureCountersAndAuditsOnSuccess() {
        User student = activeStudent();
        student.setFailedLoginCount(3);
        when(userRepository.findByStudentId("RRT2026S0001")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(student)).thenReturn("access-token");
        when(jwtService.generateOpaqueRefreshToken()).thenReturn("raw-refresh-token");
        when(jwtService.hashToken("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(jwtService.getRefreshTokenExpiryMs()).thenReturn(604_800_000L);

        AuthResponse response = authService.login(new LoginRequest("RRT2026S0001", "secret"), IP, USER_AGENT);

        assertThat(student.getFailedLoginCount()).isZero();
        assertThat(student.getLockedUntil()).isNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("raw-refresh-token");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(auditLogService).log(eq(student.getId()), eq("LOGIN_SUCCESS"), eq("User"), eq(student.getId()), eq(IP));
    }

    // --- refresh ----------------------------------------------------------------

    @Test
    void refresh_rotatesTheTokenAndRevokesTheOldOne() {
        User student = activeStudent();
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(student)
                .tokenHash("old-hash")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .revoked(false)
                .build();
        when(jwtService.hashToken("raw-old-token")).thenReturn("old-hash");
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse("old-hash")).thenReturn(Optional.of(stored));
        when(jwtService.generateAccessToken(student)).thenReturn("new-access-token");
        when(jwtService.generateOpaqueRefreshToken()).thenReturn("raw-new-token");
        when(jwtService.hashToken("raw-new-token")).thenReturn("new-hash");
        when(jwtService.getRefreshTokenExpiryMs()).thenReturn(604_800_000L);

        AuthResponse response = authService.refresh("raw-old-token", IP, USER_AGENT);

        assertThat(stored.getRevoked()).isTrue();
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("raw-new-token");
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class)); // old revoked-save + new token save
    }

    @Test
    void refresh_throwsWhenTheTokenIsNotFound() {
        when(jwtService.hashToken("unknown")).thenReturn("unknown-hash");
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse("unknown-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("unknown", IP, USER_AGENT))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid or expired");
    }

    @Test
    void refresh_throwsWhenTheStoredTokenHasExpired() {
        User student = activeStudent();
        RefreshToken expired = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(student)
                .tokenHash("expired-hash")
                .expiresAt(OffsetDateTime.now().minusDays(1))
                .revoked(false)
                .build();
        when(jwtService.hashToken("raw-expired")).thenReturn("expired-hash");
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse("expired-hash")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh("raw-expired", IP, USER_AGENT))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("expired");
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    // --- logout -------------------------------------------------------------

    @Test
    void logout_revokesAllRefreshTokensAndAudits() {
        UUID userId = UUID.randomUUID();

        authService.logout(userId);

        verify(refreshTokenRepository).revokeAllForUser(userId);
        verify(auditLogService).log(eq(userId), eq("LOGOUT"), eq("User"), eq(userId), isNull());
    }

    private static SecuritySettings securitySettingsWith(int maxAttempts, int lockoutMinutes) {
        SecuritySettings settings = new SecuritySettings();
        settings.setMaxLoginAttempts(maxAttempts);
        settings.setLockoutDurationMinutes(lockoutMinutes);
        return settings;
    }
}
