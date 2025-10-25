package com.synergyhub.organization.repository;

import com.synergyhub.organization.entity.OrganizationInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationInvitationRepository extends JpaRepository<OrganizationInvitation, Integer> {
    Optional<OrganizationInvitation> findByToken(String token);
    List<OrganizationInvitation> findByOrganizationIdAndStatus(Integer organizationId, OrganizationInvitation.InvitationStatus status);
    boolean existsByEmailAndOrganizationIdAndStatus(String email, Integer organizationId, OrganizationInvitation.InvitationStatus status);
}