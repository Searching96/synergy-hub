package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.PasswordResetToken;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.PasswordResetConfirmRequest;
import com.synergyhub.dto.request.PasswordResetRequest;
import com.synergyhub.exception.BadRequestException;
import com.synergyhub.exception.InvalidTokenException;
import com.synergyhub.repository.PasswordResetTokenRepository;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.repository.UserSessionRepository;
import com.synergyhub.service.security.AuditLogService;
import com.synergyhub.util.EmailService;
import com.synergyhub.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Value("${security.password-reset-token-expiry-minutes}")
    private int resetTokenExpiryMinutes;

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request, String ipAddress) {
        log.info("Password reset requested for email: {}", request.getEmail());

        // Find user by email
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        // Don't reveal if user exists or not (security best practice)
        if (userOptional.isEmpty()) {
            log.warn("Password reset requested for non-existent email: {}", request.getEmail());
            return;
        }

        User user = userOptional.get();

        // Invalidate all existing reset tokens for this user
        passwordResetTokenRepository.invalidateAllUserTokens(user);

        // Generate new reset token
        String token = generateResetToken();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .used(false)
                .expiryTime(LocalDateTime.now().plusMinutes(resetTokenExpiryMinutes))
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Send reset email
        emailService.sendPasswordResetEmail(user.getEmail(), token);

        // Audit log
        auditLogService.logPasswordResetRequested(user, ipAddress);

        log.info("Password reset email sent to: {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(PasswordResetConfirmRequest request, String ipAddress) {
        log.info("Password reset confirmation received");

        // Validate token
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token"));

        if (resetToken.getUsed()) {
            throw new InvalidTokenException("This password reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new InvalidTokenException("Password reset token has expired. Please request a new one.");
        }

        // Validate new password
        if (!passwordValidator.isValid(request.getNewPassword())) {
            throw new BadRequestException("Password does not meet requirements: " +
                    passwordValidator.getRequirements());
        }

        User user = resetToken.getUser();

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Revoke all active sessions (security measure - user must login again)
        userSessionRepository.revokeAllUserSessions(user);

        // Audit log
        auditLogService.logPasswordResetCompleted(user, ipAddress);

        log.info("Password reset completed successfully for user: {}", user.getEmail());
    }

    @Transactional(readOnly = true)
    public boolean validateResetToken(String token) {
        Optional<PasswordResetToken> resetToken = passwordResetTokenRepository.findByToken(token);

        if (resetToken.isEmpty()) {
            return false;
        }

        return resetToken.get().isValid();
    }

    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        passwordResetTokenRepository.deleteExpiredTokens(now);
        log.info("Cleaned up expired password reset tokens");
    }

    private String generateResetToken() {
        return UUID.randomUUID().toString();
    }
}