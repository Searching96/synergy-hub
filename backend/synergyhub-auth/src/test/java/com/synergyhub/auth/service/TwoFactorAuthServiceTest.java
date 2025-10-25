package com.synergyhub.auth.service;

import com.synergyhub.auth.dto.TwoFactorSetupResponse;
import com.synergyhub.auth.dto.TwoFactorVerifyRequest;
import com.synergyhub.auth.entity.Role;
import com.synergyhub.auth.entity.TwoFactorSecret;
import com.synergyhub.auth.entity.User;
import com.synergyhub.auth.repository.TwoFactorSecretRepository;
import com.synergyhub.auth.repository.UserRepository;
import com.synergyhub.auth.security.CustomUserDetails;
import com.synergyhub.auth.util.TOTPUtil;
import com.synergyhub.common.exception.InvalidTwoFactorCodeException;
import com.synergyhub.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoFactorAuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TwoFactorSecretRepository twoFactorSecretRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private TwoFactorAuthService twoFactorAuthService;

    private User testUser;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        Role role = Role.builder()
                .roleId(1)
                .name("Team Member")
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
                .roles(Set.of(role))
                .build();

        userDetails = new CustomUserDetails(testUser);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
    }

    @Test
    void setup2FA_ShouldGenerateSecretAndQRCode() {
        // Arrange
        when(twoFactorSecretRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TwoFactorSetupResponse response = twoFactorAuthService.setup2FA();

        // Assert
        assertNotNull(response);
        assertNotNull(response.getSecret());
        assertNotNull(response.getQrCodeUrl());
        assertNotNull(response.getBackupCodes());
        assertEquals(10, response.getBackupCodes().size());
        verify(twoFactorSecretRepository).save(any(TwoFactorSecret.class));
        verify(auditLogService).log(eq(testUser), eq("2FA_SETUP_INITIATED"), anyString(), isNull(), isNull());
    }

    @Test
    void setup2FA_WhenAlreadyEnabled_ShouldThrowException() {
        // Arrange
        testUser.setTwoFactorEnabled(true);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> twoFactorAuthService.setup2FA());
        verify(twoFactorSecretRepository, never()).save(any());
    }

    @Test
    void verify2FASetup_WithValidCode_ShouldEnable2FA() {
        // Arrange
        String secret = TOTPUtil.generateSecret();
        TwoFactorSecret twoFactorSecret = TwoFactorSecret.builder()
                .secretId(1)
                .user(testUser)
                .secret(secret)
                .backupCodes("[]")
                .build();

        when(twoFactorSecretRepository.findByUser(testUser)).thenReturn(Optional.of(twoFactorSecret));

        try (MockedStatic<TOTPUtil> totpUtil = mockStatic(TOTPUtil.class)) {
            totpUtil.when(() -> TOTPUtil.verifyCode(anyString(), anyString())).thenReturn(true);

            TwoFactorVerifyRequest request = new TwoFactorVerifyRequest("123456");

            // Act
            twoFactorAuthService.verify2FASetup(request);

            // Assert
            assertTrue(testUser.getTwoFactorEnabled());
            verify(userRepository).save(testUser);
            verify(auditLogService).log(eq(testUser), eq("2FA_ENABLED"), anyString(), isNull(), isNull());
        }
    }

    @Test
    void verify2FASetup_WithInvalidCode_ShouldThrowException() {
        // Arrange
        String secret = TOTPUtil.generateSecret();
        TwoFactorSecret twoFactorSecret = TwoFactorSecret.builder()
                .secretId(1)
                .user(testUser)
                .secret(secret)
                .backupCodes("[]")
                .build();

        when(twoFactorSecretRepository.findByUser(testUser)).thenReturn(Optional.of(twoFactorSecret));

        try (MockedStatic<TOTPUtil> totpUtil = mockStatic(TOTPUtil.class)) {
            totpUtil.when(() -> TOTPUtil.verifyCode(anyString(), anyString())).thenReturn(false);

            TwoFactorVerifyRequest request = new TwoFactorVerifyRequest("000000");

            // Act & Assert
            assertThrows(InvalidTwoFactorCodeException.class, () ->
                    twoFactorAuthService.verify2FASetup(request));

            assertFalse(testUser.getTwoFactorEnabled());
            verify(auditLogService).log(eq(testUser), eq("2FA_VERIFICATION_FAILED"), anyString(), isNull(), isNull());
        }
    }

    @Test
    void disable2FA_WithValidCode_ShouldDisable2FA() {
        // Arrange
        testUser.setTwoFactorEnabled(true);
        String secret = TOTPUtil.generateSecret();
        TwoFactorSecret twoFactorSecret = TwoFactorSecret.builder()
                .secretId(1)
                .user(testUser)
                .secret(secret)
                .backupCodes("[]")
                .build();

        when(twoFactorSecretRepository.findByUser(testUser)).thenReturn(Optional.of(twoFactorSecret));

        try (MockedStatic<TOTPUtil> totpUtil = mockStatic(TOTPUtil.class)) {
            totpUtil.when(() -> TOTPUtil.verifyCode(anyString(), anyString())).thenReturn(true);
            totpUtil.when(() -> TOTPUtil.verifyBackupCode(anyString(), anyString())).thenReturn(false);

            TwoFactorVerifyRequest request = new TwoFactorVerifyRequest("123456");

            // Act
            twoFactorAuthService.disable2FA(request);

            // Assert
            assertFalse(testUser.getTwoFactorEnabled());
            verify(userRepository).save(testUser);
            verify(twoFactorSecretRepository).delete(twoFactorSecret);
            verify(auditLogService).log(eq(testUser), eq("2FA_DISABLED"), anyString(), isNull(), isNull());
        }
    }

    @Test
    void disable2FA_WhenNotEnabled_ShouldThrowException() {
        // Arrange
        testUser.setTwoFactorEnabled(false);
        TwoFactorVerifyRequest request = new TwoFactorVerifyRequest("123456");

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> twoFactorAuthService.disable2FA(request));
    }

    @Test
    void getBackupCodes_ShouldReturnCodes() {
        // Arrange
        List<String> expectedCodes = List.of("12345678", "87654321");
        TwoFactorSecret twoFactorSecret = TwoFactorSecret.builder()
                .secretId(1)
                .user(testUser)
                .secret("secret")
                .backupCodes(TOTPUtil.backupCodesToJson(expectedCodes))
                .build();

        when(twoFactorSecretRepository.findByUser(testUser)).thenReturn(Optional.of(twoFactorSecret));

        // Act
        List<String> codes = twoFactorAuthService.getBackupCodes();

        // Assert
        assertEquals(2, codes.size());
        assertTrue(codes.contains("12345678"));
        assertTrue(codes.contains("87654321"));
    }

    @Test
    void getBackupCodes_WhenNotConfigured_ShouldThrowException() {
        // Arrange
        when(twoFactorSecretRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> twoFactorAuthService.getBackupCodes());
    }

    @Test
    void regenerateBackupCodes_WithValidCode_ShouldGenerateNewCodes() {
        // Arrange
        String secret = TOTPUtil.generateSecret();
        TwoFactorSecret twoFactorSecret = TwoFactorSecret.builder()
                .secretId(1)
                .user(testUser)
                .secret(secret)
                .backupCodes("[]")
                .build();

        when(twoFactorSecretRepository.findByUser(testUser)).thenReturn(Optional.of(twoFactorSecret));
        when(twoFactorSecretRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<TOTPUtil> totpUtil = mockStatic(TOTPUtil.class)) {
            totpUtil.when(() -> TOTPUtil.verifyCode(anyString(), anyString())).thenReturn(true);
            totpUtil.when(TOTPUtil::generateBackupCodes).thenReturn(List.of("11111111", "22222222"));
            totpUtil.when(() -> TOTPUtil.backupCodesToJson(any())).thenCallRealMethod();

            TwoFactorVerifyRequest request = new TwoFactorVerifyRequest("123456");

            // Act
            List<String> codes = twoFactorAuthService.regenerateBackupCodes(request);

            // Assert
            assertNotNull(codes);
            verify(twoFactorSecretRepository).save(twoFactorSecret);
            verify(auditLogService).log(eq(testUser), eq("2FA_BACKUP_CODES_REGENERATED"), anyString(), isNull(), isNull());
        }
    }
}