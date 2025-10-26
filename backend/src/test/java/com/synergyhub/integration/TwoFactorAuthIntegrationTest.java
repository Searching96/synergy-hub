package com.synergyhub.integration;

import com.synergyhub.config.TestSecurityConfig;
import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.Role;
import com.synergyhub.domain.entity.TwoFactorSecret;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.response.TwoFactorSetupResponse;
import com.synergyhub.exception.BadRequestException;
import com.synergyhub.repository.*;
import com.synergyhub.service.auth.TwoFactorAuthService;
import com.synergyhub.util.EmailService;
import com.synergyhub.util.TotpUtil;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@Transactional
class TwoFactorAuthIntegrationTest {

    @MockBean
    private EmailService emailService;

    @Autowired
    private TwoFactorAuthService twoFactorAuthService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TwoFactorSecretRepository twoFactorSecretRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TotpUtil totpUtil;

    private User testUser;
    private CodeGenerator codeGenerator;
    private TimeProvider timeProvider;
    private static final String TEST_IP = "192.168.1.1";  // ✅ Added constant

    @BeforeEach
    void setUp() {
        // Mock email service to do nothing
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), any(User.class), anyString());
        doNothing().when(emailService).sendEmailVerification(anyString(), anyString(), any(User.class), anyString());
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString(), any(User.class), anyString());

        // Clean up
        twoFactorSecretRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();

        // Setup test data
        Organization org = Organization.builder()
                .name("Test Organization")
                .address("123 Test Street")
                .build();
        org = organizationRepository.save(org);

        Role role = Role.builder()
                .name("Team Member")
                .description("Regular team member")
                .build();
        roleRepository.save(role);

        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .organization(org)
                .emailVerified(true)
                .twoFactorEnabled(false)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build();
        testUser = userRepository.save(testUser);

        // Initialize TOTP code generator for testing
        codeGenerator = new DefaultCodeGenerator();
        timeProvider = new SystemTimeProvider();
    }

    @Test
    void complete2FASetupFlow_EndToEnd_ShouldWorkCorrectly() throws Exception {
        // Step 1: Setup 2FA
        TwoFactorSetupResponse setupResponse = twoFactorAuthService.setupTwoFactor(testUser, TEST_IP);  // ✅ Use constant

        assertThat(setupResponse).isNotNull();
        assertThat(setupResponse.getSecret()).isNotNull();
        assertThat(setupResponse.getQrCodeUrl()).isNotNull();
        assertThat(setupResponse.getBackupCodes()).hasSize(10);

        // Verify secret was saved
        TwoFactorSecret savedSecret = twoFactorSecretRepository.findByUser(testUser).orElseThrow();
        assertThat(savedSecret.getSecret()).isEqualTo(setupResponse.getSecret());
        assertThat(savedSecret.getBackupCodes()).isNotNull();

        // Step 2: Generate valid TOTP code
        String validCode = codeGenerator.generate(
                setupResponse.getSecret(),
                timeProvider.getTime() / 30
        );

        // Step 3: Verify and enable 2FA with valid code
        boolean verifyResult = twoFactorAuthService.verifyAndEnableTwoFactor(testUser, validCode, TEST_IP);  // ✅ Use constant

        assertThat(verifyResult).isTrue();

        // Verify 2FA is enabled in database
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getTwoFactorEnabled()).isTrue();

        // Step 4: Wait a bit to ensure we're in a different time window, then verify code again
        Thread.sleep(31000); // ✅ Wait 31 seconds to ensure new TOTP window (30s window + 1s buffer)

        String loginCode = codeGenerator.generate(
                setupResponse.getSecret(),
                timeProvider.getTime() / 30
        );

        boolean loginVerifyResult = twoFactorAuthService.verifyCode(updatedUser, loginCode, TEST_IP);  // ✅ Use constant
        assertThat(loginVerifyResult).isTrue();

        // Step 5: Test backup code
        String backupCode = setupResponse.getBackupCodes().get(0);
        boolean backupResult = twoFactorAuthService.verifyCode(updatedUser, backupCode, TEST_IP);  // ✅ Use constant

        assertThat(backupResult).isTrue();

        // Verify backup code was consumed
        TwoFactorSecret afterBackup = twoFactorSecretRepository.findByUser(updatedUser).orElseThrow();
        assertThat(afterBackup.getBackupCodes()).doesNotContain(backupCode);

        // Step 6: Try to use the same backup code again (should fail)
        boolean sameBackupResult = twoFactorAuthService.verifyCode(updatedUser, backupCode, TEST_IP);  // ✅ Use constant
        assertThat(sameBackupResult).isFalse();

        // Step 7: Disable 2FA
        twoFactorAuthService.disableTwoFactor(updatedUser, "password123", TEST_IP);  // ✅ Use constant

        // Verify 2FA is disabled
        User finalUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(finalUser.getTwoFactorEnabled()).isFalse();

        // Verify secret was deleted
        assertThat(twoFactorSecretRepository.findByUser(finalUser)).isEmpty();
    }

    @Test
    void verifyAndEnable_WithInvalidCode_ShouldNotEnable2FA() throws Exception {
        // Step 1: Setup 2FA
        TwoFactorSetupResponse setupResponse = twoFactorAuthService.setupTwoFactor(testUser, TEST_IP);  // ✅ Use constant

        // Step 2: Try to verify with invalid code
        boolean result = twoFactorAuthService.verifyAndEnableTwoFactor(testUser, "000000", TEST_IP);  // ✅ Use constant

        assertThat(result).isFalse();

        // Verify 2FA is still disabled
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getTwoFactorEnabled()).isFalse();
    }

    @Test
    void setup2FA_WhenAlreadyEnabled_ShouldThrowException() {
        // Given - enable 2FA first
        testUser.setTwoFactorEnabled(true);
        userRepository.save(testUser);

        // When & Then
        assertThatThrownBy(() -> twoFactorAuthService.setupTwoFactor(testUser, TEST_IP))  // ✅ Use constant
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already enabled");
    }

    @Test
    void disableTwoFactor_WithWrongPassword_ShouldThrowException() {
        // Given - setup and enable 2FA
        testUser.setTwoFactorEnabled(true);
        userRepository.save(testUser);

        // When & Then
        assertThatThrownBy(() -> twoFactorAuthService.disableTwoFactor(testUser, "wrongpassword", TEST_IP))  // ✅ Use constant with wrong password
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid password");

        // Verify 2FA is still enabled
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getTwoFactorEnabled()).isTrue();
    }

    @Test
    void verifyCode_WithAllBackupCodesUsed_ShouldOnlyAcceptTOTP() throws Exception {
        // Setup 2FA
        TwoFactorSetupResponse setupResponse = twoFactorAuthService.setupTwoFactor(testUser, TEST_IP);  // ✅ Use constant

        // Enable 2FA
        String validCode = codeGenerator.generate(
                setupResponse.getSecret(),
                timeProvider.getTime() / 30
        );
        twoFactorAuthService.verifyAndEnableTwoFactor(testUser, validCode, TEST_IP);  // ✅ Use constant

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();

        // Use all backup codes
        for (String backupCode : setupResponse.getBackupCodes()) {
            boolean result = twoFactorAuthService.verifyCode(updatedUser, backupCode, TEST_IP);  // ✅ Use constant
            assertThat(result).isTrue();
        }

        // Verify all backup codes are consumed
        TwoFactorSecret afterAllUsed = twoFactorSecretRepository.findByUser(updatedUser).orElseThrow();
        assertThat(afterAllUsed.getBackupCodes()).isEqualTo("[]");

        // TOTP should still work - wait to ensure fresh time window
        Thread.sleep(31000); // ✅ Wait 31 seconds for new TOTP window
        String totpCode = codeGenerator.generate(
                setupResponse.getSecret(),
                timeProvider.getTime() / 30
        );
        boolean totpResult = twoFactorAuthService.verifyCode(updatedUser, totpCode, TEST_IP);  // ✅ Use constant
        assertThat(totpResult).isTrue();
    }

    // ✅ Additional test for regenerating backup codes
    @Test
    void regenerateBackupCodes_ShouldGenerateNewCodes() throws Exception {
        // Setup and enable 2FA
        TwoFactorSetupResponse setupResponse = twoFactorAuthService.setupTwoFactor(testUser, TEST_IP);

        String validCode = codeGenerator.generate(
                setupResponse.getSecret(),
                timeProvider.getTime() / 30
        );
        twoFactorAuthService.verifyAndEnableTwoFactor(testUser, validCode, TEST_IP);

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();

        // Use one backup code
        String firstBackupCode = setupResponse.getBackupCodes().get(0);
        twoFactorAuthService.verifyCode(updatedUser, firstBackupCode, TEST_IP);

        // Wait for new TOTP window
        Thread.sleep(31000);

        // Generate new verification code for regenerating backup codes
        String verificationCode = codeGenerator.generate(
                setupResponse.getSecret(),
                timeProvider.getTime() / 30
        );

        // Regenerate backup codes
        var newBackupCodes = twoFactorAuthService.regenerateBackupCodes(
                updatedUser,
                verificationCode,
                TEST_IP
        );

        // Verify new backup codes
        assertThat(newBackupCodes).hasSize(10);
        assertThat(newBackupCodes).doesNotContain(firstBackupCode);
        assertThat(newBackupCodes).isNotEqualTo(setupResponse.getBackupCodes());

        // Old backup code should no longer work
        boolean oldCodeResult = twoFactorAuthService.verifyCode(
                updatedUser,
                setupResponse.getBackupCodes().get(1),
                TEST_IP
        );
        assertThat(oldCodeResult).isFalse();

        // New backup code should work
        boolean newCodeResult = twoFactorAuthService.verifyCode(
                updatedUser,
                newBackupCodes.get(0),
                TEST_IP
        );
        assertThat(newCodeResult).isTrue();
    }

    // ✅ Test for verifying that backup codes are unique
    @Test
    void setupTwoFactor_ShouldGenerateUniqueBackupCodes() {
        // Setup 2FA
        TwoFactorSetupResponse setupResponse = twoFactorAuthService.setupTwoFactor(testUser, TEST_IP);

        // Verify all backup codes are unique
        var backupCodes = setupResponse.getBackupCodes();
        assertThat(backupCodes).hasSize(10);
        assertThat(backupCodes).doesNotHaveDuplicates();

        // Verify all codes are 8 digits
        backupCodes.forEach(code -> {
            assertThat(code).hasSize(8);
            assertThat(code).matches("\\d{8}");
        });
    }
}