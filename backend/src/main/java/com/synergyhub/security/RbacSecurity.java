package com.synergyhub.security;

import com.synergyhub.domain.entity.Role;
import com.synergyhub.domain.entity.User;
import com.synergyhub.exception.SystemRoleModificationException;
import com.synergyhub.exception.UnauthorizedRoleManagementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component("rbacSecurity")
@RequiredArgsConstructor
@Slf4j
public class RbacSecurity {

    private static final Set<String> SYSTEM_ROLES = Set.of("GLOBAL_ADMIN", "ORG_ADMIN", "GUEST");
    private static final String ORG_ADMIN_ROLE = "ORG_ADMIN";

    /**
     * Verify that user is an Admin (ORG_ADMIN or GLOBAL_ADMIN) of the organization.
     * Required to manage roles within that organization.
     */
    public void requireRoleManagementAccess(Long organizationId, User user) {
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(java.util.stream.Collectors.toSet());
        
        // GLOBAL_ADMIN has access to all role management
        if (roles.contains("GLOBAL_ADMIN")) {
            return;
        }
        
        // ORG_ADMIN of this specific organization can manage roles
        Long currentOrgId = OrganizationContext.getcurrentOrgId();
        boolean isOrgAdminOfOrg = currentOrgId != null && currentOrgId.equals(organizationId) &&
                roles.contains(ORG_ADMIN_ROLE);
        
        if (!isOrgAdminOfOrg) {
            log.warn("User {} attempted to manage roles without admin access in org {}", 
                    user.getId(), organizationId);
            throw new UnauthorizedRoleManagementException(organizationId);
        }
    }

    /**
     * Prevent modification of system roles (GLOBAL_ADMIN, ORG_ADMIN, GUEST).
     * System roles are protected and can only be read, not modified or deleted.
     */
    public void validateRoleModification(Role role) {
        if (isSystemRole(role.getName())) {
            log.warn("Attempted to modify system role: {}", role.getName());
            throw new SystemRoleModificationException(role.getName());
        }
    }

    /**
     * Check if a role is a system role.
     */
    public boolean isSystemRole(String roleName) {
        return SYSTEM_ROLES.contains(roleName);
    }

    /**
     * Check if a role is a system role (by Role entity).
     */
    public boolean isSystemRole(Role role) {
        return isSystemRole(role.getName());
    }
}