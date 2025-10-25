package com.synergyhub.auth.repository;

import com.synergyhub.auth.entity.User;
import com.synergyhub.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Integer> {
    Optional<UserSession> findByTokenId(String tokenId);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.revoked = true WHERE s.user = :user")
    void revokeAllUserSessions(@Param("user") User user);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.revoked = true WHERE s.tokenId = :tokenId")
    void revokeSession(@Param("tokenId") String tokenId);
}