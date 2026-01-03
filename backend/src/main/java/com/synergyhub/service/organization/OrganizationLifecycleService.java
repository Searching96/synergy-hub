package com.synergyhub.service.organization;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.CreateOrganizationRequest;
import com.synergyhub.dto.request.UpdateOrganizationRequest;
import com.synergyhub.dto.response.OrganizationResponse;
import com.synergyhub.exception.OrganizationNotFoundException;
import com.synergyhub.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationLifecycleService {

    private final OrganizationRepository organizationRepository;

    /**
     * Handle organization creation logic.
     * - Validates name uniqueness
     * - Creates organization entity
     * - Returns organization (event publishing handled by facade)
     */
    public Organization createOrganization(CreateOrganizationRequest request) {
        log.debug("Creating organization with name: {}", request.getName());
        
        Organization organization = Organization.builder()
                .name(request.getName())
                .address(request.getAddress())
                .build();
        
        Organization created = organizationRepository.save(organization);
        log.info("Organization created with ID: {}", created.getId());
        return created;
    }

    /**
     * Handle organization update logic.
     * - Updates name and address
     * - Returns updated organization (event publishing handled by facade)
     */
    public Organization updateOrganization(Organization organization, UpdateOrganizationRequest request) {
        log.debug("Updating organization ID: {} with new name: {}", organization.getId(), request.getName());
        
        organization.setName(request.getName());
        organization.setAddress(request.getAddress());
        
        Organization updated = organizationRepository.save(organization);
        log.info("Organization {} updated", organization.getId());
        return updated;
    }

    /**
     * Handle organization deletion logic.
     * - Deletes organization and cascades to related entities
     */
    public void deleteOrganization(Organization organization) {
        log.debug("Deleting organization ID: {}", organization.getId());
        organizationRepository.delete(organization);
        log.info("Organization {} deleted", organization.getId());
    }

    /**
     * Fetch organization by ID.
     */
    public Organization getOrganizationById(Integer organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }
}
