package com.synergyhub.service.security;

import com.synergyhub.domain.entity.User;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.util.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountLockService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;  // ✅ Added
    private final EmailService emailService;  // ✅ Added (optional)

    @Value("${security.max-login-attempts}")
    private int maxLoginAttempts;

    @Value("${security.account-lock-duration-minutes}")
    private int lockDurationMinutes;

    @Transactional
    public boolean isAccountLocked(User user) {
        if (!user.getAccountLocked()) {
            return false;
        }

        // Check if lock has expired
        if (user.getLockUntil() != null && LocalDateTime.now().isAfter(user.getLockUntil())) {
            // Auto-unlock
            unlockAccount(user, null, "Lock duration expired");
            return false;
        }

        return true;
    }

    @Transactional
    public void handleFailedLogin(User user, String ipAddress) {  // ✅ Added ipAddress
        int previousAttempts = user.getFailedLoginAttempts();
        user.incrementFailedAttempts();

        if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
            user.lock(lockDurationMinutes);

            // ✅ Audit log for account lock
            auditLogService.logAccountLocked(user, ipAddress);

            log.warn("Account locked for user: {} due to {} failed login attempts",
                    user.getEmail(), maxLoginAttempts);

            // ✅ Optional: Send email notification
            try {
                emailService.sendAccountLockedEmail(user.getEmail(), user, ipAddress);
            } catch (Exception e) {
                log.error("Failed to send account locked email to: {}", user.getEmail(), e);
            }
        } else {
            // ✅ Audit log for failed login attempt
            auditLogService.logLoginFailed(user.getEmail(), ipAddress, null,
                    String.format("Failed login attempt #%d", user.getFailedLoginAttempts()));
            log.debug("Failed login attempt #{} for user: {}",
                    user.getFailedLoginAttempts(), user.getEmail());
        }

        userRepository.save(user);
    }

    @Transactional
    public void resetFailedAttempts(User user, String ipAddress) {  // ✅ Added ipAddress
        int previousAttempts = user.getFailedLoginAttempts();

        user.resetFailedAttempts();
        user.setAccountLocked(false);
        user.setLockUntil(null);
        userRepository.save(user);

        // ✅ Audit log only if there were previous failed attempts
        if (previousAttempts > 0) {
            auditLogService.createAuditLog(
                    user,
                    "FAILED_LOGIN_ATTEMPTS_RESET",
                    String.format("Failed login attempts reset from %d to 0 after successful login",
                            previousAttempts),
                    ipAddress
            );
        }

        log.debug("Reset failed login attempts for user: {}", user.getEmail());
    }

    @Transactional
    public void unlockAccount(User user, String ipAddress, String reason) {  // ✅ Added ipAddress and reason
        boolean wasLocked = user.getAccountLocked();

        user.setAccountLocked(false);
        user.setLockUntil(null);
        user.resetFailedAttempts();
        userRepository.save(user);

        // ✅ Audit log only if account was actually locked
        if (wasLocked) {
            auditLogService.logAccountUnlocked(user, ipAddress);

            // ✅ Additional audit log with reason if provided
            if (reason != null && !reason.isEmpty()) {
                auditLogService.createAuditLog(
                        user,
                        "ACCOUNT_UNLOCK_REASON",
                        String.format("Account unlocked. Reason: %s", reason),
                        ipAddress
                );
            }

            log.info("Account unlocked for user: {} (Reason: {})", user.getEmail(), reason);
        }
    }

    // ✅ Convenience method without reason
    @Transactional
    public void unlockAccount(User user, String ipAddress) {
        unlockAccount(user, ipAddress, "Manual unlock");
    }

    // ✅ New method: Get remaining lock time
    public long getRemainingLockTimeMinutes(User user) {
        if (!user.getAccountLocked() || user.getLockUntil() == null) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(user.getLockUntil())) {
            return 0;
        }

        return java.time.Duration.between(now, user.getLockUntil()).toMinutes();
    }

    // ✅ New method: Check if user is approaching lock threshold
    public boolean isApproachingLockThreshold(User user) {
        return user.getFailedLoginAttempts() >= (maxLoginAttempts - 1);
    }

    // ✅ New method: Get remaining attempts before lock
    public int getRemainingAttempts(User user) {
        return Math.max(0, maxLoginAttempts - user.getFailedLoginAttempts());
    }

    // ✅ New method: Force lock account (admin action)
    @Transactional
    public void forceAccountLock(User user, User admin, String reason, String ipAddress) {
        user.lock(lockDurationMinutes);
        userRepository.save(user);

        auditLogService.createAuditLog(
                admin,
                "ACCOUNT_FORCE_LOCKED",
                String.format("Admin %s (ID: %d) force locked account for user %s (ID: %d). Reason: %s",
                        admin.getEmail(), admin.getId(), user.getEmail(), user.getId(), reason),
                ipAddress
        );

        log.warn("Account force locked by admin {} for user: {} (Reason: {})",
                admin.getEmail(), user.getEmail(), reason);

        // Send notification to locked user
        try {
            emailService.sendAccountLockedEmail(user.getEmail(), user, ipAddress);
        } catch (Exception e) {
            log.error("Failed to send account locked email to: {}", user.getEmail(), e);
        }
    }
}
