package com.synergyhub.auth.service;

import com.synergyhub.auth.dto.TwoFactorSetupResponse;
import com.synergyhub.auth.dto.TwoFactorVerifyRequest;
import com.synergyhub.auth.entity.TwoFactorSecret;
import com.synergyhub.auth.entity.User;
import com.synergyhub.auth.repository.TwoFactorSecretRepository;
import com.synergyhub.auth.repository.UserRepository;
import com.synergyhub.auth.security.CustomUserDetails;
import com.synergyhub.auth.util.TOTPUtil;
import com.synergyhub.common.exception.InvalidTwoFactorCodeException;
import com.synergyhub.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthService {

    private final UserRepository userRepository;
    private final TwoFactorSecretRepository twoFactorSecretRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public TwoFactorSetupResponse setup2FA() {
        User user = getCurrentUser();

        if (user.getTwoFactorEnabled()) {
            throw new IllegalStateException("2FA is already enabled for this user");
        }

        // Generate secret
        String secret = TOTPUtil.generateSecret();
        
        // Generate backup codes
        List<String> backupCodes = TOTPUtil.generateBackupCodes();
        String backupCodesJson = TOTPUtil.backupCodesToJson(backupCodes);

        // Save secret (not activated yet)
        TwoFactorSecret twoFactorSecret = TwoFactorSecret.builder()
            .user(user)
            .secret(secret)
            .backupCodes(backupCodesJson)
            .build();
        
        twoFactorSecretRepository.save(twoFactorSecret);

        // Generate QR code URL
        String qrCodeUrl = TOTPUtil.generateQRCodeUrl(user.getEmail(), secret, "SynergyHub");

        auditLogService.log(user, "2FA_SETUP_INITIATED", "2FA setup initiated", null, null);

        log.info("2FA setup initiated for user: {}", user.getEmail());

        return TwoFactorSetupResponse.builder()
            .secret(secret)
            .qrCodeUrl(qrCodeUrl)
            .backupCodes(backupCodes)
            .build();
    }

    @Transactional
    public void verify2FASetup(TwoFactorVerifyRequest request) {
        User user = getCurrentUser();

        TwoFactorSecret secret = twoFactorSecretRepository.findByUser(user)
            .orElseThrow(() -> new ResourceNotFoundException("2FA setup not found. Please initiate setup first."));

        // Verify code
        if (!TOTPUtil.verifyCode(secret.getSecret(), request.getCode())) {
            auditLogService.log(user, "2FA_VERIFICATION_FAILED", "Invalid 2FA code during setup", null, null);
            throw new InvalidTwoFactorCodeException("Invalid verification code");
        }

        // Enable 2FA for user
        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        auditLogService.log(user, "2FA_ENABLED", "2FA successfully enabled", null, null);

        log.info("2FA enabled for user: {}", user.getEmail());
    }

    @Transactional
    public void disable2FA(TwoFactorVerifyRequest request) {
        User user = getCurrentUser();

        if (!user.getTwoFactorEnabled()) {
            throw new IllegalStateException("2FA is not enabled for this user");
        }

        TwoFactorSecret secret = twoFactorSecretRepository.findByUser(user)
            .orElseThrow(() -> new ResourceNotFoundException("2FA secret not found"));

        // Verify code before disabling
        if (!TOTPUtil.verifyCode(secret.getSecret(), request.getCode()) &&
            !TOTPUtil.verifyBackupCode(secret.getBackupCodes(), request.getCode())) {
            auditLogService.log(user, "2FA_DISABLE_FAILED", "Invalid code when attempting to disable 2FA", null, null);
            throw new InvalidTwoFactorCodeException("Invalid verification code");
        }

        // Disable 2FA
        user.setTwoFactorEnabled(false);
        userRepository.save(user);

        // Delete secret
        twoFactorSecretRepository.delete(secret);

        auditLogService.log(user, "2FA_DISABLED", "2FA disabled", null, null);

        log.info("2FA disabled for user: {}", user.getEmail());
    }

    @Transactional(readOnly = true)
    public List<String> getBackupCodes() {
        User user = getCurrentUser();

        TwoFactorSecret secret = twoFactorSecretRepository.findByUser(user)
            .orElseThrow(() -> new ResourceNotFoundException("2FA not configured"));

        return TOTPUtil.jsonToBackupCodes(secret.getBackupCodes());
    }

    @Transactional
    public List<String> regenerateBackupCodes(TwoFactorVerifyRequest request) {
        User user = getCurrentUser();

        TwoFactorSecret secret = twoFactorSecretRepository.findByUser(user)
            .orElseThrow(() -> new ResourceNotFoundException("2FA not configured"));

        // Verify current code
        if (!TOTPUtil.verifyCode(secret.getSecret(), request.getCode())) {
            throw new InvalidTwoFactorCodeException("Invalid verification code");
        }

        // Generate new backup codes
        List<String> newBackupCodes = TOTPUtil.generateBackupCodes();
        secret.setBackupCodes(TOTPUtil.backupCodesToJson(newBackupCodes));
        twoFactorSecretRepository.save(secret);

        auditLogService.log(user, "2FA_BACKUP_CODES_REGENERATED", "Backup codes regenerated", null, null);

        log.info("Backup codes regenerated for user: {}", user.getEmail());

        return newBackupCodes;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userRepository.findById(userDetails.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}