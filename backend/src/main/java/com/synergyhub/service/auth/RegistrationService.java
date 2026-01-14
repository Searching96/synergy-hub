package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.EmailVerification;
import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.Role;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.mapper.UserMapper;
import com.synergyhub.dto.request.RegisterRequest;
import com.synergyhub.dto.response.UserResponse;
import com.synergyhub.events.auth.*;
import com.synergyhub.events.email.EmailVerificationEvent;
import com.synergyhub.exception.BadRequestException;
import com.synergyhub.exception.EmailAlreadyExistsException;
import com.synergyhub.exception.ResourceNotFoundException;
import com.synergyhub.repository.EmailVerificationRepository;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.service.security.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
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
    private final UserMapper userMapper;
    private final UserProvisioningStrategy userProvisioningStrategy;
    private final UserValidationService userValidationService;
    private final RateLimitService rateLimitService;

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
            userValidationService.validatePassword(
                request.getPassword(), 
                request.getEmail(),
                request.getName()
            );
        } catch (EmailAlreadyExistsException | BadRequestException ex) {
            // ✅ Publish registration failed event
            eventPublisher.publishEvent(
                new RegistrationFailedEvent(request.getEmail(), ipAddress, ex.getMessage())
            );
            throw ex;
        }



        Organization organization = null;
        Role defaultRole = null;
        boolean wantsToCreateOrg = request.getNewOrganizationName() != null && !request.getNewOrganizationName().isBlank();
        boolean wantsToJoinOrg = request.getOrganizationId() != null || (request.getInvitationToken() != null && !request.getInvitationToken().isBlank());
        if (wantsToCreateOrg || wantsToJoinOrg) {
            organization = userProvisioningStrategy.determineOrganization(request);
            defaultRole = userProvisioningStrategy.determineDefaultRole(request, organization);
        }

        // Create user (organization and roles may be null/empty)
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(userValidationService.encodePassword(request.getPassword()))
                .emailVerified(!emailVerificationEnabled)
                .twoFactorEnabled(false)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build();
        // Add membership if organization and defaultRole are present
        if (organization != null && defaultRole != null) {
            user.addMembership(organization, defaultRole);
        } else if (organization != null) {
            user.addMembership(organization, null);
        }

        User savedUser = userRepository.save(user);
        log.info("User created successfully: {}", savedUser.getEmail());

        // ✅ Publish user created event
        eventPublisher.publishEvent(new UserCreatedEvent(savedUser, ipAddress));

        // Handle email verification
        if (emailVerificationEnabled) {
            sendVerificationEmail(savedUser, ipAddress);
        } else {
            eventPublisher.publishEvent(new RegistrationCompletedEvent(savedUser, ipAddress));
        }

        return userMapper.toUserResponse(savedUser);
    }

    @Transactional
    public void verifyEmail(String token, String ipAddress) {
        EmailVerification verification = emailVerificationRepository.findByToken(token)
                .orElseThrow(() -> {
                    // ✅ Publish email verification failed event (no user)
                    eventPublisher.publishEvent(
                        new EmailVerificationFailedEvent(null, ipAddress, "Invalid token")
                    );
                    return new BadRequestException("Invalid or expired verification token");
                });

        if (verification.getVerified()) {
            // ✅ Publish already verified event
            eventPublisher.publishEvent(
                new EmailVerificationFailedEvent(
                    verification.getUser(), 
                    ipAddress, 
                    "Email already verified"
                )
            );
            throw new BadRequestException("Email has already been verified");
        }

        if (verification.isExpired()) {
            // ✅ Publish expired token event
            eventPublisher.publishEvent(
                new EmailVerificationFailedEvent(
                    verification.getUser(), 
                    ipAddress, 
                    "Token expired"
                )
            );
            throw new BadRequestException("Verification token has expired. Please request a new one.");
        }

        User user = verification.getUser();
        String email = user.getEmail();
        String name = user.getName();

        // Flip flags using bulk updates only to avoid flushing user/roles
        userRepository.markEmailVerified(user.getId());
        emailVerificationRepository.markVerified(verification.getId());

        log.info("Email verified successfully for user: {}", email);

        // ✅ Publish email verified event
        eventPublisher.publishEvent(new EmailVerifiedEvent(user, ipAddress));

        // Send welcome email
        eventPublisher.publishEvent(new RegistrationCompletedEvent(user, ipAddress));
    }

    @Transactional
    public void resendVerificationEmail(String email, String ipAddress) {
        // SECURITY: Rate limit email resends to prevent abuse
        rateLimitService.checkEmailResend(email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    // ✅ Publish failed event for non-existent user
                    eventPublisher.publishEvent(
                        new EmailVerificationFailedEvent(
                            null, 
                            ipAddress, 
                            "User not found: " + email
                        )
                    );
                    return new ResourceNotFoundException("User", "email", email);
                });

        if (user.getEmailVerified()) {
            // ✅ Publish already verified event
            eventPublisher.publishEvent(
                new EmailVerificationFailedEvent(
                    user, 
                    ipAddress, 
                    "Email already verified"
                )
            );
            throw new BadRequestException("Email is already verified");
        }

        // Delete old verification token if exists
        Optional<EmailVerification> oldVerification = emailVerificationRepository
                .findByUserAndVerifiedFalse(user);
        oldVerification.ifPresent(emailVerificationRepository::delete);

        // Send new verification email
        sendVerificationEmail(user, ipAddress);
        
        // Record the resend for rate limiting
        rateLimitService.recordEmailResend(email);

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

        // ✅ Publish email verification event (triggers email sending)
        eventPublisher.publishEvent(new EmailVerificationEvent(user, token, ipAddress));
        log.debug("Verification email sent to: {}", user.getEmail());
    }

    private String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }
}