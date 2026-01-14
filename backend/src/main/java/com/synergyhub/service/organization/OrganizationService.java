package com.synergyhub.service.organization;

import com.synergyhub.domain.entity.JoinRequest;
import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.entity.UserOrganization;
import com.synergyhub.domain.entity.UserOrganization.MembershipStatus;
import com.synergyhub.domain.entity.UserOrganizationId;
import com.synergyhub.dto.request.CreateOrganizationRequest;
import com.synergyhub.dto.request.UpdateOrganizationRequest;
import com.synergyhub.dto.response.OrganizationResponse;
import com.synergyhub.dto.response.UserResponse;
import com.synergyhub.events.organization.OrganizationCreatedEvent;
import com.synergyhub.events.organization.OrganizationUpdatedEvent;
import com.synergyhub.events.organization.UserJoinedOrganizationEvent;
import com.synergyhub.exception.*;
import com.synergyhub.repository.JoinRequestRepository;
import com.synergyhub.repository.OrganizationRepository;
import com.synergyhub.domain.entity.Role;
import com.synergyhub.repository.*;
import com.synergyhub.security.OrganizationSecurity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationLifecycleService organizationLifecycleService;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final RoleRepository roleRepository;
    private final OrganizationSecurity organizationSecurity;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * FACADE: Orchestrates organization creation.
     * 1. Validate name uniqueness
     * 2. Delegate to lifecycle service for creation
     * 3. Auto-assign creator to organization
     * 4. Publish event for auditing/side effects
     */
    @Transactional
    public OrganizationResponse createOrganization(
            CreateOrganizationRequest request,
            User actor,
            String ipAddress) {
        log.info("Creating organization: {} by user: {}", request.getName(), actor.getId());

        if (organizationRepository.existsByName(request.getName())) {
            throw new OrganizationNameAlreadyExistsException(request.getName());
        }

        Organization organization = organizationLifecycleService.createOrganization(request);

        // Add user to organization with PRIMARY flag
        UserOrganization userOrg = UserOrganization.builder()
                .id(new UserOrganizationId(actor.getId(), organization.getId()))
                .user(actor)
                .organization(organization)
                .isPrimary(true)
                .status(MembershipStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();
        userOrganizationRepository.save(userOrg);

        // Assign ORG_ADMIN role
        assignOrgAdminRole(actor, organization, userOrg);

        eventPublisher.publishEvent(
                new OrganizationCreatedEvent(organization, actor, ipAddress)
        );

        return mapToResponse(organization);
    }

    /**
     * FACADE: Orchestrates organization retrieval.
     */
    @Transactional(readOnly = true)
        public OrganizationResponse getOrganization(Long organizationId, User actor) {
        log.info("Fetching organization: {}", organizationId);

        Organization organization = organizationLifecycleService.getOrganizationById(organizationId);

        // SECURITY GUARD: Check access
        organizationSecurity.requireReadAccess(organization, actor);

        return mapToResponse(organization);
    }

    /**
     * FACADE: Orchestrates organization update.
     */
    @Transactional
    public OrganizationResponse updateOrganization(
            Long organizationId,
            UpdateOrganizationRequest request,
            User actor,
            String ipAddress) {

        log.info("Updating organization: {}", organizationId);

        Organization organization = organizationLifecycleService.getOrganizationById(organizationId);

        // SECURITY GUARD: Check access first
        organizationSecurity.requireEditAccess(organization, actor);

        // Validate name uniqueness if name changed
        if (!organization.getName().equals(request.getName()) &&
                organizationRepository.existsByName(request.getName())) {
            throw new OrganizationNameAlreadyExistsException(request.getName());
        }

        // DELEGATE: Lifecycle service handles update
        Organization updated = organizationLifecycleService.updateOrganization(organization, request);

        // EVENT-DRIVEN: Publish event for auditing
        eventPublisher.publishEvent(
                new OrganizationUpdatedEvent(updated, actor, ipAddress)
        );

        return mapToResponse(updated);
    }

    /**
     * FACADE: Orchestrates organization deletion.
     */
    @Transactional
        public void deleteOrganization(Long organizationId, User actor, String ipAddress) {
        log.info("Deleting organization: {}", organizationId);

        Organization organization = organizationLifecycleService.getOrganizationById(organizationId);

        // SECURITY GUARD: Check access first
        organizationSecurity.requireEditAccess(organization, actor);

        // DELEGATE: Lifecycle service handles deletion
        organizationLifecycleService.deleteOrganization(organization);

        // EVENT-DRIVEN: Publish event for auditing
        eventPublisher.publishEvent(
                new OrganizationUpdatedEvent(organization, actor, ipAddress)
        );
    }

    /**
     * Join organization using invite code
     */
    @Transactional
    public OrganizationResponse joinOrganization(
            String inviteCode,
            User user,
            String ipAddress) {
        log.info("User {} attempting to join organization with invite code", user.getId());

        // Delegate to lifecycle service which handles lookup, expiration check, and user update
        Organization org = organizationLifecycleService.joinWithInviteCode(user, inviteCode);

        // Check/create user organization membership record if not exists (Lifecycle service updates User entity relation, but we track history/roles in UserOrganization)
        // Ideally Lifecycle service should handle this too, but for refactor safety we keep the role assignment logic here or ensure Lifecycle does it.
        // Looking at LifecycleService.joinWithInviteCode:
        //    user.setOrganization(organization);
        //    userRepository.save(user);
        // It updates the User->Org relationship. We still need to create the UserOrganization entry for roles.

        if (userOrganizationRepository.existsByIdUserIdAndIdOrganizationId(user.getId(), org.getId())) {
             // This might be redundant if lifecycle checked it, but safe to keep
             // Actually Lifecycle just overwrites user.setOrganization.
        } else {
             UserOrganization userOrg = UserOrganization.builder()
                .id(new UserOrganizationId(user.getId(), org.getId()))
                .user(user)
                .organization(org)
                .isPrimary(userOrganizationRepository.findByIdUserId(user.getId()).isEmpty())
                .status(MembershipStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();
            userOrganizationRepository.save(userOrg);
            
            // Assign default TEAM_MEMBER role
            assignDefaultRole(user, org, userOrg);
        }

        eventPublisher.publishEvent(
                new UserJoinedOrganizationEvent(org, user, ipAddress)
        );

        log.info("User {} successfully joined organization {}", user.getId(), org.getId());
        return mapToResponse(org);
    }

    /**
     * Request to join organization via email
     */
    @Transactional
    public void requestJoinOrganization(
            String organizationEmail,
            User user,
            String ipAddress) {

        log.info("User {} requesting to join organization via email: {}",
                user.getId(), organizationEmail);



        // Find organization by contact email
        Organization organization = organizationRepository.findByContactEmail(organizationEmail)
                .orElseThrow(() -> new OrganizationNotFoundException(
                        "No organization found with email: " + organizationEmail
                ));

        // Check if request already exists
        if (joinRequestRepository.existsByUserAndOrganization(user, organization)) {
            throw new IllegalStateException("Join request already exists");
        }

        // DELEGATE: Lifecycle service handles request creation
        organizationLifecycleService.createJoinRequest(user, organization);

        log.info("Join request created for user {} to organization {}",
                user.getId(), organization.getId());

        // TODO: Send email notification to organization admins
        // emailService.notifyAdminsOfJoinRequest(organization, user);
    }

    /**
     * Generate invite code for organization
     */
    @Transactional
        public String generateInviteCode(Long organizationId, User actor) {
        log.info("Generating invite code for organization: {}", organizationId);

        Organization organization = organizationLifecycleService.getOrganizationById(organizationId);

        // Check admin access
        organizationSecurity.requireEditAccess(organization, actor);

        // DELEGATE: Lifecycle service generates invite code
        String inviteCode = organizationLifecycleService.generateInviteCode(organization);

        log.info("Invite code generated for organization {}: {}", organizationId, inviteCode);

        return inviteCode;
    }

    /**
     * Get pending join requests for organization
     */
    @Transactional(readOnly = true)
        public List<UserResponse> getPendingJoinRequests(Long organizationId, User actor) {
        log.info("Fetching pending join requests for organization: {}", organizationId);

        Organization organization = organizationLifecycleService.getOrganizationById(organizationId);

        // Check admin access
        organizationSecurity.requireEditAccess(organization, actor);

        List<JoinRequest> requests = joinRequestRepository
                .findByOrganizationAndStatus(organization, "PENDING");

        return requests.stream()
                .map(request -> mapUserToResponse(request.getUser()))
                .collect(Collectors.toList());
    }

    /**
     * Approve join request
     */
    @Transactional
    public void approveJoinRequest(
            Long organizationId,
            Long userId,
            User admin,
            String ipAddress) {

        log.info("Admin {} approving join request for user {} in org {}",
                admin.getId(), userId, organizationId);

        Organization organization = organizationLifecycleService.getOrganizationById(organizationId);

        // Check admin access
        organizationSecurity.requireEditAccess(organization, admin);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // DELEGATE: Lifecycle service handles approval
        organizationLifecycleService.approveJoinRequest(user, organization, admin);

        // Publish event
        eventPublisher.publishEvent(
                new UserJoinedOrganizationEvent(organization, user, ipAddress)
        );

        log.info("User {} approved to join organization {}", userId, organizationId);

        // TODO: Send email notification to user
        // emailService.notifyUserOfApproval(user, organization);
    }

    /**
     * Reject join request
     */
    @Transactional
    public void rejectJoinRequest(
            Long organizationId,
            Long userId,
            User admin,
            String ipAddress) {

        log.info("Admin {} rejecting join request for user {} in org {}",
                admin.getId(), userId, organizationId);

        Organization organization = organizationLifecycleService.getOrganizationById(organizationId);

        // Check admin access
        organizationSecurity.requireEditAccess(organization, admin);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // DELEGATE: Lifecycle service handles rejection
        organizationLifecycleService.rejectJoinRequest(user, organization, admin);

        log.info("Join request rejected for user {} to organization {}", userId, organizationId);

        // TODO: Send email notification to user
        // emailService.notifyUserOfRejection(user, organization);
    }

    /**
     * Map User to UserResponse
     */
    private UserResponse mapUserToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    /**
     * Convert Organization entity to OrganizationResponse DTO.
     */
    private OrganizationResponse mapToResponse(Organization organization) {
        return OrganizationResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .address(organization.getAddress())
                .createdAt(organization.getCreatedAt())
                .userCount((long) organization.getMemberships().size())
                .build();
    }

    private void assignOrgAdminRole(User user, Organization organization, UserOrganization userOrg) {
        // Ensure roles exist
        if (!roleRepository.existsByNameAndOrganizationId("ORG_ADMIN", organization.getId())) {
            createDefaultRoles(organization);
        }

        Role adminRole = roleRepository.findByNameAndOrganizationId("ORG_ADMIN", organization.getId())
                .orElseThrow(() -> new IllegalStateException("ORG_ADMIN role not found after creation"));
        
        userOrg.setRole(adminRole);
        userOrganizationRepository.save(userOrg);
    }

    private void assignDefaultRole(User user, Organization organization, UserOrganization userOrg) {
        Role memberRole = roleRepository.findByNameAndOrganizationId("TEAM_MEMBER", organization.getId())
                .orElse(null);
        
        if (memberRole == null) {
            // Should exist if created properly, but to be safe:
            if (!roleRepository.existsByNameAndOrganizationId("ORG_ADMIN", organization.getId())) {
                 createDefaultRoles(organization);
                 memberRole = roleRepository.findByNameAndOrganizationId("TEAM_MEMBER", organization.getId()).orElse(null);
            }
        }

        if (memberRole != null) {
            userOrg.setRole(memberRole);
            userOrganizationRepository.save(userOrg);
        }
    }

    private void createDefaultRoles(Organization organization) {
         Role adminRole = Role.builder()
                 .name("ORG_ADMIN")
                 .description("Organization Administrator with full access")
                 .organization(organization)
                 .build();
         
         Role memberRole = Role.builder()
                 .name("TEAM_MEMBER")
                 .description("Standard team member")
                 .organization(organization)
                 .build();
         
         roleRepository.saveAll(List.of(adminRole, memberRole));
    }
}