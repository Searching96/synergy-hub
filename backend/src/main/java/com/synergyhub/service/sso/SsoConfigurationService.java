package com.synergyhub.service.sso;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.SsoProvider;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.RegisterSsoProviderRequest;
import com.synergyhub.dto.request.UpdateSsoProviderRequest;
import com.synergyhub.dto.request.RotateSsoSecretRequest;
import com.synergyhub.dto.response.SsoProviderResponse;
import com.synergyhub.events.sso.*;
import com.synergyhub.exception.ResourceNotFoundException;
import com.synergyhub.repository.OrganizationRepository;
import com.synergyhub.repository.SsoProviderRepository;
import com.synergyhub.security.SsoSecurity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Facade Service for SSO Provider Management.
 *
 * Architectural Pattern:
 * - FACADE: Single entry point for all SSO operations
 * - Orchestrates security checks and domain operations
 * - Publishes events for audit logging
 * - All operations are transactional
 *
 * Security Flow:
 * 1. Check access via SsoSecurity (guard called first)
 * 2. Validate business logic
 * 3. Delegate to repository operations
 * 4. Publish events for side effects (auditing)
 * 5. Return DTO response
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SsoConfigurationService {

    private final SsoProviderRepository ssoProviderRepository;
    private final OrganizationRepository organizationRepository;
    private final SsoSecurity ssoSecurity;
    private final ApplicationEventPublisher eventPublisher;

    // ========== CREATE OPERATION ==========

    /**
     * Register a new SSO provider for an organization.
     *
     * Security: Only ORG_ADMIN or GLOBAL_ADMIN
     * Event: SsoProviderRegisteredEvent (for auditing)
     */
        @Transactional
    public SsoProviderResponse registerSsoProvider(
            Long organizationId,
            RegisterSsoProviderRequest request,
            User actor,
            String ipAddress) {
        
        log.info("Registering SSO provider {} for organization {}", request.getProviderName(), organizationId);
        
        // SECURITY GUARD: Check access first
        ssoSecurity.requireSsoManagementAccess(organizationId, actor);
        
        // VALIDATION: Fetch organization
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        
        // VALIDATION: Check provider name uniqueness within organization
        ssoProviderRepository.findByOrganizationAndProviderName(organization, request.getProviderName())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "SSO provider with name '" + request.getProviderName() + "' already exists in this organization"
                    );
                });
        
        // DELEGATE: Create SSO provider entity
        SsoProvider provider = SsoProvider.builder()
                .organization(organization)
                .providerType(request.getProviderType())
                .providerName(request.getProviderName())
                .clientId(request.getClientId())
                .clientSecret(request.getClientSecret())
                .metadataUrl(request.getMetadataUrl())
                .enabled(true)
                .build();
        
        SsoProvider savedProvider = ssoProviderRepository.save(provider);
        log.info("SSO provider registered with ID: {}", savedProvider.getId());
        
        // EVENT-DRIVEN: Publish event for auditing
        eventPublisher.publishEvent(
                new SsoProviderRegisteredEvent(savedProvider, actor, ipAddress)
        );
        
        return mapToResponse(savedProvider);
    }

    // ========== READ OPERATIONS ==========

    /**
     * Get all SSO providers for an organization.
     *
     * Security: Only ORG_ADMIN or GLOBAL_ADMIN
     */
        @Transactional(readOnly = true)
    public List<SsoProviderResponse> getSsoProviders(Long organizationId, User actor) {
        log.debug("Fetching SSO providers for organization {}", organizationId);
        
        // SECURITY GUARD: Check access first
        ssoSecurity.requireSsoViewAccess(organizationId, actor);
        
        // DELEGATE: Query repository
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        
        List<SsoProvider> providers = ssoProviderRepository.findByOrganization(organization);
        
        return providers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific SSO provider by ID.
     *
     * Security: Only ORG_ADMIN or GLOBAL_ADMIN
     */
        @Transactional(readOnly = true)
    public SsoProviderResponse getSsoProvider(Long organizationId, Long providerId, User actor) {
        log.debug("Fetching SSO provider {} for organization {}", providerId, organizationId);
        
        // SECURITY GUARD: Check access first
        ssoSecurity.requireSsoViewAccess(organizationId, actor);
        
        // DELEGATE: Fetch provider
        SsoProvider provider = ssoProviderRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("SSO Provider not found"));
        
        // VALIDATION: Ensure provider belongs to the organization
        if (!provider.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("SSO Provider not found in this organization");
        }
        
        return mapToResponse(provider);
    }

    // ========== UPDATE OPERATION ==========

    /**
     * Update an existing SSO provider configuration.
     *
     * Security: Only ORG_ADMIN or GLOBAL_ADMIN
     * Event: SsoProviderUpdatedEvent (for auditing)
     */
        @Transactional
    public SsoProviderResponse updateSsoProvider(
            Long organizationId,
            Long providerId,
            UpdateSsoProviderRequest request,
            User actor,
            String ipAddress) {
        
        log.info("Updating SSO provider {} in organization {}", providerId, organizationId);
        
        // SECURITY GUARD: Check access first
        ssoSecurity.requireSsoManagementAccess(organizationId, actor);
        
        // DELEGATE: Fetch provider
        SsoProvider provider = ssoProviderRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("SSO Provider not found"));
        
        // VALIDATION: Ensure provider belongs to the organization
        if (!provider.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("SSO Provider not found in this organization");
        }
        
        // VALIDATION: Check for name conflicts if name is being updated
        if (request.getProviderName() != null && !request.getProviderName().equals(provider.getProviderName())) {
            ssoProviderRepository.findByOrganizationAndProviderName(
                    provider.getOrganization(), request.getProviderName()
            ).ifPresent(existing -> {
                throw new IllegalArgumentException(
                        "SSO provider with name '" + request.getProviderName() + "' already exists"
                );
            });
        }
        
        // UPDATE: Apply changes
        if (request.getProviderName() != null) {
            provider.setProviderName(request.getProviderName());
        }
        if (request.getClientId() != null) {
            provider.setClientId(request.getClientId());
        }
        if (request.getClientSecret() != null) {
            provider.setClientSecret(request.getClientSecret());
        }
        if (request.getMetadataUrl() != null) {
            provider.setMetadataUrl(request.getMetadataUrl());
        }
        
        SsoProvider updatedProvider = ssoProviderRepository.save(provider);
        log.info("SSO provider {} updated", providerId);
        
        // EVENT-DRIVEN: Publish event for auditing
        eventPublisher.publishEvent(
                new SsoProviderUpdatedEvent(updatedProvider, actor, ipAddress)
        );
        
        return mapToResponse(updatedProvider);
    }

    // ========== SECRET ROTATION ==========

    /**
     * Rotate the client secret for an SSO provider.
     * Security-critical operation - rotates credential without full update.
     *
     * Security: Only ORG_ADMIN or GLOBAL_ADMIN
     * Event: SsoProviderSecretRotatedEvent (marked CRITICAL for compliance)
     */
        @Transactional
    public SsoProviderResponse rotateSsoSecret(
            Long organizationId,
            Long providerId,
            RotateSsoSecretRequest request,
            User actor,
            String ipAddress) {
        
        log.info("Rotating secret for SSO provider {} in organization {}", providerId, organizationId);
        
        // SECURITY GUARD: Check access first
        ssoSecurity.requireSsoManagementAccess(organizationId, actor);
        
        // DELEGATE: Fetch provider
        SsoProvider provider = ssoProviderRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("SSO Provider not found"));
        
        // VALIDATION: Ensure provider belongs to the organization
        if (!provider.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("SSO Provider not found in this organization");
        }
        
        // UPDATE: Rotate secret
        provider.setClientSecret(request.getNewClientSecret());
        SsoProvider updatedProvider = ssoProviderRepository.save(provider);
        log.info("Secret rotated for SSO provider {}", providerId);
        
        // EVENT-DRIVEN: Publish CRITICAL event for auditing (security operation)
        eventPublisher.publishEvent(
                new SsoProviderSecretRotatedEvent(updatedProvider, actor, ipAddress)
        );
        
        return mapToResponse(updatedProvider);
    }

    // ========== STATE MANAGEMENT ==========

    /**
     * Enable an SSO provider.
     *
     * Security: Only ORG_ADMIN or GLOBAL_ADMIN
     * Event: SsoProviderStateChangedEvent (for auditing)
     */
        @Transactional
    public SsoProviderResponse enableSsoProvider(
            Long organizationId,
            Long providerId,
            User actor,
            String ipAddress) {
        
        log.info("Enabling SSO provider {} in organization {}", providerId, organizationId);
        
        return changeProviderState(organizationId, providerId, true, actor, ipAddress);
    }

    /**
     * Disable an SSO provider.
     *
     * Security: Only ORG_ADMIN or GLOBAL_ADMIN
     * Event: SsoProviderStateChangedEvent (for auditing)
     */
        @Transactional
    public SsoProviderResponse disableSsoProvider(
            Long organizationId,
            Long providerId,
            User actor,
            String ipAddress) {
        
        log.info("Disabling SSO provider {} in organization {}", providerId, organizationId);
        
        return changeProviderState(organizationId, providerId, false, actor, ipAddress);
    }

        private SsoProviderResponse changeProviderState(
            Long organizationId,
            Long providerId,
            Boolean enabled,
            User actor,
            String ipAddress) {
        
        // SECURITY GUARD: Check access first
        ssoSecurity.requireSsoManagementAccess(organizationId, actor);
        
        // DELEGATE: Fetch provider
        SsoProvider provider = ssoProviderRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("SSO Provider not found"));
        
        // VALIDATION: Ensure provider belongs to the organization
        if (!provider.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("SSO Provider not found in this organization");
        }
        
        // VALIDATION: Check if state is already set
        if (provider.getEnabled().equals(enabled)) {
            log.debug("Provider {} is already in desired state: {}", providerId, enabled);
        }
        
        // UPDATE: Change state
        provider.setEnabled(enabled);
        SsoProvider updatedProvider = ssoProviderRepository.save(provider);
        log.info("SSO provider {} state changed to: {}", providerId, enabled);
        
        // EVENT-DRIVEN: Publish event for auditing
        eventPublisher.publishEvent(
                new SsoProviderStateChangedEvent(updatedProvider, enabled, actor, ipAddress)
        );
        
        return mapToResponse(updatedProvider);
    }

    // ========== DELETE OPERATION ==========

    /**
     * Delete an SSO provider.
     *
     * Security: Only ORG_ADMIN or GLOBAL_ADMIN
     * Event: SsoProviderDeletedEvent (for auditing)
     */
        @Transactional
    public void deleteSsoProvider(
            Long organizationId,
            Long providerId,
            User actor,
            String ipAddress) {
        
        log.info("Deleting SSO provider {} from organization {}", providerId, organizationId);
        
        // SECURITY GUARD: Check access first
        ssoSecurity.requireSsoManagementAccess(organizationId, actor);
        
        // DELEGATE: Fetch provider
        SsoProvider provider = ssoProviderRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("SSO Provider not found"));
        
        // VALIDATION: Ensure provider belongs to the organization
        if (!provider.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("SSO Provider not found in this organization");
        }
        
        // DELETE: Remove from database (cascades to user mappings)
        ssoProviderRepository.delete(provider);
        log.info("SSO provider {} deleted", providerId);
        
        // EVENT-DRIVEN: Publish event for auditing
        eventPublisher.publishEvent(
                new SsoProviderDeletedEvent(provider, actor, ipAddress)
        );
    }

    // ========== HELPER METHODS ==========

    /**
     * Map SsoProvider entity to DTO response.
     * Excludes sensitive fields like clientSecret.
     */
    private SsoProviderResponse mapToResponse(SsoProvider provider) {
        return SsoProviderResponse.builder()
                .id(provider.getId())
                .providerType(provider.getProviderType())
                .providerName(provider.getProviderName())
                .clientId(provider.getClientId())
                .clientSecret(null) // Never expose in response
                .metadataUrl(provider.getMetadataUrl())
                .enabled(provider.getEnabled())
                .createdAt(provider.getCreatedAt())
                .build();
    }
}
