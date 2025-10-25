package com.synergyhub.service.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synergyhub.domain.entity.TwoFactorSecret;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.response.TwoFactorSetupResponse;
import com.synergyhub.exception.BadRequestException;
import com.synergyhub.repository.TwoFactorSecretRepository;
import com.synergyhub.repository.UserRepository;
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

    private static final int BACKUP_CODES_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 8;

    @Transactional
    public TwoFactorSetupResponse setupTwoFactor(User user) {
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

        log.info("Two-factor authentication setup initiated for user: {}", user.getEmail());

        return TwoFactorSetupResponse.builder()
                .secret(secret)
                .qrCodeUrl(qrCodeUrl)
                .backupCodes(backupCodes)
                .message("Scan the QR code with your authenticator app and verify with a code to enable 2FA")
                .build();
    }

    @Transactional
    public boolean verifyAndEnableTwoFactor(User user, String code) {
        TwoFactorSecret secret = twoFactorSecretRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Two-factor setup not found. Please setup 2FA first."));

        boolean isValid = totpUtil.verifyCode(secret.getSecret(), code);

        if (isValid) {
            user.setTwoFactorEnabled(true);
            userRepository.save(user);
            log.info("Two-factor authentication enabled for user: {}", user.getEmail());
            return true;
        }
        log.warn("Failed to enable 2FA for user: {} - Invalid code", user.getEmail());
        return false;
    }

    @Transactional
    public boolean verifyCode(User user, String code) {
        TwoFactorSecret secret = twoFactorSecretRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Two-factor authentication not configured"));

        // Try TOTP code first
        boolean isValid = totpUtil.verifyCode(secret.getSecret(), code);

        if (!isValid) {
            // Try backup codes
            isValid = verifyAndConsumeBackupCode(secret, code);
        }

        return isValid;
    }

    @Transactional
    public void disableTwoFactor(User user, String password) {
        // Verify password before disabling 2FA
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadRequestException("Invalid password");
        }

        user.setTwoFactorEnabled(false);
        userRepository.save(user);
        twoFactorSecretRepository.deleteByUser(user);
        log.info("Two-factor authentication disabled for user: {}", user.getEmail());
    }

    @Transactional
    public void disableTwoFactor(User user) {
        user.setTwoFactorEnabled(false);
        userRepository.save(user);
        twoFactorSecretRepository.deleteByUser(user);
        log.info("Two-factor authentication disabled for user: {}", user.getEmail());
    }

    @Transactional
    public List<String> regenerateBackupCodes(User user, String verificationCode) {
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

        log.info("Backup codes regenerated for user: {}", user.getEmail());

        return newBackupCodes;
    }

    // Changed from private to package-private (or protected) and removed @Transactional
    // The transaction will be managed by the calling method
    boolean verifyAndConsumeBackupCode(TwoFactorSecret secret, String code) {
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
                log.info("Backup code used for user: {}", secret.getUser().getEmail());
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