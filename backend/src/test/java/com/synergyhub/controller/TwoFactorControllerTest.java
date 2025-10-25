package com.synergyhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synergyhub.config.TestSecurityConfig;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.TwoFactorDisableRequest;
import com.synergyhub.dto.request.TwoFactorVerifyRequest;
import com.synergyhub.dto.response.TwoFactorSetupResponse;
import com.synergyhub.dto.response.TwoFactorStatusResponse;
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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
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
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());
        doNothing().when(emailService).sendEmailVerification(anyString(), anyString());
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString());

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
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

        when(twoFactorAuthService.setupTwoFactor(any(User.class)))
                .thenReturn(setupResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/2fa/setup")
                        .with(csrf())
                        .with(user(userPrincipal)))  // ✅ Use UserPrincipal, not @WithMockUser
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.secret").value("TESTSECRET123"))
                .andExpect(jsonPath("$.data.backupCodes").isArray())
                .andExpect(jsonPath("$.data.backupCodes.length()").value(2));

        verify(twoFactorAuthService).setupTwoFactor(any(User.class));
    }

    @Test
    void verify2FA_WithValidCode_ShouldEnableTwoFactor() throws Exception {
        // Given
        TwoFactorVerifyRequest request = TwoFactorVerifyRequest.builder()
                .code("123456")
                .build();

        when(twoFactorAuthService.verifyAndEnableTwoFactor(any(User.class), eq("123456")))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/auth/2fa/verify")
                        .with(csrf())
                        .with(user(userPrincipal))  // ✅ Use UserPrincipal
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Two-factor authentication enabled successfully"));

        verify(twoFactorAuthService).verifyAndEnableTwoFactor(any(User.class), eq("123456"));
    }

    @Test
    void verify2FA_WithInvalidCode_ShouldReturnError() throws Exception {
        // Given
        TwoFactorVerifyRequest request = TwoFactorVerifyRequest.builder()
                .code("000000")
                .build();

        when(twoFactorAuthService.verifyAndEnableTwoFactor(any(User.class), eq("000000")))
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

        doNothing().when(twoFactorAuthService).disableTwoFactor(any(User.class), eq("password123"));

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

        verify(twoFactorAuthService).disableTwoFactor(any(User.class), eq("password123"));
    }

    @Test
    void get2FAStatus_WhenEnabled_ShouldReturnTrue() throws Exception {
        // Given
        testUser.setTwoFactorEnabled(true);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

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
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

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
        when(twoFactorAuthService.regenerateBackupCodes(any(User.class), eq("123456")))
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

        verify(twoFactorAuthService).regenerateBackupCodes(any(User.class), eq("123456"));
    }
}
