package com.synergyhub.repository;

import com.synergyhub.domain.entity.SsoProvider;
import com.synergyhub.domain.entity.SsoUserMapping;
import com.synergyhub.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SsoUserMappingRepository extends JpaRepository<SsoUserMapping, Long> {
    
    Optional<SsoUserMapping> findByProviderAndExternalId(SsoProvider provider, String externalId);
    
    Optional<SsoUserMapping> findByUser(User user);
}