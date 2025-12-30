package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.Role;
import com.synergyhub.dto.request.RegisterRequest;
import com.synergyhub.exception.ResourceNotFoundException;
import com.synergyhub.repository.OrganizationRepository;
import com.synergyhub.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Default implementation of UserProvisioningStrategy using current logic.
 */
@Component
@RequiredArgsConstructor
public class DefaultUserProvisioningStrategy implements UserProvisioningStrategy {
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;

    @Value("${app.default-organization-id:1}")
    private Integer defaultOrganizationId;

    @Value("${app.default-role-name:Team Member}")
    private String defaultRoleName;

    @Override
    public Organization determineOrganization(RegisterRequest request) {
        Integer orgId = request.getOrganizationId() != null ? request.getOrganizationId() : defaultOrganizationId;
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));
    }

    @Override
    public Role determineDefaultRole(RegisterRequest request) {
        return roleRepository.findByName(defaultRoleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", defaultRoleName));
    }
}
