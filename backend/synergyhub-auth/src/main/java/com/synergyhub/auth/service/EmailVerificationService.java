package com.synergyhub.auth.service;

import com.synergyhub.auth.entity.EmailVerification;
import com.synergyhub.auth.entity.User;
import com.synergyhub.auth.repository.EmailVerificationRepository;
import com.synergyhub.auth.repository.UserRepository;
import com.synergyhub.common.constant.AppConstants;
import com.synergyhub.common.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository verificationRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Transactional
    public void sendVerificationEmail(User user) {
        // Delete any existing verification tokens
        verificationRepository.deleteByUser(user);

        // Create new verification token
        String token = UUID.randomUUID().toString();
        EmailVerification verification = EmailVerification.builder()
            .user(user)
            .token(token)
            .verified(false)
            .expiryTime(LocalDateTime.now().plusHours(AppConstants.EMAIL_VERIFICATION_TOKEN_EXPIRY_HOURS))
            .build();

        verificationRepository.save(verification);

        // Send email
        emailService.sendVerificationEmail(user, token);

        log.info("Verification email sent to: {}", user.getEmail());
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerification verification = verificationRepository.findByToken(token)
            .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (verification.getVerified()) {
            throw new InvalidTokenException("Email already verified");
        }

        if (verification.isExpired()) {
            throw new InvalidTokenException("Verification token has expired");
        }

        User user = verification.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verification.setVerified(true);
        verificationRepository.save(verification);

        auditLogService.log(user, "EMAIL_VERIFIED", "Email verified successfully", null, null);

        log.info("Email verified for user: {}", user.getEmail());
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
            .orElseThrow(() -> new InvalidTokenException("User not found"));

        if (user.getEmailVerified()) {
            throw new IllegalStateException("Email is already verified");
        }

        sendVerificationEmail(user);
    }
}