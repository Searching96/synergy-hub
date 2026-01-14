package com.synergyhub.repository;

import com.synergyhub.domain.entity.JoinRequest;
import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {
    
    List<JoinRequest> findByOrganizationAndStatus(Organization organization, String status);
    
    Optional<JoinRequest> findByUserAndOrganization(User user, Organization organization);
    
    boolean existsByUserAndOrganization(User user, Organization organization);
}
