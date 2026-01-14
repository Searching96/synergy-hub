package com.synergyhub.repository;

import com.synergyhub.domain.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    
    Optional<Organization> findByName(String name);
    
    boolean existsByName(String name);

    // NEW: Add these methods
    Optional<Organization> findByInviteCode(String inviteCode);

    Optional<Organization> findByContactEmail(String contactEmail);
}