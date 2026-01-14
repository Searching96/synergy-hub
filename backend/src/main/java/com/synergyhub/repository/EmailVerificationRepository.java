package com.synergyhub.repository;

import com.synergyhub.domain.entity.EmailVerification;
import com.synergyhub.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    
    Optional<EmailVerification> findByToken(String token);
    
    Optional<EmailVerification> findByUserAndVerifiedFalse(User user);
    
    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.expiryTime < :now")
    void deleteExpiredVerifications(@Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = false, flushAutomatically = false)
    @Query("UPDATE EmailVerification e SET e.verified = TRUE WHERE e.id = :id")
    int markVerified(@Param("id") Long id);
}