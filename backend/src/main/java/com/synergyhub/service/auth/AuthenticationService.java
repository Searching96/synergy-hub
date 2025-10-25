package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.entity.UserSession;
import com.synergyhub.dto.mapper.UserMapper;
import com.synergyhub.dto.request.LoginRequest;
import com.synergyhub.dto.response.LoginResponse;
import com.synergyhub.dto.response.UserResponse;
import com.synergyhub.exception.AccountLockedException;
import com.synergyhub.exception.ResourceNotFoundException;
import com.synergyhub.exception.TwoFactorAuthenticationException;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.repository.UserSessionRepository;
import com.synergyhub.security.JwtTokenProvider;
import com.synergyhub.service.security.AccountLockService;
import com.synergyhub.service.security.AuditLogService;
import com.synergyhub.service.security.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginAttemptService loginAttemptService;
    private final AccountLockService accountLockService;
    private final AuditLogService auditLogService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final UserMapper userMapper;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmailWithRolesAndPermissions(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // Check if account is locked
        if (accountLockService.isAccountLocked(user)) {
            auditLogService.logLoginFailed(request.getEmail(), ipAddress, userAgent, "Account is locked");
            throw new AccountLockedException(
                    "Account is locked due to multiple failed login attempts. Please try again later.",
                    user.getLockUntil()
            );
        }

        // Authenticate user
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // Handle 2FA if enabled
            if (user.getTwoFactorEnabled()) {
                return handle2FALogin(user, request, ipAddress, userAgent);
            }

            // Reset failed attempts on successful login
            accountLockService.resetFailedAttempts(user);

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generate JWT token
            String accessToken = jwtTokenProvider.generateTokenFromUserId(user.getId(), user.getEmail());
            String tokenId = jwtTokenProvider.getTokenIdFromToken(accessToken);

            // Create session
            createUserSession(user, tokenId, ipAddress, userAgent);

            // Record successful login
            loginAttemptService.recordLoginAttempt(request.getEmail(), ipAddress, true);
            auditLogService.logLoginSuccess(user, ipAddress, userAgent);

            UserResponse userResponse = userMapper.toUserResponse(user);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                    .user(userResponse)
                    .twoFactorRequired(false)
                    .build();

        } catch (BadCredentialsException e) {
            // Handle failed login
            accountLockService.handleFailedLogin(user);
            loginAttemptService.recordLoginAttempt(request.getEmail(), ipAddress, false);
            auditLogService.logLoginFailed(request.getEmail(), ipAddress, userAgent, "Invalid credentials");
            throw e;
        }
    }

    private LoginResponse handle2FALogin(User user, LoginRequest request, String ipAddress, String userAgent) {
        if (request.getTwoFactorCode() == null || request.getTwoFactorCode().isEmpty()) {
            // Return temporary token for 2FA completion
            String tempToken = jwtTokenProvider.generateTemporaryToken(user.getId(), user.getEmail());
            auditLogService.logTwoFactorRequired(user, ipAddress);

            return LoginResponse.builder()
                    .twoFactorRequired(true)
                    .twoFactorToken(tempToken)
                    .build();
        }

        // Verify 2FA code
        boolean isValid = twoFactorAuthService.verifyCode(user, request.getTwoFactorCode());

        if (!isValid) {
            auditLogService.logTwoFactorFailed(user, ipAddress);
            throw new TwoFactorAuthenticationException("Invalid two-factor authentication code");
        }

        // 2FA successful, proceed with login
        accountLockService.resetFailedAttempts(user);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateTokenFromUserId(user.getId(), user.getEmail());
        String tokenId = jwtTokenProvider.getTokenIdFromToken(accessToken);

        createUserSession(user, tokenId, ipAddress, userAgent);

        loginAttemptService.recordLoginAttempt(request.getEmail(), ipAddress, true);
        auditLogService.logTwoFactorSuccess(user, ipAddress);
        auditLogService.logLoginSuccess(user, ipAddress, userAgent);

        UserResponse userResponse = userMapper.toUserResponse(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .user(userResponse)
                .twoFactorRequired(false)
                .build();
    }

    private void createUserSession(User user, String tokenId, String ipAddress, String deviceInfo) {
        UserSession session = UserSession.builder()
                .user(user)
                .tokenId(tokenId)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpirationMs() / 1000))
                .revoked(false)
                .build();

        userSessionRepository.save(session);
    }
}