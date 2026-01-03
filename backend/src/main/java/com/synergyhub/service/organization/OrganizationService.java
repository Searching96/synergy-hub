package com.synergyhub.service.organization;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.CreateOrganizationRequest;
import com.synergyhub.dto.request.UpdateOrganizationRequest;
import com.synergyhub.dto.response.OrganizationResponse;
import com.synergyhub.events.organization.OrganizationCreatedEvent;
import com.synergyhub.events.organization.OrganizationUpdatedEvent;
import com.synergyhub.exception.OrganizationNameAlreadyExistsException;
import com.synergyhub.exception.OrganizationNotFoundException;
import com.synergyhub.repository.OrganizationRepository;
import com.synergyhub.security.OrganizationSecurity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationLifecycleService organizationLifecycleService;
    private final OrganizationRepository organizationRepository;
    private final OrganizationSecurity organizationSecurity;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * FACADE: Orchestrates organization creation.
     * 1. Check GLOBAL_ADMIN access
     * 2. Validate name uniqueness
     * 3. Delegate to lifecycle service for creation
     * 4. Publish event for auditing/side effects
     */
    @Transactional
    public OrganizationResponse createOrganization(
            CreateOrganizationRequest request,
            User actor,
            String ipAddress) {
        
        log.info("Creating organization: {}", request.getName());
        
        // SECURITY GUARD: Check access first
        organizationSecurity.requireCreateAccess(actor);
        
        // Validate name uniqueness
        if (organizationRepository.existsByName(request.getName())) {
            throw new OrganizationNameAlreadyExistsException(request.getName());
        }
        
        // DELEGATE: Lifecycle service handles creation
        Organization organization = organizationLifecycleService.createOrganization(request);
        
        // EVENT-DRIVEN: Publish event for auditing
        eventPublisher.publishEvent(
                new OrganizationCreatedEvent(organization, actor, ipAddress)
        );
        
        return mapToResponse(organization);
    }

    /**
     * FACADE: Orchestrates organization retrieval.
     * 1. Fetch organization
     * 2. Check read access
     * 3. Return response
     */
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(Integer organizationId, User actor) {
        log.info("Fetching organization: {}", organizationId);
        
        Organization organization = organizationLifecycleService.getOrganizationById(organizationId);
        
        // SECURITY GUARD: Check access
        organizationSecurity.requireReadAccess(organization, actor);
        
        return mapToResponse(organization);
    }

    /**
     * FACADE: Orchestrates organization update.
     * 1. Fetch organization
     * 2. Check edit access
     * 3. Validate name uniqueness (if name changed)
     * 4. Delegate to lifecycle service for update
     * 5. Publish event for auditing
     */
    @Transactional
    public OrganizationResponse updateOrganization(
            Integer organizationId,
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
     * 1. Fetch organization
     * 2. Check edit access
     * 3. Delegate to lifecycle service for deletion
     * 4. Publish event for auditing
     */
    @Transactional
    public void deleteOrganization(Integer organizationId, User actor, String ipAddress) {
        log.info("Deleting organization: {}", organizationId);
        
        Organization organization = organizationLifecycleService.getOrganizationById(organizationId);
        
        // SECURITY GUARD: Check access first
        organizationSecurity.requireEditAccess(organization, actor);
        
        // DELEGATE: Lifecycle service handles deletion
        organizationLifecycleService.deleteOrganization(organization);
        
        // EVENT-DRIVEN: Publish event for auditing
        // Note: Publishing before deletion for audit trail
        eventPublisher.publishEvent(
                new OrganizationUpdatedEvent(organization, actor, ipAddress)
        );
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
                .userCount((long) organization.getUsers().size())
                .build();
    }
}
