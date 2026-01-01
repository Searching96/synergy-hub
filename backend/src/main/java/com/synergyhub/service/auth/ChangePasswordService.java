package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.ChangePasswordRequest;
import com.synergyhub.events.auth.LoginFailedEvent;
import com.synergyhub.events.auth.PasswordChangedEvent;
import com.synergyhub.exception.BadRequestException;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.repository.UserSessionRepository;
import com.synergyhub.service.security.AuditLogService;
import com.synergyhub.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChangePasswordService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request, String ipAddress) {
        User user = userRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new BadRequestException("User not found for email: " + email));

        log.info("Password change request for user: {}", user.getEmail());

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            eventPublisher.publishEvent(new LoginFailedEvent(email, ipAddress, "user-agent", "Incorrect current password")); // ✅ Optional: Log failed attempt
            throw new BadRequestException("Current password is incorrect");
        }

        // Validate new password
        if (!passwordValidator.isValid(request.getNewPassword())) {
            throw new BadRequestException("Password does not meet requirements: " +
                    passwordValidator.getRequirements());
        }

        // Check if new password is different from current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all active sessions (user must log in again with new password)
        userSessionRepository.revokeAllUserSessions(user);

        // ✅ FIXED: Pass User object instead of email String
        eventPublisher.publishEvent(new PasswordChangedEvent(user, ipAddress));

        log.info("Password changed successfully for user: {}", user.getEmail());
    }
}