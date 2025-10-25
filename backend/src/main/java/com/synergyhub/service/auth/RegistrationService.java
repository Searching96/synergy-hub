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
import com.synergyhub.repository.OrganizationRepository;
import com.synergyhub.repository.RoleRepository;
import com.synergyhub.repository.UserRepository;
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
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final UserMapper userMapper;

    @Value("${security.email-verification-token-expiry-hours}")
    private int emailVerificationTokenExpiryHours;

    @Value("${app.email-verification-enabled:true}")
    private boolean emailVerificationEnabled;

    @Value("${app.default-organization-id:1}")
    private Integer defaultOrganizationId;

    private static final String DEFAULT_ROLE = "Team Member";

    @Transactional
    public UserResponse register(RegisterRequest request, String ipAddress) {
        log.info("Registration request received for email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // Validate password
        if (!passwordValidator.isValid(request.getPassword())) {
            throw new BadRequestException("Password does not meet requirements: " +
                    passwordValidator.getRequirements());
        }

        // Determine organization
        Integer orgId = request.getOrganizationId() != null ?
                request.getOrganizationId() : defaultOrganizationId;

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

        // Get default role
        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", DEFAULT_ROLE));

        // Create user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
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
            sendVerificationEmail(savedUser);
        } else {
            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getName());
        }

        // Audit log
        auditLogService.logUserCreated(savedUser, ipAddress);

        return userMapper.toUserResponse(savedUser);
    }

    @Transactional
    public void verifyEmail(String token, String ipAddress) {
        EmailVerification verification = emailVerificationRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        if (verification.getVerified()) {
            throw new BadRequestException("Email has already been verified");
        }

        if (verification.isExpired()) {
            throw new BadRequestException("Verification token has expired. Please request a new one.");
        }

        User user = verification.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verification.setVerified(true);
        emailVerificationRepository.save(verification);

        log.info("Email verified successfully for user: {}", user.getEmail());

        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getName());

        // Audit log
        auditLogService.logEmailVerified(user, ipAddress);
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.getEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        // Delete old verification token if exists
        Optional<EmailVerification> oldVerification = emailVerificationRepository
                .findByUserAndVerifiedFalse(user);
        oldVerification.ifPresent(emailVerificationRepository::delete);

        // Send new verification email
        sendVerificationEmail(user);

        log.info("Verification email resent to: {}", email);
    }

    private void sendVerificationEmail(User user) {
        String token = generateVerificationToken();

        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .token(token)
                .verified(false)
                .expiryTime(LocalDateTime.now().plusHours(emailVerificationTokenExpiryHours))
                .build();

        emailVerificationRepository.save(verification);

        emailService.sendEmailVerification(user.getEmail(), token);
        log.debug("Verification email sent to: {}", user.getEmail());
    }

    private String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }
}