package com.synergyhub.service.rbac;

import com.synergyhub.domain.entity.Permission;
import com.synergyhub.domain.entity.Role;
import com.synergyhub.exception.PermissionNotFoundException;
import com.synergyhub.repository.PermissionRepository;
import com.synergyhub.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionAssignmentService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    /**
     * Assign permissions to a role.
     * Replaces all existing permissions with the provided set.
     */
    public void assignPermissionsToRole(Role role, Set<Integer> permissionIds) {
        log.debug("Assigning {} permissions to role ID: {}", permissionIds.size(), role.getId());
        
        // Fetch all permission entities by IDs
        Set<Permission> permissions = permissionIds.stream()
                .map(id -> permissionRepository.findById(id)
                        .orElseThrow(() -> new PermissionNotFoundException(id)))
                .collect(Collectors.toSet());
        
        // Replace permissions
        role.setPermissions(permissions);
        roleRepository.save(role);
        
        log.info("Assigned {} permissions to role {}", permissions.size(), role.getId());
    }

    /**
     * Add a single permission to a role.
     */
    public void addPermissionToRole(Role role, Integer permissionId) {
        log.debug("Adding permission {} to role ID: {}", permissionId, role.getId());
        
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException(permissionId));
        
        role.getPermissions().add(permission);
        roleRepository.save(role);
        
        log.info("Added permission {} to role {}", permissionId, role.getId());
    }

    /**
     * Remove a single permission from a role.
     */
    public void removePermissionFromRole(Role role, Integer permissionId) {
        log.debug("Removing permission {} from role ID: {}", permissionId, role.getId());
        
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException(permissionId));
        
        role.getPermissions().remove(permission);
        roleRepository.save(role);
        
        log.info("Removed permission {} from role {}", permissionId, role.getId());
    }

    /**
     * Clear all permissions from a role.
     */
    public void clearPermissionsFromRole(Role role) {
        log.debug("Clearing all permissions from role ID: {}", role.getId());
        
        role.getPermissions().clear();
        roleRepository.save(role);
        
        log.info("Cleared all permissions from role {}", role.getId());
    }

    /**
     * Get all permissions for a role.
     */
    public Set<Permission> getPermissionsForRole(Role role) {
        return role.getPermissions();
    }
}
