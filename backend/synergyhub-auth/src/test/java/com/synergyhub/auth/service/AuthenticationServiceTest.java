package com.synergyhub.auth.service;

import com.synergyhub.auth.dto.LoginRequest;
import com.synergyhub.auth.dto.LoginResponse;
import com.synergyhub.auth.dto.RegisterRequest;
import com.synergyhub.auth.entity.Role;
import com.synergyhub.auth.entity.User;
import com.synergyhub.auth.repository.*;
import com.synergyhub.auth.security.JwtTokenProvider;
import com.synergyhub.common.exception.AccountLockedException;
import com.synergyhub.common.exception.EmailAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private LoginAttemptRepository loginAttemptRepository;
    @Mock
    private UserSessionRepository sessionRepository;
    @Mock
    private TwoFactorSecretRepository twoFactorSecretRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private EmailService emailService;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private Role guestRole;

    @BeforeEach
    void setUp() {
        guestRole = Role.builder()
                .roleId(4)
                .name("Guest")
                .permissions(new HashSet<>()) // Initialize empty set
                .build();

        testUser = User.builder()
                .userId(1)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .organizationId(1)
                .twoFactorEnabled(false)
                .accountLocked(false)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .roles(Set.of(guestRole))
                .build();

        // Mock HTTP request headers
        lenient().when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(httpRequest.getHeader("User-Agent")).thenReturn("Test Browser");
        lenient().when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
    }

    @Test
    void login_WithValidCredentials_ShouldReturnToken() {
        // Arrange
        LoginRequest request = new LoginRequest("test@example.com", "password", null);
        Authentication authentication = mock(Authentication.class);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(loginAttemptRepository.countFailedAttemptsSince(anyString(), any())).thenReturn(0L);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokenProvider.generateToken(any())).thenReturn("jwt-token");
        when(tokenProvider.getTokenId(anyString())).thenReturn("token-id");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);

        // Act
        LoginResponse response = authenticationService.login(request, httpRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertFalse(response.isRequires2FA());
        verify(userRepository).save(testUser);
        verify(sessionRepository).save(any());
        verify(auditLogService).log(eq(testUser), eq("LOGIN_SUCCESS"), anyString(), anyString(), anyString());
    }

    @Test
    void login_WithInvalidCredentials_ShouldThrowException() {
        // Arrange
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword", null);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(loginAttemptRepository.countFailedAttemptsSince(anyString(), any())).thenReturn(0L);
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () ->
                authenticationService.login(request, httpRequest));

        verify(loginAttemptRepository).save(any());
        verify(auditLogService).log(eq(testUser), eq("LOGIN_FAILED"), anyString(), anyString(), isNull());
    }

    @Test
    void login_WithLockedAccount_ShouldThrowAccountLockedException() {
        // Arrange
        testUser.setAccountLocked(true);
        testUser.setLockUntil(LocalDateTime.now().plusMinutes(30));
        LoginRequest request = new LoginRequest("test@example.com", "password", null);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(AccountLockedException.class, () ->
                authenticationService.login(request, httpRequest));
    }

    @Test
    void login_With2FAEnabled_ShouldRequire2FACode() {
        // Arrange
        testUser.setTwoFactorEnabled(true);
        LoginRequest request = new LoginRequest("test@example.com", "password", null);
        Authentication authentication = mock(Authentication.class);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(loginAttemptRepository.countFailedAttemptsSince(anyString(), any())).thenReturn(0L);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        // Act
        LoginResponse response = authenticationService.login(request, httpRequest);

        // Assert
        assertTrue(response.isRequires2FA());
        assertNull(response.getAccessToken());
    }

    @Test
    void register_WithValidData_ShouldCreateUser() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "New User",
                "newuser@example.com",
                "Password123",
                1,
                null
        );

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("Guest")).thenReturn(Optional.of(guestRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LoginResponse response = authenticationService.register(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getUser());
        assertEquals("newuser@example.com", response.getUser().getEmail());
        verify(userRepository).save(any(User.class));
        verify(emailVerificationService).sendVerificationEmail(any(User.class));
        verify(auditLogService).log(any(), eq("REGISTER"), anyString(), isNull(), isNull());
    }

    @Test
    void register_WithExistingEmail_ShouldThrowEmailAlreadyExistsException() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "Test User",
                "test@example.com",
                "Password123",
                1,
                null
        );

        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(EmailAlreadyExistsException.class, () ->
                authenticationService.register(request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void logout_ShouldRevokeSession() {
        // Arrange
        String tokenId = "token-id";

        // Act
        authenticationService.logout(tokenId);

        // Assert
        verify(sessionRepository).revokeSession(tokenId);
    }
}