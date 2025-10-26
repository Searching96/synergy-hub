package com.synergyhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synergyhub.config.TestSecurityConfig;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.TwoFactorDisableRequest;
import com.synergyhub.dto.request.TwoFactorVerifyRequest;
import com.synergyhub.dto.response.TwoFactorSetupResponse;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.JwtTokenProvider;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.auth.TwoFactorAuthService;
import com.synergyhub.util.EmailService;
import com.synergyhub.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class TwoFactorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TwoFactorAuthService twoFactorAuthService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private EmailService emailService;

    private User testUser;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser();
        userPrincipal = UserPrincipal.create(testUser);

        // Mock email service
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), any(), anyString());
        doNothing().when(emailService).sendEmailVerification(anyString(), anyString(), any(), anyString());
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString(), any(), anyString());

        // ✅ Mock both findById and findByEmailWithRolesAndPermissions
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmailWithRolesAndPermissions(testUser.getEmail()))
                .thenReturn(Optional.of(testUser));
    }

    @Test
    void setup2FA_ShouldReturnSetupResponse() throws Exception {
        // Given
        TwoFactorSetupResponse setupResponse = TwoFactorSetupResponse.builder()
                .secret("TESTSECRET123")
                .qrCodeUrl("data:image/png;base64,...")
                .backupCodes(List.of("12345678", "87654321"))
                .message("Scan QR code")
                .build();

        when(twoFactorAuthService.setupTwoFactor(eq(testUser), anyString()))  // ✅ Use eq() for User
                .thenReturn(setupResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/2fa/setup")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.secret").value("TESTSECRET123"))
                .andExpect(jsonPath("$.data.backupCodes").isArray())
                .andExpect(jsonPath("$.data.backupCodes.length()").value(2));

        verify(twoFactorAuthService).setupTwoFactor(eq(testUser), anyString());  // ✅ Use eq()
    }

    @Test
    void verify2FA_WithValidCode_ShouldEnableTwoFactor() throws Exception {
        // Given
        TwoFactorVerifyRequest request = TwoFactorVerifyRequest.builder()
                .code("123456")
                .build();

        when(twoFactorAuthService.verifyAndEnableTwoFactor(eq(testUser), eq("123456"), anyString()))  // ✅ Use eq()
                .thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/auth/2fa/verify")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Two-factor authentication enabled successfully"));

        verify(twoFactorAuthService).verifyAndEnableTwoFactor(eq(testUser), eq("123456"), anyString());  // ✅ Use eq()
    }

    @Test
    void verify2FA_WithInvalidCode_ShouldReturnError() throws Exception {
        // Given
        TwoFactorVerifyRequest request = TwoFactorVerifyRequest.builder()
                .code("000000")
                .build();

        when(twoFactorAuthService.verifyAndEnableTwoFactor(eq(testUser), eq("000000"), anyString()))  // ✅ Use eq()
                .thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/auth/2fa/verify")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Invalid verification code"));
    }

    @Test
    void disable2FA_WithValidPassword_ShouldDisableTwoFactor() throws Exception {
        // Given
        TwoFactorDisableRequest request = TwoFactorDisableRequest.builder()
                .password("password123")
                .build();

        doNothing().when(twoFactorAuthService).disableTwoFactor(
                eq(testUser),
                eq("password123"),
                anyString()  // ✅ Added IP address parameter
        );

        // When & Then
        mockMvc.perform(post("/api/auth/2fa/disable")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Two-factor authentication disabled"));

        verify(twoFactorAuthService).disableTwoFactor(
                eq(testUser),
                eq("password123"),
                anyString()  // ✅ Added IP address parameter
        );
    }

    @Test
    void get2FAStatus_WhenEnabled_ShouldReturnTrue() throws Exception {
        // Given
        testUser.setTwoFactorEnabled(true);
        when(userRepository.findByEmailWithRolesAndPermissions(testUser.getEmail()))
                .thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(get("/api/auth/2fa/status")
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void get2FAStatus_WhenDisabled_ShouldReturnFalse() throws Exception {
        // Given
        testUser.setTwoFactorEnabled(false);
        when(userRepository.findByEmailWithRolesAndPermissions(testUser.getEmail()))
                .thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(get("/api/auth/2fa/status")
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void regenerateBackupCodes_WithValidCode_ShouldReturnNewCodes() throws Exception {
        // Given
        TwoFactorVerifyRequest request = TwoFactorVerifyRequest.builder()
                .code("123456")
                .build();

        List<String> newBackupCodes = List.of("11111111", "22222222", "33333333");
        when(twoFactorAuthService.regenerateBackupCodes(eq(testUser), eq("123456"), anyString()))  // ✅ Use eq()
                .thenReturn(newBackupCodes);

        // When & Then
        mockMvc.perform(post("/api/auth/2fa/regenerate-backup-codes")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.message").value("New backup codes generated. Please save them in a secure location."));

        verify(twoFactorAuthService).regenerateBackupCodes(eq(testUser), eq("123456"), anyString());  // ✅ Use eq()
    }

    // ✅ Additional test for error cases
    @Test
    void setup2FA_WhenAlreadyEnabled_ShouldReturnError() throws Exception {
        // Given
        when(twoFactorAuthService.setupTwoFactor(eq(testUser), anyString()))
                .thenThrow(new RuntimeException("Two-factor authentication is already enabled"));

        // When & Then
        mockMvc.perform(post("/api/auth/2fa/setup")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    @Test
    void disable2FA_WithInvalidPassword_ShouldReturnError() throws Exception {
        // Given
        TwoFactorDisableRequest request = TwoFactorDisableRequest.builder()
                .password("wrongpassword")
                .build();

        doThrow(new RuntimeException("Invalid password"))
                .when(twoFactorAuthService).disableTwoFactor(
                        eq(testUser),
                        eq("wrongpassword"),
                        anyString()
                );

        // When & Then
        mockMvc.perform(post("/api/auth/2fa/disable")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    @Test
    void verify2FA_WithEmptyCode_ShouldReturnValidationError() throws Exception {
        // Given
        TwoFactorVerifyRequest request = TwoFactorVerifyRequest.builder()
                .code("")  // Empty code
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/2fa/verify")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // ✅ Verify service was never called with invalid input
        verify(twoFactorAuthService, never()).verifyAndEnableTwoFactor(any(), anyString(), anyString());
    }
}
