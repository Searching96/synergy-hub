package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.entity.UserSession;
import com.synergyhub.dto.mapper.UserSessionMapper;
import com.synergyhub.dto.response.UserSessionResponse;
import com.synergyhub.exception.ResourceNotFoundException;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.repository.UserSessionRepository;
import com.synergyhub.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository; // ✅ Added dependency
    private final UserSessionMapper userSessionMapper;
    private final JwtTokenProvider jwtTokenProvider;

    public JwtTokenProvider getJwtTokenProvider() {
        return jwtTokenProvider;
    }

    @Transactional
    public String createSession(User user, String userAgent, String ipAddress) {
        String token = jwtTokenProvider.generateTokenFromUserId(user.getId(), user.getEmail());
        String tokenId = jwtTokenProvider.getTokenIdFromToken(token);

        LocalDateTime now = LocalDateTime.now();
        UserSession session = UserSession.builder()
                .user(user)
                .tokenId(tokenId)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .expiresAt(now.plusSeconds(jwtTokenProvider.getExpirationMs() / 1000))
                .lastAccessedAt(now)
                .revoked(false)
                .build();
        userSessionRepository.save(session);
        return token;
    }

    /**
     * List all active sessions for a user by User ID
     */
    @Transactional(readOnly = true)
    public List<UserSessionResponse> listActiveSessions(Long userId, String currentTokenId) {
        User user = getUserById(userId);
        List<UserSession> sessions = userSessionRepository.findActiveSessionsByUser(user, LocalDateTime.now());
        
        return sessions.stream()
                .map(session -> userSessionMapper.toDTO(session, currentTokenId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "sessionRevocation", key = "#tokenId", unless = "#result == true")
    public boolean isSessionRevoked(String tokenId) {
        return userSessionRepository.findByTokenId(tokenId)
                .map(UserSession::getRevoked)
                .orElse(true); // Treat non-existent sessions as revoked
    }

    /**
     * Revoke a session by tokenId (logout from a specific device)
     * Includes OWNERSHIP CHECK to prevent IDOR attacks.
     */
    @Transactional
    @CacheEvict(value = "sessionRevocation", key = "#tokenId")
    public void revokeSession(String tokenId, Long userId) {
        // 1. Fetch the session
        UserSession session = userSessionRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "tokenId", tokenId));

        // 2. Verify ownership
        if (!session.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to revoke this session");
        }

        // 3. Revoke
        session.setRevoked(true);
        userSessionRepository.save(session);
    }

    /**
     * Revoke all sessions for a user (force logout everywhere)
     */
    @Transactional
    @CacheEvict(value = "sessionRevocation", allEntries = true)
    public void revokeAllSessions(Long userId) {
        User user = getUserById(userId);
        userSessionRepository.revokeAllUserSessions(user);
    }

    /**
     * Cleanup expired and revoked sessions
     * Scheduled to run daily at 2 AM
     */
    @Transactional
    @Scheduled(cron = "0 0 2 * * *")
    @CacheEvict(value = "sessionRevocation", allEntries = true)
    public void cleanupExpiredAndRevokedSessions() {
        int deleted = userSessionRepository.cleanupExpiredAndRevokedSessions(LocalDateTime.now());
        log.info("Cleaned up {} expired and revoked sessions", deleted);
    }

    // Helper to fetch user or throw exception
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}