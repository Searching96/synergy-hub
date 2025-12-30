package com.synergyhub.service.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.TwoFactorSecret;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.response.TwoFactorSetupResponse;
import com.synergyhub.exception.BadRequestException;
import com.synergyhub.repository.TwoFactorSecretRepository;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.service.security.AuditLogService;
// import com.synergyhub.util.TotpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoFactorAuthServiceTest {

    @Mock
    private TwoFactorSecretRepository twoFactorSecretRepository;

    @Mock
    private UserRepository userRepository;

        private MockTotpService mockTotpService;
        private MockBackupCodeService mockBackupCodeService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    private TwoFactorAuthService twoFactorAuthService;

    private ObjectMapper objectMapper;
    private User testUser;
    private static final String TEST_IP = "192.168.1.1";

    @BeforeEach
    void setUp() {
        // Create real ObjectMapper
        objectMapper = new ObjectMapper();

        mockTotpService = new MockTotpService();
        mockBackupCodeService = new MockBackupCodeService();
        twoFactorAuthService = new TwoFactorAuthService(
                twoFactorSecretRepository,
                userRepository,
                mockTotpService,
                objectMapper,
                passwordEncoder,
                auditLogService,
                mockBackupCodeService
        );

        Organization org = Organization.builder()
                .id(1)
                .name("Test Organization")
                .build();

        testUser = User.builder()
                .id(1)
                .email("test@example.com")
                .name("Test User")
                .passwordHash("$2a$10$hashedPassword")
                .organization(org)
                .emailVerified(true)
                .twoFactorEnabled(false)
                .roles(new HashSet<>())
                .build();
    }

    @Test
    void setupTwoFactor_WhenNotAlreadyEnabled_ShouldGenerateSecretAndQrCode() throws Exception {
        // Given
        String secret = "TESTSECRET123456";
        String qrCodeUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...";

        mockTotpService.secret = secret;
        mockTotpService.qrCodeUrl = qrCodeUrl;

        ArgumentCaptor<TwoFactorSecret> captor = ArgumentCaptor.forClass(TwoFactorSecret.class);

        // When
        TwoFactorSetupResponse response = twoFactorAuthService.setupTwoFactor(testUser, TEST_IP);  

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSecret()).isEqualTo(secret);
        assertThat(response.getQrCodeUrl()).isEqualTo(qrCodeUrl);
        assertThat(response.getBackupCodes()).isNotNull().hasSize(10);
        assertThat(response.getMessage()).contains("Scan the QR code");

        // Verify backup codes format (8 digits each)
        response.getBackupCodes().forEach(code -> {
            assertThat(code).hasSize(8);
            assertThat(code).matches("\\d{8}");
        });

        verify(twoFactorSecretRepository).save(captor.capture());
        TwoFactorSecret saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getSecret()).isEqualTo(secret);
        assertThat(saved.getBackupCodes()).isNotNull();

        verify(auditLogService).createAuditLog(
                eq(testUser),
                eq("TWO_FACTOR_SETUP_INITIATED"),
                anyString(),
                eq(TEST_IP)
        );
    }

    @Test
    void setupTwoFactor_WhenAlreadyEnabled_ShouldThrowException() {
        // Given
        testUser.setTwoFactorEnabled(true);

        // When & Then
        assertThatThrownBy(() -> twoFactorAuthService.setupTwoFactor(testUser, TEST_IP))  
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already enabled");

        verify(twoFactorSecretRepository, never()).save(any());
        verify(auditLogService, never()).createAuditLog(any(), anyString(), anyString(), anyString());
    }

    @Test
    void verifyAndEnableTwoFactor_WithValidCode_ShouldEnableTwoFactor() {
        // Given
        String secret = "TESTSECRET123456";
        String code = "123456";

        TwoFactorSecret twoFactorSecret = TwoFactorSecret.builder()
                .user(testUser)
                .secret(secret)
                .backupCodes("[\"12345678\",\"87654321\"]")
                .build();

        when(twoFactorSecretRepository.findByUser(testUser)).thenReturn(Optional.of(twoFactorSecret));
        mockTotpService.validCode = true;

        // When
        boolean result = twoFactorAuthService.verifyAndEnableTwoFactor(testUser, code, TEST_IP);  

        // Then
        assertThat(result).isTrue();
        assertThat(testUser.getTwoFactorEnabled()).isTrue();
        verify(userRepository).save(testUser);

        verify(auditLogService).logTwoFactorSuccess(testUser, TEST_IP);
    }

    @Test
    void verifyAndEnableTwoFactor_WithInvalidCode_ShouldReturnFalse() {
        // Given
        String secret = "TESTSECRET123456";
        String code = "000000";

        TwoFactorSecret twoFactorSecret = TwoFactorSecret.builder()
                .user(testUser)
                .secret(secret)
                .build();

        when(twoFactorSecretRepository.findByUser(testUser)).thenReturn(Optional.of(twoFactorSecret));
        mockTotpService.validCode = false;

        // When
        boolean result = twoFactorAuthService.verifyAndEnableTwoFactor(testUser, code, TEST_IP);  

        // Then
        assertThat(result).isFalse();
        assertThat(testUser.getTwoFactorEnabled()).isFalse();
        verify(userRepository, never()).save(testUser);

        verify(auditLogService).logTwoFactorFailed(testUser, TEST_IP);
    }

    @Test
    void verifyAndEnableTwoFactor_WithoutSetup_ShouldThrowException() {
        // Given
        when(twoFactorSecretRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> twoFactorAuthService.verifyAndEnableTwoFactor(testUser, "123456", TEST_IP))  
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Two-factor setup not found");
    }

    @Test
    void verifyCode_WithValidTotpCode_ShouldReturnTrue() {
        // Given
        testUser.setTwoFactorEnabled(true);
        String secret = "TESTSECRET123456";
        String code = "123456";

        TwoFactorSecret twoFactorSecret = TwoFactorSecret.builder()
                .user(testUser)
                .secret(secret)
                .backupCodes("[\"12345678\",\"87654321\"]")
                .build();

        when(twoFactorSecretRepository.findByUser(testUser)).thenReturn(Optional.of(twoFactorSecret));
        mockTotpService.validCode = true;

        // When
        boolean result = twoFactorAuthService.verifyCode(testUser, code, TEST_IP);  

        // Then
        assertThat(result).isTrue();

        verify(auditLogService).logTwoFactorSuccess(testUser, TEST_IP);
    }

    @Test
    void verifyCode_WithValidBackupCode_ShouldReturnTrueAndRemoveCode() throws Exception {
        // Given
        testUser.setTwoFactorEnabled(true);
        String secret = "TESTSECRET123456";
        String backupCode = "12345678";

        TwoFactorSecret twoFactorSecret = TwoFactorSecret.builder()
                .id(1)
                .user(testUser)
                .secret(secret)
                .backupCodes("[\"12345678\",\"87654321\"]")
                .build();

        when(twoFactorSecretRepository.findByUser(testUser)).thenReturn(Optional.of(twoFactorSecret));
        mockTotpService.validCode = false; // TOTP fails

        ArgumentCaptor<TwoFactorSecret> captor = ArgumentCaptor.forClass(TwoFactorSecret.class);

        // When
        boolean result = twoFactorAuthService.verifyCode(testUser, backupCode, TEST_IP);  

        // Then
        assertThat(result).isTrue();

        verify(twoFactorSecretRepository).save(captor.capture());
        TwoFactorSecret saved = captor.getValue();

        // Verify backup code was removed - check the JSON doesn't contain it
        assertThat(saved.getBackupCodes()).doesNotContain("12345678");
        assertThat(saved.getBackupCodes()).contains("87654321");

        verify(auditLogService).createAuditLog(
                eq(testUser),
                eq("TWO_FACTOR_BACKUP_CODE_USED"),
                contains("1 codes remaining"),
                eq(TEST_IP)
        );
        verify(auditLogService).logTwoFactorSuccess(testUser, TEST_IP);
    }

    @Test
    void verifyCode_WithInvalidCode_ShouldReturnFalse() throws Exception {
        // Given
        testUser.setTwoFactorEnabled(true);
        String secret = "TESTSECRET123456";
        String code = "000000";

        TwoFactorSecret twoFactorSecret = TwoFactorSecret.builder()
                .user(testUser)
                .secret(secret)
                .backupCodes("[\"12345678\",\"87654321\"]")
                .build();

        when(twoFactorSecretRepository.findByUser(testUser)).thenReturn(Optional.of(twoFactorSecret));
        mockTotpService.validCode = false;

        // When
        boolean result = twoFactorAuthService.verifyCode(testUser, code, TEST_IP);  

        // Then
        assertThat(result).isFalse();

        verify(auditLogService).logTwoFactorFailed(testUser, TEST_IP);
    }

    @Test
    void disableTwoFactor_WithValidPassword_ShouldDisableAndRemoveSecret() {
        // Given
        testUser.setTwoFactorEnabled(true);
        String password = "password123";

        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(true);

        // When
        twoFactorAuthService.disableTwoFactor(testUser, password, TEST_IP);  

        // Then
        assertThat(testUser.getTwoFactorEnabled()).isFalse();
        verify(userRepository).save(testUser);
        verify(twoFactorSecretRepository).deleteByUser(testUser);

        verify(auditLogService).createAuditLog(
                eq(testUser),
                eq("TWO_FACTOR_DISABLED"),
                contains("disabled by user"),
                eq(TEST_IP)
        );
    }

    @Test
    void disableTwoFactor_WithInvalidPassword_ShouldThrowException() {
        // Given
        testUser.setTwoFactorEnabled(true);
        String wrongPassword = "wrongpassword";

        when(passwordEncoder.matches(wrongPassword, testUser.getPasswordHash())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> twoFactorAuthService.disableTwoFactor(testUser, wrongPassword, TEST_IP))  
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid password");

        assertThat(testUser.getTwoFactorEnabled()).isTrue();
        verify(userRepository, never()).save(any());
        verify(twoFactorSecretRepository, never()).deleteByUser(any());

        verify(auditLogService).createAuditLog(
                eq(testUser),
                eq("DISABLE_2FA_FAILED"),
                contains("Invalid password"),
                eq(TEST_IP)
        );
    }

    @Test
    void verifyCode_WhenNotConfigured_ShouldThrowException() {
        // Given
        when(twoFactorSecretRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> twoFactorAuthService.verifyCode(testUser, "123456", TEST_IP))  
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void setupTwoFactor_ShouldGenerateUniqueBackupCodes() throws Exception {
        // Given
        when(totpUtil.generateSecret()).thenReturn("SECRET1", "SECRET2");
        when(totpUtil.generateQrCodeUrl(anyString(), anyString())).thenReturn("QR_URL");

        // When
        TwoFactorSetupResponse response1 = twoFactorAuthService.setupTwoFactor(testUser, TEST_IP);  

        User anotherUser = User.builder()
                .id(2)
                .email("another@example.com")
                .name("Another User")
                .organization(testUser.getOrganization())
                .twoFactorEnabled(false)
                .build();

        TwoFactorSetupResponse response2 = twoFactorAuthService.setupTwoFactor(anotherUser, TEST_IP);  

        // Then - backup codes should be different
        assertThat(response1.getBackupCodes()).isNotEqualTo(response2.getBackupCodes());

        verify(auditLogService, times(2)).createAuditLog(
                any(User.class),
                eq("TWO_FACTOR_SETUP_INITIATED"),
                anyString(),
                eq(TEST_IP)
        );
    }

    @Test
    void regenerateBackupCodes_WithValidCode_ShouldGenerateNewCodes() {
        // Given
        testUser.setTwoFactorEnabled(true);
        String secret = "TESTSECRET123456";
        String verificationCode = "123456";

        TwoFactorSecret twoFactorSecret = TwoFactorSecret.builder()
                .user(testUser)
                .secret(secret)
                .backupCodes("[\"12345678\",\"87654321\"]")
                .build();

        when(twoFactorSecretRepository.findByUser(testUser)).thenReturn(Optional.of(twoFactorSecret));
        mockTotpService.validCode = true;

        ArgumentCaptor<TwoFactorSecret> captor = ArgumentCaptor.forClass(TwoFactorSecret.class);

        // When
        List<String> newBackupCodes = twoFactorAuthService.regenerateBackupCodes(testUser, verificationCode, TEST_IP);

        // Then
        assertThat(newBackupCodes).isNotNull().hasSize(10);
        newBackupCodes.forEach(code -> {
            assertThat(code).hasSize(8);
            assertThat(code).matches("\\d{8}");
        });

        verify(twoFactorSecretRepository).save(captor.capture());

        verify(auditLogService).createAuditLog(
                eq(testUser),
                eq("TWO_FACTOR_BACKUP_CODES_REGENERATED"),
                anyString(),
                eq(TEST_IP)
        );
    }

    @Test
    void regenerateBackupCodes_WithInvalidCode_ShouldThrowException() {
        // Given
        testUser.setTwoFactorEnabled(true);
        String secret = "TESTSECRET123456";
        String invalidCode = "000000";

        TwoFactorSecret twoFactorSecret = TwoFactorSecret.builder()
                .user(testUser)
                .secret(secret)
                .backupCodes("[\"12345678\",\"87654321\"]")
                .build();

        when(twoFactorSecretRepository.findByUser(testUser)).thenReturn(Optional.of(twoFactorSecret));
        mockTotpService.validCode = false;

        // When & Then
        assertThatThrownBy(() -> twoFactorAuthService.regenerateBackupCodes(testUser, invalidCode, TEST_IP))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid verification code");

        verify(twoFactorSecretRepository, never()).save(any());
        verify(auditLogService, never()).createAuditLog(any(), anyString(), anyString(), anyString());
    }
}
