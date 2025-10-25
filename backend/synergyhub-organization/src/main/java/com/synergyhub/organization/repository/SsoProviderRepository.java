package com.synergyhub.organization.repository;

import com.synergyhub.organization.entity.SsoProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SsoProviderRepository extends JpaRepository<SsoProvider, Integer> {
    List<SsoProvider> findByOrganizationId(Integer organizationId);
    Optional<SsoProvider> findByOrganizationIdAndIsActiveTrue(Integer organizationId);
}