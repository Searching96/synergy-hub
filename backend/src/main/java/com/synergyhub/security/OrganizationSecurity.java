package com.synergyhub.security;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;
import com.synergyhub.exception.UnauthorizedOrganizationAccessException;
import com.synergyhub.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component("organizationSecurity")
@RequiredArgsConstructor
@Slf4j
public class OrganizationSecurity {

    private final OrganizationRepository organizationRepository;
    private static final String GLOBAL_ADMIN_ROLE = "GLOBAL_ADMIN";
    private static final String ORG_ADMIN_ROLE = "ORG_ADMIN";

    /**
     * Only GLOBAL_ADMIN can create organizations.
     */
    public void requireCreateAccess(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(java.util.stream.Collectors.toSet());
        
        if (!roles.contains(GLOBAL_ADMIN_ROLE)) {
            log.warn("User {} attempted to create organization without GLOBAL_ADMIN role", user.getId());
            throw new UnauthorizedOrganizationAccessException(
                "Only GLOBAL_ADMIN users can create organizations"
            );
        }
    }

    /**
     * Only ORG_ADMIN of the specific organization can edit/delete.
     */
    public void requireEditAccess(Organization organization, User user) {
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(java.util.stream.Collectors.toSet());
        
        // Check if user is GLOBAL_ADMIN (has unrestricted access)
        if (roles.contains(GLOBAL_ADMIN_ROLE)) {
            return;
        }
        
        // Check if user is ORG_ADMIN of this specific organization
        boolean isOrgAdmin = organization.getUsers().stream()
                .filter(u -> u.getId().equals(user.getId()))
                .anyMatch(u -> u.getRoles().stream()
                        .map(r -> r.getName())
                        .collect(java.util.stream.Collectors.toSet())
                        .contains(ORG_ADMIN_ROLE)
                );
        
        if (!isOrgAdmin) {
            log.warn("User {} attempted to edit organization {} without ORG_ADMIN role in that org", 
                    user.getId(), organization.getId());
            throw new UnauthorizedOrganizationAccessException(
                organization.getId(), user.getId()
            );
        }
    }

    /**
     * Check if user has read access to organization.
     */
    public boolean hasReadAccess(Organization organization, User user) {
        if (organization == null || user == null) return false;
        
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(java.util.stream.Collectors.toSet());
        
        // GLOBAL_ADMIN has access to all organizations
        if (roles.contains(GLOBAL_ADMIN_ROLE)) {
            return true;
        }
        
        // User must belong to the organization
        return organization.getUsers().stream()
                .anyMatch(u -> u.getId().equals(user.getId()));
    }

    public void requireReadAccess(Organization organization, User user) {
        if (!hasReadAccess(organization, user)) {
            log.warn("User {} attempted to read organization {} without access", 
                    user.getId(), organization.getId());
            throw new UnauthorizedOrganizationAccessException(
                organization.getId(), user.getId()
            );
        }
    }
}
