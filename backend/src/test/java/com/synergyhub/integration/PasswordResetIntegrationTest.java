package com.synergyhub.integration;

import com.synergyhub.config.TestSecurityConfig;
import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.PasswordResetToken;
import com.synergyhub.domain.entity.Role;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.PasswordResetConfirmRequest;
import com.synergyhub.dto.request.PasswordResetRequest;
import com.synergyhub.exception.InvalidTokenException;
import com.synergyhub.repository.*;
import com.synergyhub.service.auth.PasswordResetService;
import com.synergyhub.util.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@Transactional
class PasswordResetIntegrationTest {

    @MockBean
    private EmailService emailService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Organization testOrganization;

    @BeforeEach
    void setUp() {
        // Mock email service to do nothing
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());
        doNothing().when(emailService).sendEmailVerification(anyString(), anyString());
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString());

        // Clean up
        userSessionRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();

        // Setup test data
        testOrganization = Organization.builder()
                .name("Test Organization")
                .address("123 Test Street")
                .build();
        testOrganization = organizationRepository.save(testOrganization);

        Role role = Role.builder()
                .name("Team Member")
                .description("Regular team member")
                .build();
        roleRepository.save(role);

        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("OldPassword123"))
                .organization(testOrganization)
                .emailVerified(true)
                .twoFactorEnabled(false)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void passwordResetFlow_EndToEnd_ShouldWorkCorrectly() {
        // Step 1: Request password reset
        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .email(testUser.getEmail())
                .build();

        passwordResetService.requestPasswordReset(resetRequest, "127.0.0.1");

        // Verify token was created
        PasswordResetToken token = passwordResetTokenRepository.findAll().get(0);
        assertThat(token).isNotNull();
        assertThat(token.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(token.getUsed()).isFalse();
        assertThat(token.getExpiryTime()).isAfter(LocalDateTime.now());

        // Step 2: Validate token
        boolean isValid = passwordResetService.validateResetToken(token.getToken());
        assertThat(isValid).isTrue();

        // Step 3: Reset password
        PasswordResetConfirmRequest confirmRequest = PasswordResetConfirmRequest.builder()
                .token(token.getToken())
                .newPassword("NewSecurePass123")
                .build();

        passwordResetService.resetPassword(confirmRequest, "127.0.0.1");

        // Verify password was changed
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewSecurePass123", updatedUser.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("OldPassword123", updatedUser.getPasswordHash())).isFalse();

        // Verify token was marked as used
        PasswordResetToken usedToken = passwordResetTokenRepository.findById(token.getId()).orElseThrow();
        assertThat(usedToken.getUsed()).isTrue();

        // Step 4: Try to use the same token again (should fail)
        assertThatThrownBy(() -> passwordResetService.resetPassword(confirmRequest, "127.0.0.1"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    void requestPasswordReset_MultipleRequests_ShouldInvalidateOldTokens() {
        // First request
        PasswordResetRequest request1 = PasswordResetRequest.builder()
                .email(testUser.getEmail())
                .build();

        passwordResetService.requestPasswordReset(request1, "127.0.0.1");

        // Wait a bit
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Second request
        PasswordResetRequest request2 = PasswordResetRequest.builder()
                .email(testUser.getEmail())
                .build();

        passwordResetService.requestPasswordReset(request2, "127.0.0.1");

        // Only the latest token should be valid
        var tokens = passwordResetTokenRepository.findByUser(testUser);
        long validTokenCount = tokens.stream().filter(t -> !t.getUsed()).count();

        assertThat(validTokenCount).isEqualTo(1);
    }
}