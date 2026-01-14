package com.synergyhub.repository;

import com.synergyhub.domain.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    
    @Query("SELECT la FROM LoginAttempt la WHERE la.email = :email AND la.attemptedAt > :since ORDER BY la.attemptedAt DESC")
    List<LoginAttempt> findRecentAttemptsByEmail(@Param("email") String email, @Param("since") LocalDateTime since);
    
    @Query("SELECT la FROM LoginAttempt la WHERE la.ipAddress = :ip AND la.attemptedAt > :since ORDER BY la.attemptedAt DESC")
    List<LoginAttempt> findRecentAttemptsByIp(@Param("ip") String ip, @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.email = :email AND la.success = false AND la.attemptedAt > :since")
    long countFailedAttemptsByEmail(@Param("email") String email, @Param("since") LocalDateTime since);
    
    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.attemptedAt < :before")
    void deleteOldAttempts(@Param("before") LocalDateTime before);

    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.ipAddress = :ipAddress AND la.success = false AND la.attemptedAt > :since")
    long countFailedAttemptsByIpAddress(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.attemptedAt < :before")
    long countByAttemptedAtBefore(@Param("before") LocalDateTime before);

    List<LoginAttempt> findByEmailOrderByAttemptedAtDesc(String email);

    List<LoginAttempt> findByIpAddressAndAttemptedAtAfter(String ipAddress, LocalDateTime since);

}