package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.Role;
import com.synergyhub.dto.request.RegisterRequest;

/**
 * Strategy interface for provisioning new users (organization, roles, etc).
 * Allows RegistrationService to be decoupled from hardcoded org/role logic.
 */
public interface UserProvisioningStrategy {
    Organization determineOrganization(RegisterRequest request);
    Role determineDefaultRole(RegisterRequest request, Organization organization);
}
