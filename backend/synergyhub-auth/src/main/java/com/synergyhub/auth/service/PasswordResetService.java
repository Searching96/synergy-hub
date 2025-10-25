package com.synergyhub.auth.service;

import com.synergyhub.auth.dto.PasswordResetConfirm;
import com.synergyhub.auth.dto.PasswordResetRequest;
import com.synergyhub.auth.entity.PasswordResetToken;
import com.synergyhub.auth.entity.User;
import com.synergyhub.auth.repository.PasswordResetTokenRepository;
import com.synergyhub.auth.repository.UserRepository;
import com.synergyhub.auth.repository.UserSessionRepository;
import com.synergyhub.common.constant.AppConstants;
import com.synergyhub.common.constant.ErrorMessages;
import com.synergyhub.common.exception.InvalidRequestException;
import com.synergyhub.common.exception.InvalidTokenException;
import com.synergyhub.common.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        String email = request.getEmail().toLowerCase();

        userRepository.findByEmail(email).ifPresent(user -> {
            // Delete any existing reset tokens
            tokenRepository.deleteByUser(user);

            // Create new reset token
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .expiryTime(LocalDateTime.now().plusMinutes(AppConstants.PASSWORD_RESET_TOKEN_EXPIRY_MINUTES))
                    .used(false)
                    .build();

            tokenRepository.save(resetToken);

            // Send email
            emailService.sendPasswordResetEmail(user, token);

            auditLogService.log(user, "PASSWORD_RESET_REQUESTED",
                    "Password reset requested", null, null);

            log.info("Password reset email sent to: {}", user.getEmail());
        });

        // Always return success to prevent email enumeration
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirm request) {
        // Validate password strength
        if (!ValidationUtil.isValidPassword(request.getNewPassword())) {
            throw new InvalidRequestException(ErrorMessages.WEAK_PASSWORD);
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));

        if (!resetToken.isValid()) {
            throw new InvalidTokenException("Invalid or expired reset token");
        }

        User user = resetToken.getUser();

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        // Reset failed login attempts and unlock account
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLockUntil(null);

        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        // Revoke all existing sessions
        sessionRepository.revokeAllUserSessions(user);

        auditLogService.log(user, "PASSWORD_RESET_COMPLETED",
                "Password reset completed successfully", null, null);

        log.info("Password reset completed for user: {}", user.getEmail());
    }
}