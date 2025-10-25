package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.Role;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.entity.UserSession;
import com.synergyhub.dto.mapper.UserMapper;
import com.synergyhub.dto.request.LoginRequest;
import com.synergyhub.dto.response.LoginResponse;
import com.synergyhub.dto.response.UserResponse;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

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
    private AuthenticationService authenticationService;

    private User testUser;
    private Organization testOrganization;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
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

        // When
        LoginResponse response = authenticationService.login(loginRequest, "127.0.0.1", "Test User Agent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.isTwoFactorRequired()).isFalse();

        verify(loginAttemptService).recordLoginAttempt(loginRequest.getEmail(), "127.0.0.1", true);
        verify(accountLockService).resetFailedAttempts(testUser);
        verify(userRepository).save(testUser);
        verify(userSessionRepository).save(any(UserSession.class));
        verify(auditLogService).logLoginSuccess(eq(testUser), anyString(), anyString());
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
        assertThatThrownBy(() -> authenticationService.login(loginRequest, "127.0.0.1", "Test User Agent"))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginAttemptService).recordLoginAttempt(eq(loginRequest.getEmail()), eq("127.0.0.1"), eq(false));  // FIX: Use eq() for all args
        verify(accountLockService).handleFailedLogin(testUser);
        verify(auditLogService).logLoginFailed(eq(loginRequest.getEmail()), anyString(), anyString(), anyString());  // FIX: Use eq() for first arg
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
        assertThatThrownBy(() -> authenticationService.login(loginRequest, "127.0.0.1", "Test User Agent"))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("Account is locked");

        verify(authenticationManager, never()).authenticate(any());
        verify(auditLogService).logLoginFailed(eq(loginRequest.getEmail()), anyString(), anyString(), contains("locked"));
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
        LoginResponse response = authenticationService.login(loginRequest, "127.0.0.1", "Test User Agent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isTwoFactorRequired()).isTrue();
        assertThat(response.getTwoFactorToken()).isEqualTo("temp-token");
        assertThat(response.getAccessToken()).isNull();

        verify(userSessionRepository, never()).save(any());
        verify(auditLogService).logTwoFactorRequired(testUser, "127.0.0.1");
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
        when(twoFactorAuthService.verifyCode(testUser, "123456")).thenReturn(true);
        when(jwtTokenProvider.generateTokenFromUserId(testUser.getId(), testUser.getEmail()))
                .thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(jwtTokenProvider.getTokenIdFromToken(anyString())).thenReturn("token-id-123");

        // When
        LoginResponse response = authenticationService.login(loginRequest, "127.0.0.1", "Test User Agent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.isTwoFactorRequired()).isFalse();

        verify(twoFactorAuthService).verifyCode(testUser, "123456");
        verify(auditLogService).logTwoFactorSuccess(testUser, "127.0.0.1");
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
        when(twoFactorAuthService.verifyCode(testUser, "000000")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authenticationService.login(loginRequest, "127.0.0.1", "Test User Agent"))
                .isInstanceOf(TwoFactorAuthenticationException.class)
                .hasMessageContaining("Invalid");

        verify(auditLogService).logTwoFactorFailed(testUser, "127.0.0.1");
    }
}