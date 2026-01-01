package com.synergyhub.service.security;

import com.synergyhub.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting service to prevent brute force attacks.
 * Uses in-memory storage. For production with multiple instances, use Redis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    // In-memory storage: key -> RateLimitEntry
    // For production with multiple instances, replace with Redis
    private final Map<String, RateLimitEntry> rateLimitStore = new ConcurrentHashMap<>();

    @Value("${security.rate-limit.two-factor-attempts:5}")
    private int twoFactorMaxAttempts;

    @Value("${security.rate-limit.two-factor-window-minutes:5}")
    private int twoFactorWindowMinutes;

    @Value("${security.rate-limit.email-resend-attempts:3}")
    private int emailResendMaxAttempts;

    @Value("${security.rate-limit.email-resend-window-minutes:60}")
    private int emailResendWindowMinutes;

    /**
     * Check if a 2FA verification attempt is rate limited
     */
    public void check2FAAttempt(String identifier) {
        String key = "2fa:" + identifier;
        if (isLimited(key, twoFactorMaxAttempts, twoFactorWindowMinutes)) {
            log.warn("Rate limit exceeded for 2FA attempts: {}", identifier);
            throw new TooManyRequestsException(
                String.format("Too many 2FA verification attempts. Please try again in %d minutes.", 
                    twoFactorWindowMinutes)
            );
        }
    }

    /**
     * Record a failed 2FA attempt
     */
    public void recordFailed2FAAttempt(String identifier) {
        String key = "2fa:" + identifier;
        recordAttempt(key, twoFactorWindowMinutes);
    }

    /**
     * Clear 2FA rate limit (called on successful verification)
     */
    public void clear2FAAttempts(String identifier) {
        String key = "2fa:" + identifier;
        rateLimitStore.remove(key);
        log.debug("Cleared 2FA rate limit for: {}", identifier);
    }

    /**
     * Check if email resend is rate limited
     */
    public void checkEmailResend(String email) {
        String key = "email-resend:" + email;
        if (isLimited(key, emailResendMaxAttempts, emailResendWindowMinutes)) {
            log.warn("Rate limit exceeded for email resend: {}", email);
            throw new TooManyRequestsException(
                String.format("Too many verification emails sent. Please try again in %d minutes.", 
                    emailResendWindowMinutes)
            );
        }
    }

    /**
     * Record an email resend attempt
     */
    public void recordEmailResend(String email) {
        String key = "email-resend:" + email;
        recordAttempt(key, emailResendWindowMinutes);
    }

    /**
     * Generic rate limit check
     */
    private boolean isLimited(String key, int maxAttempts, int windowMinutes) {
        cleanupExpiredEntries();
        
        RateLimitEntry entry = rateLimitStore.get(key);
        if (entry == null) {
            return false;
        }

        if (entry.isExpired(windowMinutes)) {
            rateLimitStore.remove(key);
            return false;
        }

        return entry.getAttempts() >= maxAttempts;
    }

    /**
     * Record an attempt for rate limiting
     */
    private void recordAttempt(String key, int windowMinutes) {
        cleanupExpiredEntries();
        
        rateLimitStore.compute(key, (k, entry) -> {
            if (entry == null || entry.isExpired(windowMinutes)) {
                return new RateLimitEntry(LocalDateTime.now(), 1);
            } else {
                entry.increment();
                return entry;
            }
        });
    }

    /**
     * Periodically cleanup expired entries to prevent memory leaks
     */
    private void cleanupExpiredEntries() {
        // Simple cleanup - removes entries older than 2 hours
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        rateLimitStore.entrySet().removeIf(entry -> 
            entry.getValue().getFirstAttempt().isBefore(cutoff)
        );
    }

    /**
     * Internal class to track rate limit attempts
     */
    private static class RateLimitEntry {
        private final LocalDateTime firstAttempt;
        private final AtomicInteger attempts;

        public RateLimitEntry(LocalDateTime firstAttempt, int initialCount) {
            this.firstAttempt = firstAttempt;
            this.attempts = new AtomicInteger(initialCount);
        }

        public LocalDateTime getFirstAttempt() {
            return firstAttempt;
        }

        public int getAttempts() {
            return attempts.get();
        }

        public void increment() {
            attempts.incrementAndGet();
        }

        public boolean isExpired(int windowMinutes) {
            return LocalDateTime.now().isAfter(firstAttempt.plusMinutes(windowMinutes));
        }
    }
}
