package com.synergyhub.service.security;

import com.synergyhub.domain.entity.User;
import com.synergyhub.repository.UserRepository;
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
            unlockAccount(user);
            return false;
        }

        return true;
    }

    @Transactional
    public void handleFailedLogin(User user) {
        user.incrementFailedAttempts();

        if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
            user.lock(lockDurationMinutes);
            log.warn("Account locked for user: {} due to {} failed login attempts",
                    user.getEmail(), maxLoginAttempts);
        }

        userRepository.save(user);
    }

    @Transactional
    public void resetFailedAttempts(User user) {
        user.resetFailedAttempts();
        user.setAccountLocked(false);
        user.setLockUntil(null);
        userRepository.save(user);
        log.debug("Reset failed login attempts for user: {}", user.getEmail());
    }

    @Transactional
    public void unlockAccount(User user) {
        user.setAccountLocked(false);
        user.setLockUntil(null);
        user.resetFailedAttempts();
        userRepository.save(user);
        log.info("Account unlocked for user: {}", user.getEmail());
    }
}