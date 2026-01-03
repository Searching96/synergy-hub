package com.synergyhub.service.rbac;

import com.synergyhub.domain.entity.Role;
import com.synergyhub.dto.request.CreateRoleRequest;
import com.synergyhub.dto.request.UpdateRoleRequest;
import com.synergyhub.exception.RoleNameAlreadyExistsException;
import com.synergyhub.exception.RoleNotFoundException;
import com.synergyhub.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleStateService {

    private final RoleRepository roleRepository;

    /**
     * Create a new role with provided request details.
     * Database will enforce unique constraint on role name.
     */
    public Role createRole(CreateRoleRequest request) {
        log.debug("Creating role: {}", request.getName());
        
        if (roleRepository.existsByName(request.getName())) {
            throw new RoleNameAlreadyExistsException(request.getName());
        }
        
        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        
        Role created = roleRepository.save(role);
        log.info("Role created with ID: {}", created.getId());
        return created;
    }

    /**
     * Update existing role with new details.
     */
    public Role updateRole(Role role, UpdateRoleRequest request) {
        log.debug("Updating role ID: {} with name: {}", role.getId(), request.getName());
        
        // If name changed, check uniqueness
        if (!role.getName().equals(request.getName()) && 
                roleRepository.existsByName(request.getName())) {
            throw new RoleNameAlreadyExistsException(request.getName());
        }
        
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        
        Role updated = roleRepository.save(role);
        log.info("Role {} updated", role.getId());
        return updated;
    }

    /**
     * Delete a role.
     */
    public void deleteRole(Role role) {
        log.debug("Deleting role ID: {}", role.getId());
        roleRepository.delete(role);
        log.info("Role {} deleted", role.getId());
    }

    /**
     * Fetch role by ID.
     */
    public Role getRoleById(Integer roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));
    }

    /**
     * Fetch role by name.
     */
    public Role getRoleByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new RoleNotFoundException("Role '" + name + "' not found"));
    }
}
