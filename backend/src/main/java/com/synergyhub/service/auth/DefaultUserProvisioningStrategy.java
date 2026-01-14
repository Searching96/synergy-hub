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

    @Value("${app.default-role-name:TEAM_MEMBER}")
    private String defaultRoleName;

    @Override
    public Organization determineOrganization(RegisterRequest request) {
        // If new organization name is provided, create a new organization
        if (request.getNewOrganizationName() != null && !request.getNewOrganizationName().isBlank()) {
            if (organizationRepository.existsByName(request.getNewOrganizationName())) {
                throw new ResourceNotFoundException("Organization", "name", request.getNewOrganizationName() + " already exists");
            }
            Organization newOrg = Organization.builder()
                    .name(request.getNewOrganizationName())
                    .address(request.getNewOrganizationAddress())
                    .build();
            return organizationRepository.save(newOrg);
        }
        // Otherwise, join existing organization by ID (invite flow)
        Long orgId = request.getOrganizationId() != null ? request.getOrganizationId() : defaultOrganizationId;
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));
    }

    @Override
    public Role determineDefaultRole(RegisterRequest request, Organization organization) {
        return roleRepository.findByNameAndOrganizationId(defaultRoleName, organization.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role", "name", defaultRoleName + " in organization " + organization.getId()
                ));
    }
}
