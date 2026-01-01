package com.synergyhub.repository;

import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Integer> {
    
    Optional<UserSession> findByTokenId(String tokenId);
    
    List<UserSession> findByUserAndRevokedFalse(User user);
    
    @Query("SELECT s FROM UserSession s WHERE s.user = :user AND s.revoked = false AND s.expiresAt > :now")
    List<UserSession> findActiveSessionsByUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.revoked = true WHERE s.user = :user AND s.revoked = false")
    void revokeAllUserSessions(@Param("user") User user);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.revoked = true WHERE s.tokenId = :tokenId")
    void revokeSessionByTokenId(@Param("tokenId") String tokenId);
    
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now OR s.revoked = true")
    int cleanupExpiredAndRevokedSessions(@Param("now") LocalDateTime now);
}