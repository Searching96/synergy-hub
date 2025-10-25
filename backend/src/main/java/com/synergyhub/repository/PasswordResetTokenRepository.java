package com.synergyhub.repository;

import com.synergyhub.domain.entity.PasswordResetToken;
import com.synergyhub.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    List<PasswordResetToken> findByUser(User user);
    
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PasswordResetToken p SET p.used = true WHERE p.user = :user AND p.used = false")
    void invalidateAllUserTokens(@Param("user") User user);
    
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiryTime < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}