package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.EmailVerification;
import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.Role;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.mapper.UserMapper;
import com.synergyhub.dto.request.RegisterRequest;
import com.synergyhub.dto.response.UserResponse;
import com.synergyhub.exception.BadRequestException;
import com.synergyhub.exception.EmailAlreadyExistsException;
import com.synergyhub.exception.ResourceNotFoundException;
import com.synergyhub.repository.EmailVerificationRepository;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.service.security.AuditLogService;
import com.synergyhub.events.auth.RegistrationCompletedEvent;
import com.synergyhub.events.email.EmailVerificationEvent;

import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j


public class RegistrationService {

        private final UserRepository userRepository;
        private final EmailVerificationRepository emailVerificationRepository;
        private final ApplicationEventPublisher eventPublisher;
        private final AuditLogService auditLogService;
        private final UserMapper userMapper;
        private final UserProvisioningStrategy userProvisioningStrategy;
        private final UserValidationService userValidationService;

        @Value("${security.email-verification-token-expiry-hours}")
        private int emailVerificationTokenExpiryHours;

        @Value("${app.email-verification-enabled:true}")
        private boolean emailVerificationEnabled;

    @Transactional
    public UserResponse register(RegisterRequest request, String ipAddress) {
        log.info("Registration request received for email: {}", request.getEmail());


        // Validate email uniqueness and password
        try {
            userValidationService.validateEmailUniqueness(request.getEmail());
            userValidationService.validatePassword(request.getPassword());
        } catch (EmailAlreadyExistsException | BadRequestException ex) {
            auditLogService.createAuditLog(
                    null,
                    "REGISTRATION_FAILED",
                    ex.getMessage(),
                    ipAddress,
                    null
            );
            throw ex;
        }


        // Use strategy for organization and role
        Organization organization = userProvisioningStrategy.determineOrganization(request);
        Role defaultRole = userProvisioningStrategy.determineDefaultRole(request);

        // Create user

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(userValidationService.encodePassword(request.getPassword()))
                .organization(organization)
                .emailVerified(!emailVerificationEnabled)
                .twoFactorEnabled(false)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .roles(new HashSet<>())
                .build();

        user.getRoles().add(defaultRole);

        User savedUser = userRepository.save(user);
        log.info("User created successfully: {}", savedUser.getEmail());

        // Handle email verification
                if (emailVerificationEnabled) {
                        sendVerificationEmail(savedUser, ipAddress);
                        // ✅ Audit log for verification email sent
                        auditLogService.createAuditLog(
                                        savedUser,
                                        "EMAIL_VERIFICATION_SENT",
                                        "Email verification sent",
                                        ipAddress,
                                        null
                        );
                } else {
                        eventPublisher.publishEvent(new RegistrationCompletedEvent(savedUser, ipAddress));
                }

        // Audit log
        auditLogService.logUserCreated(savedUser, ipAddress);

        return userMapper.toUserResponse(savedUser);
    }

    @Transactional
    public void verifyEmail(String token, String ipAddress) {
        EmailVerification verification = emailVerificationRepository.findByToken(token)
                .orElseThrow(() -> {
                    // ✅ Audit log for invalid token
                    auditLogService.createAuditLog(
                            null,
                            "EMAIL_VERIFICATION_FAILED",
                            "Email verification attempt with invalid token",
                            ipAddress,
                            null
                    );
                    return new BadRequestException("Invalid or expired verification token");
                });

        if (verification.getVerified()) {
            // ✅ Audit log for already verified email
            auditLogService.createAuditLog(
                    verification.getUser(),
                    "EMAIL_VERIFICATION_FAILED",
                    "Email verification attempt for already verified email",
                    ipAddress,
                    null
            );
            throw new BadRequestException("Email has already been verified");
        }

        if (verification.isExpired()) {
            // ✅ Audit log for expired token
            auditLogService.createAuditLog(
                    verification.getUser(),
                    "EMAIL_VERIFICATION_FAILED",
                    "Email verification attempt with expired token",
                    ipAddress,
                    null
            );
            throw new BadRequestException("Verification token has expired. Please request a new one.");
        }

        User user = verification.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verification.setVerified(true);
        emailVerificationRepository.save(verification);

        log.info("Email verified successfully for user: {}", user.getEmail());

        // Send welcome email
        eventPublisher.publishEvent(new RegistrationCompletedEvent(user, ipAddress));

        // Audit log
        auditLogService.logEmailVerified(user, ipAddress);
    }

    @Transactional
    public void resendVerificationEmail(String email, String ipAddress) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    // ✅ Audit log for non-existent user
                    auditLogService.createAuditLog(
                            null,
                            "EMAIL_VERIFICATION_RESEND_FAILED",
                            String.format("Verification email resend attempt for non-existent email: %s", email),
                            ipAddress,
                            null
                    );
                    return new ResourceNotFoundException("User", "email", email);
                });

        if (user.getEmailVerified()) {
            // ✅ Audit log for already verified email
            auditLogService.createAuditLog(
                    user,
                    "EMAIL_VERIFICATION_RESEND_FAILED",
                    "Verification email resend attempt for already verified email",
                    ipAddress,
                    null
            );
            throw new BadRequestException("Email is already verified");
        }

        // Delete old verification token if exists
        Optional<EmailVerification> oldVerification = emailVerificationRepository
                .findByUserAndVerifiedFalse(user);
        oldVerification.ifPresent(emailVerificationRepository::delete);

        // Send new verification email
        sendVerificationEmail(user, ipAddress);

        // ✅ Audit log for resend
        auditLogService.createAuditLog(
                user,
                "EMAIL_VERIFICATION_RESENT",
                "Verification email resent",
                ipAddress,
                null
        );

        log.info("Verification email resent to: {}", email);
    }

        private void sendVerificationEmail(User user, String ipAddress) {
                String token = generateVerificationToken();

                EmailVerification verification = EmailVerification.builder()
                                .user(user)
                                .token(token)
                                .verified(false)
                                .expiryTime(LocalDateTime.now().plusHours(emailVerificationTokenExpiryHours))
                                .build();

                emailVerificationRepository.save(verification);

                eventPublisher.publishEvent(new EmailVerificationEvent(user, token, ipAddress));
                log.debug("Verification email sent to: {}", user.getEmail());
        }

    private String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }
}
