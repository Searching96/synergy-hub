package com.synergyhub.organization.repository;

import com.synergyhub.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Integer> {
    Optional<Organization> findBySlug(String slug);
    boolean existsByName(String name);
    boolean existsBySlug(String slug);
}