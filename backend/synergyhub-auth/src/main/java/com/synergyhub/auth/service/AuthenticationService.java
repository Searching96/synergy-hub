package com.synergyhub.auth.service;

import com.synergyhub.auth.dto.*;
import com.synergyhub.auth.entity.*;
import com.synergyhub.auth.repository.*;
import com.synergyhub.auth.security.CustomUserDetails;
import com.synergyhub.auth.security.JwtTokenProvider;
import com.synergyhub.auth.util.TOTPUtil;
import com.synergyhub.common.constant.AppConstants;
import com.synergyhub.common.constant.ErrorMessages;
import com.synergyhub.common.exception.AccountLockedException;
import com.synergyhub.common.exception.InvalidRequestException;
import com.synergyhub.common.exception.EmailAlreadyExistsException;
import com.synergyhub.common.exception.InvalidTwoFactorCodeException;
import com.synergyhub.common.util.ValidationUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final LoginAttemptRepository loginAttemptRepository;
    private final UserSessionRepository sessionRepository;
    private final TwoFactorSecretRepository twoFactorSecretRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail().toLowerCase();
        String ipAddress = getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Check for account lockout
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            checkAccountLockout(user, email);
        }

        try {
            // Attempt authentication
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            if (user == null) {
                user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
            }

            // Check 2FA
            if (user.getTwoFactorEnabled()) {
                if (request.getTwoFactorCode() == null || request.getTwoFactorCode().isBlank()) {
                    return LoginResponse.builder()
                            .requires2FA(true)
                            .build();
                }

                // Verify 2FA code
                verify2FACode(user, request.getTwoFactorCode());
            }

            // Reset failed attempts on successful login
            user.setFailedLoginAttempts(0);
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Record successful login attempt
            recordLoginAttempt(email, ipAddress, true);

            // Generate JWT
            String jwt = tokenProvider.generateToken(authentication);
            String tokenId = tokenProvider.getTokenId(jwt);

            // Create session
            createUserSession(user, tokenId, ipAddress, userAgent);

            // Audit log
            auditLogService.log(user, "LOGIN_SUCCESS", "User logged in successfully", ipAddress, userAgent);

            log.info("User logged in: {}", email);

            return LoginResponse.builder()
                    .accessToken(jwt)
                    .tokenType("Bearer")
                    .expiresIn(tokenProvider.getExpirationMs())
                    .user(mapToUserInfoDto(user))
                    .requires2FA(false)
                    .build();

        } catch (BadCredentialsException e) {
            handleFailedLogin(user, email, ipAddress);
            throw e;
        }
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase();

        // Validate email format
        if (!ValidationUtil.isValidEmail(email)) {
            throw new InvalidRequestException(ErrorMessages.INVALID_EMAIL_FORMAT);
        }

        // Validate password strength
        if (!ValidationUtil.isValidPassword(request.getPassword())) {
            throw new InvalidRequestException(ErrorMessages.WEAK_PASSWORD);
        }

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        // Determine organization ID
        Integer organizationId = request.getOrganizationId();
        if (organizationId == null) {
            // For self-registration, create a new organization or assign to a default one
            // This will be handled by the organization module in future
            organizationId = 1; // Temporary: assign to default organization
        }

        // Get default role (Guest)
        Role guestRole = roleRepository.findByName("Guest")
                .orElseThrow(() -> new IllegalStateException("Default Guest role not found"));

        // Create user
        User user = User.builder()
                .name(request.getName())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .organizationId(organizationId)
                .twoFactorEnabled(false)
                .accountLocked(false)
                .emailVerified(false)
                .failedLoginAttempts(0)
                .roles(Set.of(guestRole))
                .build();

        userRepository.save(user);

        // Send verification email
        emailVerificationService.sendVerificationEmail(user);

        // Audit log
        auditLogService.log(user, "REGISTER", "User registered successfully", null, null);

        log.info("New user registered: {}", email);

        return LoginResponse.builder()
                .user(mapToUserInfoDto(user))
                .build();
    }

    @Transactional
    public void logout(String tokenId) {
        sessionRepository.revokeSession(tokenId);
        log.info("User session revoked: {}", tokenId);
    }

    @Transactional
    public void logoutAllDevices() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        sessionRepository.revokeAllUserSessions(user);
        log.info("All sessions revoked for user: {}", user.getEmail());
    }

    private void checkAccountLockout(User user, String email) {
        if (user.getAccountLocked()) {
            if (user.getLockUntil() != null && user.getLockUntil().isBefore(LocalDateTime.now())) {
                // Unlock account
                user.setAccountLocked(false);
                user.setLockUntil(null);
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
            } else {
                throw new AccountLockedException(
                        "Account is locked due to multiple failed login attempts. Please try again later.");
            }
        }

        // Check for too many failed attempts in the last 30 minutes
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(AppConstants.LOCKOUT_DURATION_MINUTES);
        long failedAttempts = loginAttemptRepository.countFailedAttemptsSince(email, thirtyMinutesAgo);

        if (failedAttempts >= AppConstants.MAX_LOGIN_ATTEMPTS) {
            lockAccount(user);
            throw new AccountLockedException(
                    "Account is locked due to multiple failed login attempts. Please try again in 30 minutes.");
        }
    }

    private void handleFailedLogin(User user, String email, String ipAddress) {
        recordLoginAttempt(email, ipAddress, false);

        if (user != null) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            if (user.getFailedLoginAttempts() >= AppConstants.MAX_LOGIN_ATTEMPTS) {
                lockAccount(user);
            } else {
                userRepository.save(user);
            }

            auditLogService.log(user, "LOGIN_FAILED", "Failed login attempt", ipAddress, null);
        }
    }

    private void lockAccount(User user) {
        user.setAccountLocked(true);
        user.setLockUntil(LocalDateTime.now().plusMinutes(AppConstants.LOCKOUT_DURATION_MINUTES));
        userRepository.save(user);

        emailService.sendAccountLockedEmail(user);
        auditLogService.log(user, "ACCOUNT_LOCKED", "Account locked due to failed login attempts", null, null);

        log.warn("Account locked: {}", user.getEmail());
    }

    private void verify2FACode(User user, String code) {
        TwoFactorSecret secret = twoFactorSecretRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("2FA is enabled but no secret found"));

        boolean validTotp = TOTPUtil.verifyCode(secret.getSecret(), code);
        boolean validBackup = TOTPUtil.verifyBackupCode(secret.getBackupCodes(), code);

        if (!validTotp && !validBackup) {
            auditLogService.log(user, "2FA_FAILED", "Invalid 2FA code", null, null);
            throw new InvalidTwoFactorCodeException("Invalid 2FA code");
        }

        // If backup code was used, remove it
        if (validBackup) {
            secret.setBackupCodes(TOTPUtil.removeBackupCode(secret.getBackupCodes(), code));
            twoFactorSecretRepository.save(secret);
            auditLogService.log(user, "2FA_BACKUP_CODE_USED", "Backup code used", null, null);
        }
    }

    private void createUserSession(User user, String tokenId, String ipAddress, String userAgent) {
        UserSession session = UserSession.builder()
                .user(user)
                .tokenId(tokenId)
                .ipAddress(ipAddress)
                .deviceInfo(userAgent)
                .expiresAt(LocalDateTime.now().plusSeconds(tokenProvider.getExpirationMs() / 1000))
                .revoked(false)
                .build();

        sessionRepository.save(session);
    }

    private void recordLoginAttempt(String email, String ipAddress, boolean success) {
        LoginAttempt attempt = LoginAttempt.builder()
                .email(email)
                .ipAddress(ipAddress)
                .success(success)
                .build();

        loginAttemptRepository.save(attempt);
    }

    private UserInfoDto mapToUserInfoDto(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = new HashSet<>();
        user.getRoles().forEach(role ->
                role.getPermissions().forEach(perm ->
                        permissions.add(perm.getName())
                )
        );

        return UserInfoDto.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .organizationId(user.getOrganizationId())
                .roles(roleNames)
                .permissions(permissions)
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .emailVerified(user.getEmailVerified())
                .build();
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}