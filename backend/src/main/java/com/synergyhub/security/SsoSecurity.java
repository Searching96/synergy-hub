package com.synergyhub.security;

import com.synergyhub.domain.entity.User;
import com.synergyhub.exception.UnauthorizedOrganizationAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Security Guard for SSO Provider Management.
 * Only Organization Admins can view/manage SSO settings.
 *
 * Adheres to architectural standard:
 * - All authorization checks happen here
 * - Called first in every service operation
 * - Clear separation of security from business logic
 */
@Component("ssoSecurity")
@RequiredArgsConstructor
@Slf4j
public class SsoSecurity {

    private static final String GLOBAL_ADMIN_ROLE = "GLOBAL_ADMIN";
    private static final String ORG_ADMIN_ROLE = "ORG_ADMIN";

    /**
     * Verify that user is an Admin (ORG_ADMIN or GLOBAL_ADMIN) of the organization.
     * Required to manage SSO settings within that organization.
     *
     * @param organizationId Target organization ID
     * @param user Authenticated user
     * @throws UnauthorizedOrganizationAccessException if user lacks required role
     */
    public void requireSsoManagementAccess(Long organizationId, User user) {
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(java.util.stream.Collectors.toSet());
        
        // GLOBAL_ADMIN has access to all SSO management
        if (roles.contains(GLOBAL_ADMIN_ROLE)) {
            log.debug("GLOBAL_ADMIN user {} granted SSO management access to org {}", 
                    user.getId(), organizationId);
            return;
        }
        
        // ORG_ADMIN of this specific organization can manage SSO
        boolean isOrgAdminOfOrg = user.getOrganization() != null &&
                user.getOrganization().getId().equals(organizationId) &&
                roles.contains(ORG_ADMIN_ROLE);
        
        if (!isOrgAdminOfOrg) {
            log.warn("User {} attempted to manage SSO without admin access in org {}", 
                    user.getId(), organizationId);
            throw new UnauthorizedOrganizationAccessException(
                    "Only ORG_ADMIN or GLOBAL_ADMIN can manage SSO settings"
            );
        }
        
        log.debug("ORG_ADMIN user {} granted SSO management access to org {}", 
                user.getId(), organizationId);
    }

    /**
     * Verify that user can view SSO configuration for an organization.
     * Same as management access (Org Admins only).
     *
     * @param organizationId Target organization ID
     * @param user Authenticated user
     * @throws UnauthorizedOrganizationAccessException if user lacks required role
     */
    public void requireSsoViewAccess(Long organizationId, User user) {
        // Same as management access for now (only admins can view SSO config)
        requireSsoManagementAccess(organizationId, user);
    }
}

