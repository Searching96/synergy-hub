package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.entity.UserSession;
import com.synergyhub.dto.mapper.UserMapper;
import com.synergyhub.dto.request.LoginRequest;
import com.synergyhub.dto.response.LoginResponse;
import com.synergyhub.exception.AccountLockedException;
import com.synergyhub.exception.TwoFactorAuthenticationException;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.repository.UserSessionRepository;
import com.synergyhub.security.JwtTokenProvider;
import com.synergyhub.service.security.AccountLockService;
import com.synergyhub.service.security.AuditLogService;
import com.synergyhub.service.security.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private AccountLockService accountLockService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private TwoFactorAuthService twoFactorAuthService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private LoginService loginService;

    private User testUser;
    private LoginRequest loginRequest;
    private static final String TEST_IP = "192.168.1.1";  // ✅ Added constant
    private static final String TEST_USER_AGENT = "Mozilla/5.0 Test";  // ✅ Added constant

    @BeforeEach
    void setUp() {
        Organization testOrganization = Organization.builder()
                .id(1)
                .name("Test Org")
                .build();

        testUser = User.builder()
                .id(1)
                .email("test@example.com")
                .name("Test User")
                .passwordHash("$2a$10$hashedPassword")
                .organization(testOrganization)
                .emailVerified(true)
                .twoFactorEnabled(false)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .roles(new HashSet<>())
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();
    }

    @Test
    void login_WithValidCredentials_ShouldReturnLoginResponse() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(userRepository.findByEmailWithRolesAndPermissions(loginRequest.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(accountLockService.isAccountLocked(testUser)).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateTokenFromUserId(testUser.getId(), testUser.getEmail()))
                .thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(jwtTokenProvider.getTokenIdFromToken(anyString())).thenReturn("token-id-123");

        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);

        // When
        LoginResponse response = loginService.login(loginRequest, TEST_IP, TEST_USER_AGENT);  // ✅ Use constants

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.isTwoFactorRequired()).isFalse();

        verify(loginAttemptService).recordLoginAttempt(
                eq(loginRequest.getEmail()),
                eq(TEST_IP),
                eq(true)
        );
        verify(accountLockService).resetFailedAttempts(eq(testUser), eq(TEST_IP));  // ✅ Use eq()
        verify(userRepository).save(testUser);

        verify(userSessionRepository).save(sessionCaptor.capture());
        UserSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getUser()).isEqualTo(testUser);
        assertThat(savedSession.getTokenId()).isEqualTo("token-id-123");
        assertThat(savedSession.getIpAddress()).isEqualTo(TEST_IP);
        assertThat(savedSession.getUserAgent()).isEqualTo(TEST_USER_AGENT);

        verify(auditLogService).logLoginSuccess(
                eq(testUser),
                eq(TEST_IP),
                eq(TEST_USER_AGENT)
        );
    }

    @Test
    void login_WithInvalidCredentials_ShouldThrowBadCredentialsException() {
        // Given
        when(userRepository.findByEmailWithRolesAndPermissions(loginRequest.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(accountLockService.isAccountLocked(testUser)).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        assertThatThrownBy(() -> loginService.login(loginRequest, TEST_IP, TEST_USER_AGENT))  // ✅ Use constants
                .isInstanceOf(BadCredentialsException.class);

        verify(loginAttemptService).recordLoginAttempt(
                eq(loginRequest.getEmail()),
                eq(TEST_IP),
                eq(false)
        );
        verify(accountLockService).handleFailedLogin(eq(testUser), eq(TEST_IP));  // ✅ Use eq()
        verify(auditLogService).logLoginFailed(
                eq(loginRequest.getEmail()),
                eq(TEST_IP),
                eq(TEST_USER_AGENT),
                contains("Invalid credentials")  // ✅ Use contains() matcher
        );
    }

    @Test
    void login_WithLockedAccount_ShouldThrowAccountLockedException() {
        // Given
        testUser.setAccountLocked(true);
        testUser.setLockUntil(LocalDateTime.now().plusMinutes(30));

        when(userRepository.findByEmailWithRolesAndPermissions(loginRequest.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(accountLockService.isAccountLocked(testUser)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> loginService.login(loginRequest, TEST_IP, TEST_USER_AGENT))  // ✅ Use constants
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("Account is locked");

        verify(authenticationManager, never()).authenticate(any());
        verify(auditLogService).logLoginFailed(
                eq(loginRequest.getEmail()),
                eq(TEST_IP),
                eq(TEST_USER_AGENT),
                contains("locked")
        );
    }

    @Test
    void login_With2FAEnabled_ShouldReturnTemporaryToken() {
        // Given
        testUser.setTwoFactorEnabled(true);
        Authentication authentication = mock(Authentication.class);

        when(userRepository.findByEmailWithRolesAndPermissions(loginRequest.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(accountLockService.isAccountLocked(testUser)).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateTemporaryToken(testUser.getId(), testUser.getEmail()))
                .thenReturn("temp-token");

        // When
        LoginResponse response = loginService.login(loginRequest, TEST_IP, TEST_USER_AGENT);  // ✅ Use constants

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isTwoFactorRequired()).isTrue();
        assertThat(response.getTwoFactorToken()).isEqualTo("temp-token");
        assertThat(response.getAccessToken()).isNull();

        verify(userSessionRepository, never()).save(any());
        verify(auditLogService).logTwoFactorRequired(eq(testUser), eq(TEST_IP));  // ✅ Use eq()
    }

    @Test
    void login_With2FACode_ShouldValidateAndReturnToken() {
        // Given
        testUser.setTwoFactorEnabled(true);
        loginRequest.setTwoFactorCode("123456");
        Authentication authentication = mock(Authentication.class);

        when(userRepository.findByEmailWithRolesAndPermissions(loginRequest.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(accountLockService.isAccountLocked(testUser)).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(twoFactorAuthService.verifyCode(eq(testUser), eq("123456"), eq(TEST_IP)))  // ✅ Use eq()
                .thenReturn(true);
        when(jwtTokenProvider.generateTokenFromUserId(testUser.getId(), testUser.getEmail()))
                .thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(jwtTokenProvider.getTokenIdFromToken(anyString())).thenReturn("token-id-123");

        // When
        LoginResponse response = loginService.login(loginRequest, TEST_IP, TEST_USER_AGENT);  // ✅ Use constants

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.isTwoFactorRequired()).isFalse();

        verify(twoFactorAuthService).verifyCode(eq(testUser), eq("123456"), eq(TEST_IP));  // ✅ Use eq()
        verify(auditLogService).logTwoFactorSuccess(eq(testUser), eq(TEST_IP));  // ✅ Use eq()
    }

    @Test
    void login_WithInvalid2FACode_ShouldThrowException() {
        // Given
        testUser.setTwoFactorEnabled(true);
        loginRequest.setTwoFactorCode("000000");
        Authentication authentication = mock(Authentication.class);

        when(userRepository.findByEmailWithRolesAndPermissions(loginRequest.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(accountLockService.isAccountLocked(testUser)).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(twoFactorAuthService.verifyCode(eq(testUser), eq("000000"), eq(TEST_IP)))  // ✅ Use eq()
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> loginService.login(loginRequest, TEST_IP, TEST_USER_AGENT))  // ✅ Use constants
                .isInstanceOf(TwoFactorAuthenticationException.class)
                .hasMessageContaining("Invalid");

        verify(auditLogService).logTwoFactorFailed(eq(testUser), eq(TEST_IP));  // ✅ Use eq()
    }

    // ✅ Additional tests for better coverage
    @Test
    void login_WithNonExistentUser_ShouldThrowException() {
        // Given
        when(userRepository.findByEmailWithRolesAndPermissions(loginRequest.getEmail()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> loginService.login(loginRequest, TEST_IP, TEST_USER_AGENT))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid credentials");

        verify(authenticationManager, never()).authenticate(any());
        verify(auditLogService).logLoginFailed(
                eq(loginRequest.getEmail()),
                eq(TEST_IP),
                eq(TEST_USER_AGENT),
                contains("not found")
        );
    }

    @Test
    void login_WithUnverifiedEmail_ShouldThrowException() {
        // Given
        testUser.setEmailVerified(false);

        when(userRepository.findByEmailWithRolesAndPermissions(loginRequest.getEmail()))
                .thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> loginService.login(loginRequest, TEST_IP, TEST_USER_AGENT))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Email not verified");

        verify(authenticationManager, never()).authenticate(any());
        verify(auditLogService).logLoginFailed(
                eq(loginRequest.getEmail()),
                eq(TEST_IP),
                eq(TEST_USER_AGENT),
                contains("Email not verified")
        );
    }

    @Test
    void login_ShouldUpdateLastLoginTimestamp() {
        // Given
        Authentication authentication = mock(Authentication.class);
        LocalDateTime beforeLogin = LocalDateTime.now();

        when(userRepository.findByEmailWithRolesAndPermissions(loginRequest.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(accountLockService.isAccountLocked(testUser)).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateTokenFromUserId(testUser.getId(), testUser.getEmail()))
                .thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(jwtTokenProvider.getTokenIdFromToken(anyString())).thenReturn("token-id-123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // When
        loginService.login(loginRequest, TEST_IP, TEST_USER_AGENT);

        LocalDateTime afterLogin = LocalDateTime.now();

        // Then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getLastLogin()).isNotNull();
        assertThat(savedUser.getLastLogin()).isAfterOrEqualTo(beforeLogin);
        assertThat(savedUser.getLastLogin()).isBeforeOrEqualTo(afterLogin);
    }
}