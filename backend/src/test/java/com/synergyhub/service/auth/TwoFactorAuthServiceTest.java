package com.synergyhub.service.auth;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.TwoFactorSecret;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.response.TwoFactorSetupResponse;
import com.synergyhub.exception.BadRequestException;
import com.synergyhub.repository.TwoFactorSecretRepository;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.util.TotpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoFactorAuthServiceTest {

    @Mock
    private TwoFactorSecretRepository twoFactorSecretRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TotpUtil totpUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    private TwoFactorAuthService twoFactorAuthService;

    private ObjectMapper objectMapper;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create real ObjectMapper
        objectMapper = new ObjectMapper();

        // Manually create the service with dependencies
        twoFactorAuthService = new TwoFactorAuthService(
                twoFactorSecretRepository,
                userRepository,
                totpUtil,
                objectMapper,
                passwordEncoder
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

        when(totpUtil.generateSecret()).thenReturn(secret);
        when(totpUtil.generateQrCodeUrl(secret, testUser.getEmail())).thenReturn(qrCodeUrl);

        ArgumentCaptor<TwoFactorSecret> captor = ArgumentCaptor.forClass(TwoFactorSecret.class);

        // When
        TwoFactorSetupResponse response = twoFactorAuthService.setupTwoFactor(testUser);

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
    }

    @Test
    void setupTwoFactor_WhenAlreadyEnabled_ShouldThrowException() {
        // Given
        testUser.setTwoFactorEnabled(true);

        // When & Then
        assertThatThrownBy(() -> twoFactorAuthService.setupTwoFactor(testUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already enabled");

        verify(twoFactorSecretRepository, never()).save(any());
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
        when(totpUtil.verifyCode(secret, code)).thenReturn(true);

        // When
        boolean result = twoFactorAuthService.verifyAndEnableTwoFactor(testUser, code);

        // Then
        assertThat(result).isTrue();
        assertThat(testUser.getTwoFactorEnabled()).isTrue();
        verify(userRepository).save(testUser);
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
        when(totpUtil.verifyCode(secret, code)).thenReturn(false);

        // When
        boolean result = twoFactorAuthService.verifyAndEnableTwoFactor(testUser, code);

        // Then
        assertThat(result).isFalse();
        assertThat(testUser.getTwoFactorEnabled()).isFalse();
        verify(userRepository, never()).save(testUser);
    }

    @Test
    void verifyAndEnableTwoFactor_WithoutSetup_ShouldThrowException() {
        // Given
        when(twoFactorSecretRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> twoFactorAuthService.verifyAndEnableTwoFactor(testUser, "123456"))
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
        when(totpUtil.verifyCode(secret, code)).thenReturn(true);

        // When
        boolean result = twoFactorAuthService.verifyCode(testUser, code);

        // Then
        assertThat(result).isTrue();
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
        when(totpUtil.verifyCode(secret, backupCode)).thenReturn(false); // TOTP fails

        ArgumentCaptor<TwoFactorSecret> captor = ArgumentCaptor.forClass(TwoFactorSecret.class);

        // When
        boolean result = twoFactorAuthService.verifyCode(testUser, backupCode);

        // Then
        assertThat(result).isTrue();

        verify(twoFactorSecretRepository).save(captor.capture());
        TwoFactorSecret saved = captor.getValue();

        // Verify backup code was removed - check the JSON doesn't contain it
        assertThat(saved.getBackupCodes()).doesNotContain("12345678");
        assertThat(saved.getBackupCodes()).contains("87654321");
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
        when(totpUtil.verifyCode(secret, code)).thenReturn(false);

        // When
        boolean result = twoFactorAuthService.verifyCode(testUser, code);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void disableTwoFactor_WithValidPassword_ShouldDisableAndRemoveSecret() {
        // Given
        testUser.setTwoFactorEnabled(true);
        String password = "password123";

        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(true);

        // When
        twoFactorAuthService.disableTwoFactor(testUser, password);

        // Then
        assertThat(testUser.getTwoFactorEnabled()).isFalse();
        verify(userRepository).save(testUser);
        verify(twoFactorSecretRepository).deleteByUser(testUser);
    }

    @Test
    void disableTwoFactor_WithInvalidPassword_ShouldThrowException() {
        // Given
        testUser.setTwoFactorEnabled(true);
        String wrongPassword = "wrongpassword";

        when(passwordEncoder.matches(wrongPassword, testUser.getPasswordHash())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> twoFactorAuthService.disableTwoFactor(testUser, wrongPassword))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid password");

        assertThat(testUser.getTwoFactorEnabled()).isTrue();
        verify(userRepository, never()).save(any());
        verify(twoFactorSecretRepository, never()).deleteByUser(any());
    }

    @Test
    void verifyCode_WhenNotConfigured_ShouldThrowException() {
        // Given
        when(twoFactorSecretRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> twoFactorAuthService.verifyCode(testUser, "123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void setupTwoFactor_ShouldGenerateUniqueBackupCodes() throws Exception {
        // Given
        when(totpUtil.generateSecret()).thenReturn("SECRET1", "SECRET2");
        when(totpUtil.generateQrCodeUrl(anyString(), anyString())).thenReturn("QR_URL");

        // When
        TwoFactorSetupResponse response1 = twoFactorAuthService.setupTwoFactor(testUser);

        User anotherUser = User.builder()
                .id(2)
                .email("another@example.com")
                .name("Another User")
                .organization(testUser.getOrganization())
                .twoFactorEnabled(false)
                .build();

        TwoFactorSetupResponse response2 = twoFactorAuthService.setupTwoFactor(anotherUser);

        // Then - backup codes should be different
        assertThat(response1.getBackupCodes()).isNotEqualTo(response2.getBackupCodes());
    }
}