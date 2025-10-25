package com.synergyhub.auth.repository;

import com.synergyhub.auth.entity.PasswordResetToken;
import com.synergyhub.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
    Optional<PasswordResetToken> findByToken(String token);
    
    void deleteByUser(User user);
    
    void deleteByExpiryTimeBefore(LocalDateTime now);
}