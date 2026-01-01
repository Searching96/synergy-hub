package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.PasswordResetToken;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.PasswordResetConfirmRequest;
import com.synergyhub.dto.request.PasswordResetRequest;
import com.synergyhub.events.auth.PasswordResetCompletedEvent;
import com.synergyhub.events.auth.PasswordResetRequestedEvent;
import com.synergyhub.exception.InvalidTokenException;
import com.synergyhub.repository.PasswordResetTokenRepository;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.repository.UserSessionRepository;
import com.synergyhub.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
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
    private final PasswordValidator passwordValidator;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionRepository userSessionRepository;
    private final ApplicationEventPublisher eventPublisher; // ✅ Only event publisher needed

    @Value("${security.password-reset-token-expiry-minutes}")
    private int resetTokenExpiryMinutes;

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request, String ipAddress) {
        log.info("Password reset requested for email: {}", request.getEmail());

        // Find user by email
        userRepository.findByEmail(request.getEmail()).ifPresentOrElse(user -> {
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

            // ✅ Publish single event - handles both email sending AND audit logging
            eventPublisher.publishEvent(new PasswordResetRequestedEvent(user, token, ipAddress));

            log.info("Password reset email sent to: {}", user.getEmail());
        }, () -> {
            log.warn("Password reset requested for non-existent email: {}", request.getEmail());
            // Do nothing else for security (prevent email enumeration)
        });
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

        // Validate and encode new password
        User user = resetToken.getUser();
        String newPassword = request.getNewPassword();
        
        if (!passwordValidator.isValid(newPassword)) {
            throw new IllegalArgumentException(
                "Password does not meet requirements: " + passwordValidator.getRequirements()
            );
        }
        
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Revoke all active sessions (security measure - user must log in again)
        userSessionRepository.revokeAllUserSessions(user);

        // ✅ Publish event instead of calling auditLogService
        eventPublisher.publishEvent(new PasswordResetCompletedEvent(user, ipAddress));

        log.info("Password reset completed successfully for user: {}", user.getEmail());
    }

    @Transactional(readOnly = true)
    public boolean validateResetToken(String token) {
        Optional<PasswordResetToken> resetToken = passwordResetTokenRepository.findByToken(token);
        return resetToken.map(PasswordResetToken::isValid).orElse(false);
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