package com.synergyhub.service.rbac;

import com.synergyhub.domain.entity.Role;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.CreateRoleRequest;
import com.synergyhub.dto.request.UpdateRoleRequest;
import com.synergyhub.dto.request.AssignPermissionsRequest;
import com.synergyhub.dto.response.RoleResponse;
import com.synergyhub.events.rbac.RoleCreatedEvent;
import com.synergyhub.events.rbac.RoleDeletedEvent;
import com.synergyhub.events.rbac.RolePermissionChangedEvent;
import com.synergyhub.events.rbac.RoleUpdatedEvent;
import com.synergyhub.repository.RoleRepository;
import com.synergyhub.security.RbacSecurity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleManagementFacade {

    private final RoleStateService roleStateService;
    private final PermissionAssignmentService permissionAssignmentService;
    private final RoleRepository roleRepository;
    private final RbacSecurity rbacSecurity;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * FACADE: Create a new role.
     * 1. Check role management access (ORG_ADMIN or GLOBAL_ADMIN)
     * 2. Validate role name uniqueness
     * 3. Delegate to RoleStateService for creation
     * 4. Assign initial permissions
     * 5. Publish event for auditing
     */
    @Transactional
    public RoleResponse createRole(
            Long organizationId,
            CreateRoleRequest request,
            User actor,
            String ipAddress) {
        
        log.info("Creating role: {} in organization: {}", request.getName(), organizationId);
        
        // SECURITY GUARD: Check access first
        rbacSecurity.requireRoleManagementAccess(organizationId, actor);
        
        // DELEGATE: Lifecycle service creates role
        Role role = roleStateService.createRole(organizationId, request);
        
        // DELEGATE: Assign initial permissions
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            permissionAssignmentService.assignPermissionsToRole(role, request.getPermissionIds());
            role = roleStateService.getRoleById(role.getId()); // Refresh to get permissions
        }
        
        // EVENT-DRIVEN: Publish event for auditing
        eventPublisher.publishEvent(
                new RoleCreatedEvent(role, actor, ipAddress)
        );
        
        return mapToResponse(role);
    }

    /**
     * FACADE: Update an existing role.
     * 1. Fetch role
     * 2. Check role management access
     * 3. Validate that role is not a system role
     * 4. Delegate to RoleStateService for update
     * 5. Publish event for auditing
     */
    @Transactional
    public RoleResponse updateRole(
            Long organizationId,
            Long roleId,
            UpdateRoleRequest request,
            User actor,
            String ipAddress) {
        
        log.info("Updating role: {} in organization: {}", roleId, organizationId);
        
        Role role = roleStateService.getRoleById(roleId);
        
        // SECURITY GUARD: Check access
        rbacSecurity.requireRoleManagementAccess(organizationId, actor);
        
        // SECURITY GUARD: Prevent system role modification
        rbacSecurity.validateRoleModification(role);
        
        // DELEGATE: Update role
        Role updated = roleStateService.updateRole(role, request, organizationId);
        
        // DELEGATE: Update permissions
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            permissionAssignmentService.assignPermissionsToRole(updated, request.getPermissionIds());
            updated = roleStateService.getRoleById(updated.getId()); // Refresh
        }
        
        // EVENT-DRIVEN: Publish event for auditing
        eventPublisher.publishEvent(
                new RoleUpdatedEvent(updated, actor, ipAddress)
        );
        
        return mapToResponse(updated);
    }

    /**
     * FACADE: Delete a role.
     * 1. Fetch role
     * 2. Check role management access
     * 3. Validate that role is not a system role
     * 4. Delegate to RoleStateService for deletion
     * 5. Publish event for auditing
     */
    @Transactional
    public void deleteRole(
            Long organizationId,
            Long roleId,
            User actor,
            String ipAddress) {
        
        log.info("Deleting role: {} in organization: {}", roleId, organizationId);
        
        Role role = roleStateService.getRoleById(roleId);
        
        // SECURITY GUARD: Check access
        rbacSecurity.requireRoleManagementAccess(organizationId, actor);
        
        // SECURITY GUARD: Prevent system role deletion
        rbacSecurity.validateRoleModification(role);
        
        // DELEGATE: Delete role
        roleStateService.deleteRole(role);
        
        // EVENT-DRIVEN: Publish event for auditing
        eventPublisher.publishEvent(
                new RoleDeletedEvent(role, actor, ipAddress)
        );
    }

    /**
     * FACADE: Assign permissions to a role.
     * 1. Fetch role
     * 2. Check role management access
     * 3. Validate that role is not a system role
     * 4. Delegate to PermissionAssignmentService
     * 5. Publish security event for auditing
     */
    @Transactional
    public RoleResponse assignPermissionsToRole(
            Long organizationId,
            Long roleId,
            AssignPermissionsRequest request,
            User actor,
            String ipAddress) {
        
        log.info("Assigning {} permissions to role: {} in organization: {}", 
                request.getPermissionIds().size(), roleId, organizationId);
        
        Role role = roleStateService.getRoleById(roleId);
        
        // SECURITY GUARD: Check access
        rbacSecurity.requireRoleManagementAccess(organizationId, actor);
        
        // SECURITY GUARD: Prevent system role modification
        rbacSecurity.validateRoleModification(role);
        
        // DELEGATE: Assign permissions
        permissionAssignmentService.assignPermissionsToRole(role, request.getPermissionIds());
        Role updated = roleStateService.getRoleById(roleId); // Refresh
        
        // EVENT-DRIVEN: Publish security event (critical for audit)
        eventPublisher.publishEvent(
                new RolePermissionChangedEvent(
                        updated,
                        "ASSIGNED",
                        request.getPermissionIds(),
                        actor,
                        ipAddress
                )
        );
        
        return mapToResponse(updated);
    }

    /**
     * FACADE: Get a single role by ID.
     * 1. Fetch role
     * 2. Return as response DTO
     */
    @Transactional(readOnly = true)
        public RoleResponse getRole(Long roleId) {
        log.info("Fetching role: {}", roleId);
        Role role = roleStateService.getRoleById(roleId);
        return mapToResponse(role);
    }

    /**
     * FACADE: Get all roles.
     */
    @Transactional(readOnly = true)
        public List<RoleResponse> getAllRoles(Long organizationId) {
                log.info("Fetching all roles for org: {}", organizationId);
                return roleRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * FACADE: Get all non-system roles (editable roles).
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> getEditableRoles() {
        log.info("Fetching all editable (non-system) roles");
        return roleRepository.findAll().stream()
                .filter(role -> !rbacSecurity.isSystemRole(role))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert Role entity to RoleResponse DTO.
     */
    private RoleResponse mapToResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(role.getPermissions().stream()
                        .map(p -> new com.synergyhub.dto.response.PermissionResponse(
                                p.getId(),
                                p.getName(),
                                p.getDescription()
                        ))
                        .collect(Collectors.toSet())
                )
                .isSystemRole(rbacSecurity.isSystemRole(role))
                .build();
    }
}
