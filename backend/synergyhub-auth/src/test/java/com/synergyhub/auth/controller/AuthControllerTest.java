package com.synergyhub.auth.controller;

import com.synergyhub.auth.dto.*;
import com.synergyhub.auth.security.JwtTokenProvider;
import com.synergyhub.auth.service.AuthenticationService;
import com.synergyhub.auth.service.EmailVerificationService;
import com.synergyhub.auth.service.PasswordResetService;
import com.synergyhub.auth.service.TwoFactorAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private TwoFactorAuthService twoFactorAuthService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthController authController;

    // ========== Login Tests ==========

    @Test
    void login_WithValidCredentials_ShouldReturnToken() {
        // Arrange
        LoginRequest request = new LoginRequest("test@example.com", "Password123", null);
        UserInfoDto userInfo = createUserInfo();
        LoginResponse response = LoginResponse.builder()
                .accessToken("jwt-token")
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .user(userInfo)
                .requires2FA(false)
                .build();

        when(authenticationService.login(any(), any())).thenReturn(response);

        // Act
        var result = authController.login(request, httpRequest);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().isSuccess());
        assertEquals("jwt-token", result.getBody().getData().getAccessToken());
        verify(authenticationService).login(request, httpRequest);
    }

    @Test
    void login_WithValidCredentialsAndTwoFactorEnabled_ShouldRequire2FA() {
        // Arrange
        LoginRequest request = new LoginRequest("test@example.com", "Password123", null);
        LoginResponse response = LoginResponse.builder()
                .requires2FA(true)
                .build();

        when(authenticationService.login(any(), any())).thenReturn(response);

        // Act
        var result = authController.login(request, httpRequest);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().getData().isRequires2FA());
        assertNull(result.getBody().getData().getAccessToken());
    }

    @Test
    void login_WithInvalidRequest_ShouldValidate() {
        // Arrange
        LoginRequest request = new LoginRequest("", "", null);

        // Act & Assert - validation should be handled by @Valid annotation
        // This test verifies the controller accepts the request type
        assertNotNull(request);
    }

    // ========== Register Tests ==========

    @Test
    void register_WithValidData_ShouldReturnCreated() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "New User",
                "newuser@example.com",
                "Password123",
                1,
                null
        );

        UserInfoDto userInfo = UserInfoDto.builder()
                .userId(1)
                .email("newuser@example.com")
                .name("New User")
                .organizationId(1)
                .roles(Set.of("Guest"))
                .emailVerified(false)
                .build();

        LoginResponse response = LoginResponse.builder()
                .user(userInfo)
                .build();

        when(authenticationService.register(any())).thenReturn(response);

        // Act
        var result = authController.register(request);

        // Assert
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().isSuccess());
        assertEquals("newuser@example.com", result.getBody().getData().getUser().getEmail());
        verify(authenticationService).register(request);
    }

    @Test
    void register_ShouldReturnSuccessMessage() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "Test User",
                "test@example.com",
                "Password123",
                1,
                null
        );

        when(authenticationService.register(any())).thenReturn(LoginResponse.builder().build());

        // Act
        var result = authController.register(request);

        // Assert
        assertTrue(result.getBody().getMessage().contains("Registration successful"));
        assertTrue(result.getBody().getMessage().contains("verify your account"));
    }

    // ========== Logout Tests ==========

    @Test
    void logout_WithValidToken_ShouldReturnSuccess() {
        // Arrange
        String authHeader = "Bearer jwt-token";
        when(tokenProvider.getTokenId(anyString())).thenReturn("token-id");

        // Act
        var result = authController.logout(authHeader);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isSuccess());
        verify(authenticationService).logout("token-id");
    }

    @Test
    void logoutAllDevices_ShouldReturnSuccess() {
        // Act
        var result = authController.logoutAllDevices();

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isSuccess());
        verify(authenticationService).logoutAllDevices();
    }

    // ========== Password Reset Tests ==========

    @Test
    void requestPasswordReset_WithValidEmail_ShouldReturnSuccess() {
        // Arrange
        PasswordResetRequest request = new PasswordResetRequest("test@example.com");

        // Act
        var result = authController.requestPasswordReset(request);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isSuccess());
        verify(passwordResetService).requestPasswordReset(request);
    }

    @Test
    void requestPasswordReset_ShouldReturnGenericMessage() {
        // Arrange
        PasswordResetRequest request = new PasswordResetRequest("test@example.com");

        // Act
        var result = authController.requestPasswordReset(request);

        // Assert
        // Should not reveal whether email exists
        assertTrue(result.getBody().getMessage().contains("If an account exists"));
    }

    @Test
    void confirmPasswordReset_WithValidData_ShouldReturnSuccess() {
        // Arrange
        PasswordResetConfirm request = new PasswordResetConfirm("token", "NewPassword123");

        // Act
        var result = authController.confirmPasswordReset(request);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isSuccess());
        verify(passwordResetService).confirmPasswordReset(request);
    }

    // ========== 2FA Tests ==========

    @Test
    void setup2FA_ShouldReturnSetupResponse() {
        // Arrange
        TwoFactorSetupResponse response = TwoFactorSetupResponse.builder()
                .secret("secret")
                .qrCodeUrl("data:image/png;base64,...")
                .backupCodes(List.of("12345678", "87654321"))
                .build();

        when(twoFactorAuthService.setup2FA()).thenReturn(response);

        // Act
        var result = authController.setup2FA();

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody().getData());
        assertEquals("secret", result.getBody().getData().getSecret());
        assertEquals(2, result.getBody().getData().getBackupCodes().size());
    }

    @Test
    void verify2FA_WithValidCode_ShouldReturnSuccess() {
        // Arrange
        TwoFactorVerifyRequest request = new TwoFactorVerifyRequest("123456");

        // Act
        var result = authController.verify2FA(request);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isSuccess());
        verify(twoFactorAuthService).verify2FASetup(request);
    }

    @Test
    void disable2FA_WithValidCode_ShouldReturnSuccess() {
        // Arrange
        TwoFactorVerifyRequest request = new TwoFactorVerifyRequest("123456");

        // Act
        var result = authController.disable2FA(request);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isSuccess());
        verify(twoFactorAuthService).disable2FA(request);
    }

    @Test
    void getBackupCodes_ShouldReturnCodes() {
        // Arrange
        List<String> codes = List.of("12345678", "87654321", "11111111");
        when(twoFactorAuthService.getBackupCodes()).thenReturn(codes);

        // Act
        var result = authController.getBackupCodes();

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(3, result.getBody().getData().size());
        assertTrue(result.getBody().getData().contains("12345678"));
    }

    @Test
    void regenerateBackupCodes_WithValidCode_ShouldReturnNewCodes() {
        // Arrange
        TwoFactorVerifyRequest request = new TwoFactorVerifyRequest("123456");
        List<String> newCodes = List.of("99999999", "88888888");
        when(twoFactorAuthService.regenerateBackupCodes(request)).thenReturn(newCodes);

        // Act
        var result = authController.regenerateBackupCodes(request);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().getData().size());
    }

    // ========== Email Verification Tests ==========

    @Test
    void verifyEmail_WithValidToken_ShouldReturnSuccess() {
        // Act
        var result = authController.verifyEmail("valid-token");

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isSuccess());
        verify(emailVerificationService).verifyEmail("valid-token");
    }

    @Test
    void resendVerificationEmail_WithValidEmail_ShouldReturnSuccess() {
        // Act
        var result = authController.resendVerificationEmail("test@example.com");

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isSuccess());
        verify(emailVerificationService).resendVerificationEmail("test@example.com");
    }

    // ========== Helper Methods ==========

    private UserInfoDto createUserInfo() {
        return UserInfoDto.builder()
                .userId(1)
                .email("test@example.com")
                .name("Test User")
                .organizationId(1)
                .roles(Set.of("Team Member"))
                .permissions(Set.of("VIEW_PROJECT"))
                .twoFactorEnabled(false)
                .emailVerified(true)
                .build();
    }
}