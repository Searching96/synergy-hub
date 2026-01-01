package com.synergyhub.service.security;

import com.synergyhub.domain.entity.User;
import com.synergyhub.events.auth.AccountForceLockedEvent;
import com.synergyhub.events.auth.AccountLockedEvent;
import com.synergyhub.events.auth.AccountUnlockedEvent;
import com.synergyhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountLockService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

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
    public void handleFailedLogin(User user, String ipAddress) {
        user.incrementFailedAttempts();

        if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
            user.lock(lockDurationMinutes);
            userRepository.save(user);

            // ✅ Publish account locked event
            eventPublisher.publishEvent(
                new AccountLockedEvent(user, ipAddress, maxLoginAttempts, lockDurationMinutes)
            );

            log.warn("Account locked for user: {} due to {} failed login attempts",
                    user.getEmail(), maxLoginAttempts);
        } else {
            userRepository.save(user);
            log.debug("Failed login attempt #{} for user: {}",
                    user.getFailedLoginAttempts(), user.getEmail());
        }
    }

    @Transactional
    public void resetFailedAttempts(User user, String ipAddress) {
        user.resetFailedAttempts();
        user.setAccountLocked(false);
        user.setLockUntil(null);
        userRepository.save(user);

        log.debug("Reset failed login attempts for user: {}", user.getEmail());
    }

    @Transactional
    public void unlockAccount(User user, String ipAddress, String reason) {
        boolean wasLocked = user.getAccountLocked();

        user.setAccountLocked(false);
        user.setLockUntil(null);
        user.resetFailedAttempts();
        userRepository.save(user);

        // ✅ Publish account unlocked event only if it was actually locked
        if (wasLocked) {
            eventPublisher.publishEvent(
                new AccountUnlockedEvent(user, ipAddress, reason != null ? reason : "Manual unlock")
            );
            log.info("Account unlocked for user: {} (Reason: {})", user.getEmail(), reason);
        }
    }

    @Transactional
    public void unlockAccount(User user, String ipAddress) {
        unlockAccount(user, ipAddress, "Manual unlock");
    }

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

    public boolean isApproachingLockThreshold(User user) {
        return user.getFailedLoginAttempts() >= (maxLoginAttempts - 1);
    }

    public int getRemainingAttempts(User user) {
        return Math.max(0, maxLoginAttempts - user.getFailedLoginAttempts());
    }

    @Transactional
    public void forceAccountLock(User user, User admin, String reason, String ipAddress) {
        user.lock(lockDurationMinutes);
        userRepository.save(user);

        // ✅ Publish force locked event
        eventPublisher.publishEvent(
            new AccountForceLockedEvent(user, admin, ipAddress, reason)
        );

        log.warn("Account force locked by admin {} for user: {} (Reason: {})",
                admin.getEmail(), user.getEmail(), reason);
    }
}