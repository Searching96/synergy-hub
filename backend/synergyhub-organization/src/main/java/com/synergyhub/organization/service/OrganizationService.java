package com.synergyhub.organization.service;

import com.synergyhub.common.exception.ResourceAlreadyExistsException;
import com.synergyhub.common.exception.ResourceNotFoundException;
import com.synergyhub.organization.dto.OrganizationCreateRequest;
import com.synergyhub.organization.dto.OrganizationResponse;
import com.synergyhub.organization.dto.OrganizationUpdateRequest;
import com.synergyhub.organization.entity.Organization;
import com.synergyhub.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    @Transactional
    public OrganizationResponse createOrganization(OrganizationCreateRequest request) {
        log.info("Creating organization: {}", request.getName());

        if (organizationRepository.existsByName(request.getName())) {
            throw new ResourceAlreadyExistsException("Organization with name '" + request.getName() + "' already exists");
        }

        String slug = request.getSlug();
        if (slug != null && organizationRepository.existsBySlug(slug)) {
            throw new ResourceAlreadyExistsException("Organization with slug '" + slug + "' already exists");
        }

        Organization organization = Organization.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .contactEmail(request.getContactEmail())
                .websiteUrl(request.getWebsiteUrl())
                .maxUsers(request.getMaxUsers())
                .build();

        Organization saved = organizationRepository.save(organization);
        log.info("Organization created with ID: {}", saved.getOrganizationId());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationById(Integer id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + id));
        return mapToResponse(organization);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationBySlug(String slug) {
        Organization organization = organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with slug: " + slug));
        return mapToResponse(organization);
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> getAllOrganizations() {
        return organizationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrganizationResponse updateOrganization(Integer id, OrganizationUpdateRequest request) {
        log.info("Updating organization with ID: {}", id);

        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + id));

        if (request.getName() != null) {
            if (!request.getName().equals(organization.getName()) &&
                    organizationRepository.existsByName(request.getName())) {
                throw new ResourceAlreadyExistsException("Organization with name '" + request.getName() + "' already exists");
            }
            organization.setName(request.getName());
        }

        if (request.getDescription() != null) {
            organization.setDescription(request.getDescription());
        }

        if (request.getContactEmail() != null) {
            organization.setContactEmail(request.getContactEmail());
        }

        if (request.getWebsiteUrl() != null) {
            organization.setWebsiteUrl(request.getWebsiteUrl());
        }

        if (request.getLogoUrl() != null) {
            organization.setLogoUrl(request.getLogoUrl());
        }

        if (request.getMaxUsers() != null) {
            organization.setMaxUsers(request.getMaxUsers());
        }

        if (request.getSsoEnabled() != null) {
            organization.setSsoEnabled(request.getSsoEnabled());
        }

        if (request.getIsActive() != null) {
            organization.setIsActive(request.getIsActive());
        }

        Organization updated = organizationRepository.save(organization);
        log.info("Organization updated: {}", updated.getOrganizationId());

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteOrganization(Integer id) {
        log.info("Deleting organization with ID: {}", id);

        if (!organizationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Organization not found with ID: " + id);
        }

        organizationRepository.deleteById(id);
        log.info("Organization deleted: {}", id);
    }

    private OrganizationResponse mapToResponse(Organization organization) {
        return OrganizationResponse.builder()
                .organizationId(organization.getOrganizationId())
                .name(organization.getName())
                .slug(organization.getSlug())
                .description(organization.getDescription())
                .logoUrl(organization.getLogoUrl())
                .websiteUrl(organization.getWebsiteUrl())
                .contactEmail(organization.getContactEmail())
                .maxUsers(organization.getMaxUsers())
                .ssoEnabled(organization.getSsoEnabled())
                .isActive(organization.getIsActive())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }
}