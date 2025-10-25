package com.synergyhub.repository;

import com.synergyhub.domain.entity.TwoFactorSecret;
import com.synergyhub.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TwoFactorSecretRepository extends JpaRepository<TwoFactorSecret, Integer> {
    
    Optional<TwoFactorSecret> findByUser(User user);
    
    Optional<TwoFactorSecret> findByUserId(Integer userId);
    
    void deleteByUser(User user);
}