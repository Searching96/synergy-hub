package com.synergyhub.service.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synergyhub.domain.entity.TwoFactorSecret;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.response.TwoFactorSetupResponse;
import com.synergyhub.exception.BadRequestException;
import com.synergyhub.repository.TwoFactorSecretRepository;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.service.security.AuditLogService;
import com.synergyhub.util.TotpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthService {

    private final TwoFactorSecretRepository twoFactorSecretRepository;
    private final UserRepository userRepository;
    private final TotpUtil totpUtil;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;  // ✅ Added

    private static final int BACKUP_CODES_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 8;

    @Transactional
    public TwoFactorSetupResponse setupTwoFactor(User user, String ipAddress) {
        if (user.getTwoFactorEnabled()) {
            throw new BadRequestException("Two-factor authentication is already enabled");
        }

        // Generate secret
        String secret = totpUtil.generateSecret();

        // Generate QR code
        String qrCodeUrl = totpUtil.generateQrCodeUrl(secret, user.getEmail());

        // Generate backup codes
        List<String> backupCodes = generateBackupCodes();

        // Save secret and backup codes
        String backupCodesJson = convertBackupCodesToJson(backupCodes);

        TwoFactorSecret twoFactorSecret = TwoFactorSecret.builder()
                .user(user)
                .secret(secret)
                .backupCodes(backupCodesJson)
                .build();

        twoFactorSecretRepository.save(twoFactorSecret);

        // ✅ Audit log instead of regular log
        auditLogService.createAuditLog(
                user,
                "TWO_FACTOR_SETUP_INITIATED",
                "Two-factor authentication setup initiated",
                ipAddress
        );
        log.debug("Two-factor authentication setup initiated for user: {}", user.getEmail());

        return TwoFactorSetupResponse.builder()
                .secret(secret)
                .qrCodeUrl(qrCodeUrl)
                .backupCodes(backupCodes)
                .message("Scan the QR code with your authenticator app and verify with a code to enable 2FA")
                .build();
    }

    @Transactional
    public boolean verifyAndEnableTwoFactor(User user, String code, String ipAddress) {
        TwoFactorSecret secret = twoFactorSecretRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Two-factor setup not found. Please setup 2FA first."));

        boolean isValid = totpUtil.verifyCode(secret.getSecret(), code);

        if (isValid) {
            user.setTwoFactorEnabled(true);
            userRepository.save(user);

            // ✅ Audit log for success
            auditLogService.logTwoFactorSuccess(user, ipAddress);
            log.info("Two-factor authentication enabled for user: {}", user.getEmail());
            return true;
        }

        // ✅ Audit log for failure
        auditLogService.logTwoFactorFailed(user, ipAddress);
        log.warn("Failed to enable 2FA for user: {} - Invalid code", user.getEmail());
        return false;
    }

    @Transactional
    public boolean verifyCode(User user, String code, String ipAddress) {
        TwoFactorSecret secret = twoFactorSecretRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Two-factor authentication not configured"));

        // Try TOTP code first
        boolean isValid = totpUtil.verifyCode(secret.getSecret(), code);

        if (!isValid) {
            // Try backup codes
            isValid = verifyAndConsumeBackupCode(secret, code, ipAddress);
        }

        // ✅ Audit log based on result
        if (isValid) {
            auditLogService.logTwoFactorSuccess(user, ipAddress);
        } else {
            auditLogService.logTwoFactorFailed(user, ipAddress);
        }

        return isValid;
    }

    @Transactional
    public void disableTwoFactor(User user, String password, String ipAddress) {
        // Verify password before disabling 2FA
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            auditLogService.createAuditLog(
                    user,
                    "DISABLE_2FA_FAILED",
                    "Failed to disable 2FA: Invalid password",
                    ipAddress
            );
            throw new BadRequestException("Invalid password");  // ✅ Throws BadRequestException
        }

        if (!user.getTwoFactorEnabled()) {
            throw new BadRequestException("Two-factor authentication is not enabled");
        }

        user.setTwoFactorEnabled(false);
        userRepository.save(user);
        twoFactorSecretRepository.deleteByUser(user);

        // ✅ Audit log
        auditLogService.createAuditLog(
                user,
                "TWO_FACTOR_DISABLED",
                "Two-factor authentication disabled by user",
                ipAddress
        );
        log.info("Two-factor authentication disabled for user: {}", user.getEmail());
    }

    @Transactional
    public void disableTwoFactor(User user, String ipAddress) {
        user.setTwoFactorEnabled(false);
        userRepository.save(user);
        twoFactorSecretRepository.deleteByUser(user);

        // ✅ Audit log
        auditLogService.createAuditLog(
                user,
                "TWO_FACTOR_DISABLED",
                "Two-factor authentication disabled by admin",
                ipAddress
        );
        log.info("Two-factor authentication disabled for user: {}", user.getEmail());
    }

    @Transactional
    public List<String> regenerateBackupCodes(User user, String verificationCode, String ipAddress) {
        if (!user.getTwoFactorEnabled()) {
            throw new BadRequestException("Two-factor authentication is not enabled");
        }

        // Verify the user with current TOTP code
        TwoFactorSecret secret = twoFactorSecretRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Two-factor secret not found"));

        if (!totpUtil.verifyCode(secret.getSecret(), verificationCode)) {
            throw new BadRequestException("Invalid verification code");
        }

        // Generate new backup codes
        List<String> newBackupCodes = generateBackupCodes();
        secret.setBackupCodes(convertBackupCodesToJson(newBackupCodes));
        twoFactorSecretRepository.save(secret);

        // ✅ Audit log
        auditLogService.createAuditLog(
                user,
                "TWO_FACTOR_BACKUP_CODES_REGENERATED",
                "Two-factor backup codes regenerated",
                ipAddress
        );
        log.info("Backup codes regenerated for user: {}", user.getEmail());

        return newBackupCodes;
    }

    boolean verifyAndConsumeBackupCode(TwoFactorSecret secret, String code, String ipAddress) {
        try {
            List<String> backupCodes = objectMapper.readValue(
                    secret.getBackupCodes(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );

            boolean isValid = backupCodes.contains(code);

            if (isValid) {
                // Remove used backup code
                backupCodes.remove(code);
                secret.setBackupCodes(objectMapper.writeValueAsString(backupCodes));
                twoFactorSecretRepository.save(secret);

                // ✅ Audit log
                auditLogService.createAuditLog(
                        secret.getUser(),
                        "TWO_FACTOR_BACKUP_CODE_USED",
                        String.format("Two-factor backup code used. %d codes remaining", backupCodes.size()),
                        ipAddress
                );
                log.info("Backup code used for user: {} ({} codes remaining)",
                        secret.getUser().getEmail(), backupCodes.size());
            }

            return isValid;
        } catch (JsonProcessingException e) {
            log.error("Failed to process backup codes", e);
            return false;
        }
    }

    private List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < BACKUP_CODES_COUNT; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < BACKUP_CODE_LENGTH; j++) {
                code.append(random.nextInt(10));
            }
            codes.add(code.toString());
        }

        return codes;
    }

    private String convertBackupCodesToJson(List<String> backupCodes) {
        try {
            return objectMapper.writeValueAsString(backupCodes);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert backup codes to JSON", e);
            throw new RuntimeException("Failed to generate backup codes", e);
        }
    }
}
