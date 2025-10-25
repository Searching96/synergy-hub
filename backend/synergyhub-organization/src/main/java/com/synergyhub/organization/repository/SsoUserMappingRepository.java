package com.synergyhub.organization.repository;

import com.synergyhub.organization.entity.SsoUserMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SsoUserMappingRepository extends JpaRepository<SsoUserMapping, Integer> {
    Optional<SsoUserMapping> findByUserIdAndSsoProviderId(Integer userId, Integer ssoProviderId);
    Optional<SsoUserMapping> findByExternalUserIdAndSsoProviderId(String externalUserId, Integer ssoProviderId);
}