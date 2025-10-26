package com.synergyhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synergyhub.config.TestSecurityConfig;
import com.synergyhub.domain.entity.*;
import com.synergyhub.dto.request.*;
import com.synergyhub.repository.*;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.util.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockBean
    private EmailService emailService;

    private Organization testOrganization;
    private Role testRole;
    private User testUser;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        // Clean up
        emailVerificationRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();

        // Mock email service
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), any(User.class), anyString());
        doNothing().when(emailService).sendEmailVerification(anyString(), anyString(), any(User.class), anyString());
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString(), any(User.class), anyString());

        // Setup test organization
        testOrganization = new Organization();
        testOrganization.setName("Test Organization");
        testOrganization.setAddress("123 Test Street");
        testOrganization = organizationRepository.save(testOrganization);

        // Setup test role
        testRole = Role.builder()
                .name("Team Member")
                .description("Regular team member")
                .build();
        testRole = roleRepository.save(testRole);

        // Setup test user
        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .organization(testOrganization)
                .emailVerified(true)
                .twoFactorEnabled(false)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .roles(new HashSet<>())
                .build();
        testUser.getRoles().add(testRole);
        testUser = userRepository.save(testUser);

        userPrincipal = UserPrincipal.create(testUser);
    }

    @Test
    void login_WithValidCredentials_ShouldReturnLoginResponse() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").exists())
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.user.name").value("Test User"))
                .andExpect(jsonPath("$.data.twoFactorRequired").value(false));
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrong_password")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_WithLockedAccount_ShouldReturnForbidden() throws Exception {
        // Given - Lock the account
        testUser.setAccountLocked(true);
        testUser.setLockUntil(LocalDateTime.now().plusMinutes(30));
        userRepository.save(testUser);

        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_WithValidationErrors_ShouldReturnBadRequest() throws Exception {
        // Given - Invalid email format
        LoginRequest request = LoginRequest.builder()
                .email("invalid-email")
                .password("password123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_WithValidData_ShouldReturnUserResponse() throws Exception {
        // Given

        RegisterRequest request = RegisterRequest.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .password("SecurePass123")
                .organizationId(testOrganization.getId())
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.data.name").value("John Doe"))
                .andExpect(jsonPath("$.data.emailVerified").value(false))
                .andExpect(jsonPath("$.message").value("Registration successful. Please check your email to verify your account."));

        // Verify user was created in database
        User createdUser = userRepository.findByEmail("john.doe@example.com").orElseThrow();
        assertThat(createdUser.getName()).isEqualTo("John Doe");
        assertThat(createdUser.getEmailVerified()).isFalse();
    }

    @Test
    void register_WithExistingEmail_ShouldReturnConflict() throws Exception {
        // Given - user already exists
        RegisterRequest request = RegisterRequest.builder()
                .name("Test User")
                .email("test@example.com")
                .password("SecurePass123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void verifyEmail_WithValidToken_ShouldReturnSuccess() throws Exception {
        // Given - Create an unverified user with verification token
        User unverifiedUser = User.builder()
                .name("Unverified User")
                .email("unverified@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .organization(testOrganization)
                .emailVerified(false)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .roles(new HashSet<>())
                .build();
        unverifiedUser.getRoles().add(testRole);
        unverifiedUser = userRepository.save(unverifiedUser);

        EmailVerification verification = EmailVerification.builder()
                .user(unverifiedUser)
                .token("valid-token-12345")
                .verified(false)
                .expiryTime(LocalDateTime.now().plusHours(24))
                .build();
        emailVerificationRepository.save(verification);

        // When & Then
        mockMvc.perform(post("/api/auth/verify-email")
                        .with(csrf())
                        .param("token", "valid-token-12345"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        // Verify user is now verified
        User verifiedUser = userRepository.findById(unverifiedUser.getId()).orElseThrow();
        assertThat(verifiedUser.getEmailVerified()).isTrue();
    }

    @Test
    void verifyEmail_WithInvalidToken_ShouldReturnBadRequest() throws Exception {
        // Given
        String token = "invalid-token";

        // When & Then
        mockMvc.perform(post("/api/auth/verify-email")
                        .with(csrf())
                        .param("token", token))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void resendVerificationEmail_WithValidEmail_ShouldReturnSuccess() throws Exception {
        // Given - Create unverified user
        User unverifiedUser = User.builder()
                .name("Unverified User")
                .email("unverified2@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .organization(testOrganization)
                .emailVerified(false)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .roles(new HashSet<>())
                .build();
        unverifiedUser.getRoles().add(testRole);
        userRepository.save(unverifiedUser);

        // When & Then
        mockMvc.perform(post("/api/auth/resend-verification")
                        .with(csrf())
                        .param("email", "unverified2@example.com"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Verification email sent"));
    }

    @Test
    void requestPasswordReset_WithValidEmail_ShouldReturnSuccess() throws Exception {
        // Given
        PasswordResetRequest request = PasswordResetRequest.builder()
                .email("test@example.com")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If the email exists, a password reset link has been sent"));

        // Verify token was created
        assertThat(passwordResetTokenRepository.findAll()).isNotEmpty();
    }

    @Test
    void resetPassword_WithValidToken_ShouldReturnSuccess() throws Exception {
        // Given - Create password reset token
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(testUser)
                .token("valid-reset-token-12345")
                .expiryTime(LocalDateTime.now().plusHours(1))
                .build();
        passwordResetTokenRepository.save(resetToken);

        PasswordResetConfirmRequest request = PasswordResetConfirmRequest.builder()
                .token("valid-reset-token-12345")
                .newPassword("NewSecurePass123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password has been reset successfully"));
    }

    @Test
    void resetPassword_WithInvalidToken_ShouldReturnBadRequest() throws Exception {
        // Given
        PasswordResetConfirmRequest request = PasswordResetConfirmRequest.builder()
                .token("invalid-token")
                .newPassword("NewSecurePass123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void validateResetToken_WithValidToken_ShouldReturnTrue() throws Exception {
        // Given - Create valid token
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(testUser)
                .token("valid-token-check")
                .expiryTime(LocalDateTime.now().plusHours(1))
                .build();
        passwordResetTokenRepository.save(resetToken);

        // When & Then
        mockMvc.perform(get("/api/auth/validate-reset-token")
                        .param("token", "valid-token-check"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void changePassword_WithValidData_ShouldReturnSuccess() throws Exception {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("password123")
                .newPassword("NewSecurePass123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/change-password")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully. Please login again with your new password."));
    }
}