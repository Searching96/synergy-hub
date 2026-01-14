package com.synergyhub.repository;

import com.synergyhub.domain.entity.TwoFactorSecret;
import com.synergyhub.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TwoFactorSecretRepository extends JpaRepository<TwoFactorSecret, Long> {
    
    Optional<TwoFactorSecret> findByUser(User user);
    
    Optional<TwoFactorSecret> findByUserId(Long userId);
    
    void deleteByUser(User user);
}