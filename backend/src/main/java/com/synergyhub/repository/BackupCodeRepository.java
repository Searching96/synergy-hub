package com.synergyhub.repository;

import com.synergyhub.domain.entity.BackupCode;
import com.synergyhub.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BackupCodeRepository extends JpaRepository<BackupCode, Long> {
    
    // Fetch all unused codes for a user
    List<BackupCode> findByUserAndUsedFalse(User user);

    // Find a specific code for a user (to verify)
    Optional<BackupCode> findByUserAndCodeAndUsedFalse(User user, String code);

    // Optional: Delete all codes for a user (when regenerating)
    void deleteByUser(User user);
}