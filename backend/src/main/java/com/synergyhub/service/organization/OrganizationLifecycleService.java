package com.synergyhub.service.organization;

import com.synergyhub.domain.entity.JoinRequest;
import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.CreateOrganizationRequest;
import com.synergyhub.dto.request.UpdateOrganizationRequest;
import com.synergyhub.exception.InvalidInviteCodeException;
import com.synergyhub.exception.OrganizationNotFoundException;
import com.synergyhub.repository.JoinRequestRepository;
import com.synergyhub.repository.OrganizationRepository;
import com.synergyhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationLifecycleService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final JoinRequestRepository joinRequestRepository;

    /**
     * Handle organization creation logic.
     */
    public Organization createOrganization(CreateOrganizationRequest request) {
        log.debug("Creating organization with name: {}", request.getName());
        
        Organization organization = Organization.builder()
                .name(request.getName())
                .address(request.getAddress())
                .contactEmail(request.getContactEmail())
                .build();
        
        Organization created = organizationRepository.save(organization);
        log.info("Organization created with ID: {}", created.getId());
        return created;
    }

    /**
     * Handle organization update logic.
     */
    public Organization updateOrganization(Organization organization, UpdateOrganizationRequest request) {
        log.debug("Updating organization ID: {} with new name: {}", organization.getId(), request.getName());
        
        organization.setName(request.getName());
        organization.setAddress(request.getAddress());
        organization.setContactEmail(request.getContactEmail());
        
        Organization updated = organizationRepository.save(organization);
        log.info("Organization {} updated", organization.getId());
        return updated;
    }

    /**
     * Handle organization deletion logic.
     */
    public void deleteOrganization(Organization organization) {
        log.debug("Deleting organization ID: {}", organization.getId());
        organizationRepository.delete(organization);
        log.info("Organization {} deleted", organization.getId());
    }

    /**
     * Fetch organization by ID.
     */
    public Organization getOrganizationById(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    /**
     * Join organization with invite code
     */
    public Organization joinWithInviteCode(User user, String inviteCode) {
        log.debug("User {} joining organization with invite code", user.getId());
        
        // Find organization by invite code
        Organization organization = organizationRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new InvalidInviteCodeException("Invalid or expired invite code"));
        
        // Check if invite code is expired
        if (organization.getInviteCodeExpiresAt() != null && 
            organization.getInviteCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidInviteCodeException("Invite code has expired");
        }
        
        // Add user to organization
        user.setOrganization(organization);
        userRepository.save(user);
        
        log.info("User {} joined organization {}", user.getId(), organization.getId());
        
        return organization;
    }

    /**
     * Create join request
     */
    public JoinRequest createJoinRequest(User user, Organization organization) {
        log.debug("Creating join request for user {} to organization {}", 
            user.getId(), organization.getId());
        
        JoinRequest joinRequest = JoinRequest.builder()
                .user(user)
                .organization(organization)
                .status("PENDING")
                .requestedAt(LocalDateTime.now())
                .build();
        
        JoinRequest saved = joinRequestRepository.save(joinRequest);
        log.info("Join request created with ID: {}", saved.getId());
        
        return saved;
    }

    /**
     * Approve join request
     */
    public void approveJoinRequest(User user, Organization organization, User admin) {
        log.debug("Approving join request for user {} to organization {}", 
            user.getId(), organization.getId());
        
        JoinRequest joinRequest = joinRequestRepository
                .findByUserAndOrganization(user, organization)
                .orElseThrow(() -> new IllegalStateException("Join request not found"));
        
        // Add user to organization
        user.setOrganization(organization);
        userRepository.save(user);
        
        // Update request status
        joinRequest.setStatus("APPROVED");
        joinRequest.setApprovedBy(admin);
        joinRequest.setApprovedAt(LocalDateTime.now());
        joinRequestRepository.save(joinRequest);
        
        log.info("Join request approved for user {} to organization {}", 
            user.getId(), organization.getId());
    }

    /**
     * Reject join request
     */
    public void rejectJoinRequest(User user, Organization organization, User admin) {
        log.debug("Rejecting join request for user {} to organization {}", 
            user.getId(), organization.getId());
        
        JoinRequest joinRequest = joinRequestRepository
                .findByUserAndOrganization(user, organization)
                .orElseThrow(() -> new IllegalStateException("Join request not found"));
        
        // Update request status
        joinRequest.setStatus("REJECTED");
        joinRequest.setApprovedBy(admin);
        joinRequest.setApprovedAt(LocalDateTime.now());
        joinRequestRepository.save(joinRequest);
        
        log.info("Join request rejected for user {} to organization {}", 
            user.getId(), organization.getId());
    }

    /**
     * Generate unique invite code for organization
     */
    public String generateInviteCode(Organization organization) {
        log.debug("Generating invite code for organization: {}", organization.getId());
        
        // Generate unique invite code
        String inviteCode = generateUniqueInviteCode(organization.getName());
        
        // Set expiry (7 days from now)
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        
        organization.setInviteCode(inviteCode);
        organization.setInviteCodeExpiresAt(expiresAt);
        organizationRepository.save(organization);
        
        log.info("Invite code generated: {} (expires: {})", inviteCode, expiresAt);
        
        return inviteCode;
    }

    /**
     * Generate unique invite code
     * Format: ACME-123456
     */
    private String generateUniqueInviteCode(String orgName) {
        // Extract prefix from organization name (first 4 uppercase letters/numbers)
        String prefix = orgName.toUpperCase()
                .replaceAll("[^A-Z0-9]", "")
                .substring(0, Math.min(4, orgName.replaceAll("[^A-Z0-9]", "").length()));
        
        // If prefix is too short, pad with 'X'
        while (prefix.length() < 4) {
            prefix += "X";
        }
        
        // Generate random 6-digit number
        SecureRandom random = new SecureRandom();
        String randomPart = String.format("%06d", random.nextInt(1000000));
        
        String inviteCode = prefix + "-" + randomPart;
        
        // Ensure uniqueness (very unlikely to collide, but check anyway)
        while (organizationRepository.findByInviteCode(inviteCode).isPresent()) {
            randomPart = String.format("%06d", random.nextInt(1000000));
            inviteCode = prefix + "-" + randomPart;
        }
        
        return inviteCode;
    }
}