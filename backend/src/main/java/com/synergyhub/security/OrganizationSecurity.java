package com.synergyhub.security;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.entity.UserOrganization;
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
    private final com.synergyhub.repository.UserOrganizationRepository userOrganizationRepository; // ✅ Inject
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
        
        // Check if user is ORG_ADMIN of this specific organization via explicit membership check
        boolean isOrgAdmin = organization.getMemberships().stream()
                .filter(m -> m.getUser().getId().equals(user.getId()))
                .anyMatch(m -> m.getRole() != null && ORG_ADMIN_ROLE.equals(m.getRole().getName()));
        
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
        return organization.getMemberships().stream()
                .anyMatch(m -> m.getUser().getId().equals(user.getId()));
    }
    
    // ✅ NEW methods for PreAuthorize
    public boolean hasOrganizationAccess(Long organizationId, UserPrincipal principal) {
        if (organizationId == null || principal == null) return false;
        
        // Check local roles from principal (approximate) or fetch user
        // Ideally we just check the repo
        return hasOrganizationAccess(organizationId, principal.getId());
    }

    public boolean hasOrganizationAccess(Long organizationId, User user) {
        if (organizationId == null || user == null) return false;
        
        // If user is GLOBAL_ADMIN, allow
        boolean isGlobalAdmin = user.getRoles().stream().anyMatch(r -> GLOBAL_ADMIN_ROLE.equals(r.getName()));
        if (isGlobalAdmin) return true;

        return hasOrganizationAccess(organizationId, user.getId());
    }

    public boolean hasOrganizationAccess(Long organizationId, Long userId) {
        return userOrganizationRepository.existsByIdUserIdAndIdOrganizationId(userId, organizationId);
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
