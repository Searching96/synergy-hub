package com.synergyhub.repository;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.SsoProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SsoProviderRepository extends JpaRepository<SsoProvider, Integer> {
    
    List<SsoProvider> findByOrganizationAndEnabledTrue(Organization organization);
    
    Optional<SsoProvider> findByOrganizationAndProviderName(Organization organization, String providerName);
}