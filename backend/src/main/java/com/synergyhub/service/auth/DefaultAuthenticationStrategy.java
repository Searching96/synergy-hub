package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.LoginRequest;
import com.synergyhub.events.auth.LoginFailedEvent;
import com.synergyhub.events.auth.LoginSuccessEvent;
import com.synergyhub.exception.AccountLockedException;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.service.security.AccountLockService;

import org.springframework.context.ApplicationEventPublisher;
import com.synergyhub.service.security.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Default implementation of AuthenticationStrategy using current logic.
 */
@Component
@RequiredArgsConstructor
public class DefaultAuthenticationStrategy implements AuthenticationStrategy {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;
    private final AccountLockService accountLockService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public User authenticate(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmailWithRolesAndPermissions(request.getEmail())
                .orElseThrow(() -> {
                    eventPublisher.publishEvent(new LoginFailedEvent(request.getEmail(), ipAddress, userAgent, "User not found"));
                    return new BadCredentialsException("Invalid credentials");
                });

        if (!user.getEmailVerified()) {
            eventPublisher.publishEvent(new LoginFailedEvent(request.getEmail(), ipAddress, userAgent, "Email not verified"));
            throw new BadCredentialsException("Email not verified. Please check your email for verification link.");
        }

        if (accountLockService.isAccountLocked(user)) {
            long remainingMinutes = accountLockService.getRemainingLockTimeMinutes(user);
            String message = String.format("Account is locked. Please try again in %d minutes.", remainingMinutes);
            eventPublisher.publishEvent(new LoginFailedEvent(request.getEmail(), ipAddress, userAgent, "Account locked"));
            throw new AccountLockedException(message, LocalDateTime.now().plusMinutes(remainingMinutes));
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        return user;
    }

    @Override
    public void handlePostLogin(User user, String ipAddress, String userAgent) {
        loginAttemptService.recordLoginAttempt(user.getEmail(), ipAddress, true);
        accountLockService.resetFailedAttempts(user, ipAddress);
        eventPublisher.publishEvent(new LoginSuccessEvent(user, ipAddress, userAgent));
    }

    @Override
    public void handleFailedLogin(String email, String ipAddress, String userAgent, String reason) {
        loginAttemptService.recordLoginAttempt(email, ipAddress, false);
        eventPublisher.publishEvent(new LoginFailedEvent(email, ipAddress, userAgent, reason));
    }
}
