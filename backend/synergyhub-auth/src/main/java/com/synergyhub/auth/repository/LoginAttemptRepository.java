package com.synergyhub.auth.repository;

import com.synergyhub.auth.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Integer> {
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.email = :email AND la.success = false AND la.attemptedAt > :since")
    long countFailedAttemptsSince(@Param("email") String email, @Param("since") LocalDateTime since);
    
    void deleteByAttemptedAtBefore(LocalDateTime cutoff);
}