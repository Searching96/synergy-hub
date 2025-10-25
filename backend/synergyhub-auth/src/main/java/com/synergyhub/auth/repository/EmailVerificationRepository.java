package com.synergyhub.auth.repository;

import com.synergyhub.auth.entity.EmailVerification;
import com.synergyhub.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Integer> {
    Optional<EmailVerification> findByToken(String token);
    
    void deleteByUser(User user);
}