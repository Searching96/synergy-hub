package com.synergyhub.auth.repository;

import com.synergyhub.auth.entity.TwoFactorSecret;
import com.synergyhub.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TwoFactorSecretRepository extends JpaRepository<TwoFactorSecret, Integer> {
    Optional<TwoFactorSecret> findByUser(User user);
    
    void deleteByUser(User user);
}