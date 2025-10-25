package com.synergyhub.organization.service;

import com.synergyhub.common.exception.ResourceNotFoundException;
import com.synergyhub.organization.dto.SsoProviderConfigRequest;
import com.synergyhub.organization.dto.SsoProviderResponse;
import com.synergyhub.organization.entity.SsoProvider;
import com.synergyhub.organization.repository.OrganizationRepository;
import com.synergyhub.organization.repository.SsoProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SsoProviderService {

    private final SsoProviderRepository ssoProviderRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional
    public SsoProviderResponse configureSsoProvider(Integer organizationId, SsoProviderConfigRequest request) {
        log.info("Configuring SSO provider for organization: {}", organizationId);

        // Verify organization exists
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found with ID: " + organizationId);
        }

        SsoProvider provider = SsoProvider.builder()
                .organizationId(organizationId)
                .providerType(request.getProviderType())
                .providerName(request.getProviderName())
                .entityId(request.getEntityId())
                .ssoUrl(request.getSsoUrl())
                .sloUrl(request.getSloUrl())
                .x509Certificate(request.getX509Certificate())
                .clientId(request.getClientId())
                .clientSecret(request.getClientSecret())
                .authorizationEndpoint(request.getAuthorizationEndpoint())
                .tokenEndpoint(request.getTokenEndpoint())
                .userinfoEndpoint(request.getUserinfoEndpoint())
                .jwksUri(request.getJwksUri())
                .build();

        SsoProvider saved = ssoProviderRepository.save(provider);
        log.info("SSO provider configured with ID: {}", saved.getSsoProviderId());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SsoProviderResponse> getProvidersByOrganization(Integer organizationId) {
        return ssoProviderRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SsoProviderResponse getActiveProvider(Integer organizationId) {
        SsoProvider provider = ssoProviderRepository.findByOrganizationIdAndIsActiveTrue(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("No active SSO provider found for organization: " + organizationId));
        return mapToResponse(provider);
    }

    @Transactional
    public SsoProviderResponse updateProvider(Integer providerId, SsoProviderConfigRequest request) {
        log.info("Updating SSO provider: {}", providerId);

        SsoProvider provider = ssoProviderRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("SSO provider not found with ID: " + providerId));

        provider.setProviderType(request.getProviderType());
        provider.setProviderName(request.getProviderName());
        provider.setEntityId(request.getEntityId());
        provider.setSsoUrl(request.getSsoUrl());
        provider.setSloUrl(request.getSloUrl());
        provider.setX509Certificate(request.getX509Certificate());
        provider.setClientId(request.getClientId());
        provider.setClientSecret(request.getClientSecret());
        provider.setAuthorizationEndpoint(request.getAuthorizationEndpoint());
        provider.setTokenEndpoint(request.getTokenEndpoint());
        provider.setUserinfoEndpoint(request.getUserinfoEndpoint());
        provider.setJwksUri(request.getJwksUri());

        SsoProvider updated = ssoProviderRepository.save(provider);
        log.info("SSO provider updated: {}", updated.getSsoProviderId());

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteProvider(Integer providerId) {
        log.info("Deleting SSO provider: {}", providerId);

        if (!ssoProviderRepository.existsById(providerId)) {
            throw new ResourceNotFoundException("SSO provider not found with ID: " + providerId);
        }

        ssoProviderRepository.deleteById(providerId);
        log.info("SSO provider deleted: {}", providerId);
    }

    @Transactional
    public void toggleProviderStatus(Integer providerId, boolean active) {
        SsoProvider provider = ssoProviderRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("SSO provider not found with ID: " + providerId));

        provider.setIsActive(active);
        ssoProviderRepository.save(provider);
        log.info("SSO provider {} status changed to: {}", providerId, active);
    }

    private SsoProviderResponse mapToResponse(SsoProvider provider) {
        return SsoProviderResponse.builder()
                .ssoProviderId(provider.getSsoProviderId())
                .organizationId(provider.getOrganizationId())
                .providerType(provider.getProviderType())
                .providerName(provider.getProviderName())
                .isActive(provider.getIsActive())
                .createdAt(provider.getCreatedAt())
                .updatedAt(provider.getUpdatedAt())
                .build();
    }
}