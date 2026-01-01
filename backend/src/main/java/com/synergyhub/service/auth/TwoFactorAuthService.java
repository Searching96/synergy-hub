package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.TwoFactorSecret;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.response.TwoFactorSetupResponse;
import com.synergyhub.events.auth.*;
import com.synergyhub.exception.BadRequestException;
import com.synergyhub.repository.TwoFactorSecretRepository;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.service.security.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthService {

    private final TwoFactorSecretRepository twoFactorSecretRepository;
    private final UserRepository userRepository;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;
    private final BackupCodeService backupCodeService;
    private final ApplicationEventPublisher eventPublisher;
    private final RateLimitService rateLimitService;

    @Value("${security.two-factor.backup-codes-count:10}")
    private int backupCodesCount;

    @Value("${security.two-factor.backup-code-length:8}")
    private int backupCodeLength;

    @Transactional
    public TwoFactorSetupResponse setupTwoFactor(String email, String ipAddress) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadRequestException("User not found for email: " + email));
        
        if (user.getTwoFactorEnabled()) {
            throw new BadRequestException("Two-factor authentication is already enabled");
        }

        // Generate secret
        String secret = totpService.generateSecret();

        // Generate QR code
        String qrCodeUrl = totpService.generateQrCodeUrl(secret, user.getEmail());

        // Generate backup codes
        List<String> backupCodes = backupCodeService.generateBackupCodes(backupCodesCount, backupCodeLength);

        // Save secret and backup codes
        backupCodeService.setBackupCodes(user.getId().toString(), backupCodes);

        TwoFactorSecret twoFactorSecret = TwoFactorSecret.builder()
            .user(user)
            .secret(secret)
            .build();

        twoFactorSecretRepository.save(twoFactorSecret);

        // ✅ Publish event instead of calling auditLogService
        eventPublisher.publishEvent(new TwoFactorSetupInitiatedEvent(user, ipAddress));
        log.debug("Two-factor authentication setup initiated for user: {}", user.getEmail());

        return TwoFactorSetupResponse.builder()
            .secret(secret)
            .qrCodeUrl(qrCodeUrl)
            .backupCodes(backupCodes)
            .message("Scan the QR code with your authenticator app and verify with a code to enable 2FA")
            .build();
    }

    @Transactional
    public boolean verifyAndEnableTwoFactor(String email, String code, String ipAddress) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found for email: " + email));
        
        TwoFactorSecret secret = twoFactorSecretRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Two-factor setup not found. Please setup 2FA first."));

        boolean isValid = totpService.verifyCode(secret.getSecret(), code);

        if (isValid) {
            user.setTwoFactorEnabled(true);
            userRepository.save(user);

            // ✅ Publish success event
            eventPublisher.publishEvent(new TwoFactorSuccessEvent(user, ipAddress));
            log.info("Two-factor authentication enabled for user: {}", user.getEmail());
            return true;
        }

        // ✅ Publish failure event
        eventPublisher.publishEvent(new TwoFactorFailedEvent(user, ipAddress));
        log.warn("Failed to enable 2FA for user: {} - Invalid code", user.getEmail());
        return false;
    }

    @Transactional
    public boolean verifyCode(String email, String code, String ipAddress) {
        // SECURITY: Rate limit 2FA attempts to prevent brute force
        rateLimitService.check2FAAttempt(email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found for email: " + email));
        
        TwoFactorSecret secret = twoFactorSecretRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Two-factor authentication not configured"));

        // Try TOTP code first
        boolean isValid = totpService.verifyCode(secret.getSecret(), code);

        if (!isValid) {
            // Try backup codes
            isValid = verifyAndConsumeBackupCode(secret, code, ipAddress);
        }
        
        if (isValid) {
            // Clear rate limit on successful verification
            rateLimitService.clear2FAAttempts(email);
        } else {
            // Record failed attempt
            rateLimitService.recordFailed2FAAttempt(email);
        }

        return isValid;
    }

    @Transactional
    public void disableTwoFactor(String email, String password, String ipAddress) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found for email: " + email));
        
        // Verify password before disabling 2FA
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            // ✅ Publish disable failed event
            eventPublisher.publishEvent(new TwoFactorDisableFailedEvent(user, ipAddress));
            throw new BadRequestException("Invalid password");
        }

        if (!user.getTwoFactorEnabled()) {
            throw new BadRequestException("Two-factor authentication is not enabled");
        }

        user.setTwoFactorEnabled(false);
        userRepository.save(user);
        twoFactorSecretRepository.deleteByUser(user);

        // ✅ Publish disabled event
        eventPublisher.publishEvent(new TwoFactorDisabledEvent(user, ipAddress));
        log.info("Two-factor authentication disabled for user: {}", user.getEmail());
    }

    @Transactional
    public List<String> regenerateBackupCodes(String email, String verificationCode, String ipAddress) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found for email: " + email));
        
        if (!user.getTwoFactorEnabled()) {
            throw new BadRequestException("Two-factor authentication is not enabled");
        }

        // Verify the user with current TOTP code
        TwoFactorSecret secret = twoFactorSecretRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Two-factor secret not found"));

        if (!totpService.verifyCode(secret.getSecret(), verificationCode)) {
            throw new BadRequestException("Invalid verification code");
        }

        // Generate new backup codes
        List<String> newBackupCodes = backupCodeService.generateBackupCodes(backupCodesCount, backupCodeLength);
        backupCodeService.setBackupCodes(user.getId().toString(), newBackupCodes);

        // ✅ Publish backup code regenerated event
        eventPublisher.publishEvent(new TwoFactorBackupCodeRegeneratedEvent(user, ipAddress));
        log.info("Backup codes regenerated for user: {}", user.getEmail());

        return newBackupCodes;
    }

    boolean verifyAndConsumeBackupCode(TwoFactorSecret secret, String code, String ipAddress) {
        boolean isValid = backupCodeService.verifyBackupCode(secret.getUser().getId().toString(), code);
        
        if (isValid) {
            backupCodeService.consumeBackupCode(secret.getUser().getId().toString(), code);
            
            // ✅ Publish backup code used event
            eventPublisher.publishEvent(
                new TwoFactorBackupCodeUsedEvent(secret.getUser(), ipAddress)
            );
            
            log.info("Backup code used for user: {}", secret.getUser().getEmail());
        }
        
        return isValid;
    }
}