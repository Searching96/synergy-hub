package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.CreateRoleRequest;
import com.synergyhub.dto.request.UpdateRoleRequest;
import com.synergyhub.dto.request.AssignPermissionsRequest;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.RoleResponse;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.rbac.RoleManagementFacade;
import com.synergyhub.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{organizationId}/roles")
@RequiredArgsConstructor
@Slf4j
@Validated
public class RoleController {

    private final RoleManagementFacade roleManagementFacade;
    private final UserRepository userRepository;
    private final ClientIpResolver ipResolver;

    /**
     * CREATE: POST /api/organizations/{organizationId}/roles
     * Creates a new role (ORG_ADMIN or GLOBAL_ADMIN only).
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @PathVariable @Positive(message = "Organization ID must be positive") Long organizationId,
            @Valid @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        
        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        
        log.info("Creating role in organization: {}", organizationId);
        RoleResponse response = roleManagementFacade.createRole(organizationId, request, user, ipAddress);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Role created successfully", response));
    }

    /**
     * READ: GET /api/organizations/{organizationId}/roles
     * Lists all roles in the organization.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles(
            @PathVariable @Positive(message = "Organization ID must be positive") Long organizationId) {
        
        log.info("Fetching all roles in organization: {}", organizationId);
        List<RoleResponse> responses = roleManagementFacade.getAllRoles(organizationId);
        
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", responses));
    }

    /**
     * READ: GET /api/organizations/{organizationId}/roles/{roleId}
     * Retrieves a specific role.
     */
    @GetMapping("/{roleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RoleResponse>> getRole(
            @PathVariable @Positive(message = "Organization ID must be positive") Long organizationId,
            @PathVariable @Positive(message = "Role ID must be positive") Long roleId) {
        
        log.info("Fetching role: {} in organization: {}", roleId, organizationId);
        RoleResponse response = roleManagementFacade.getRole(roleId);
        
        return ResponseEntity.ok(ApiResponse.success("Role retrieved successfully", response));
    }

    /**
     * UPDATE: PUT /api/organizations/{organizationId}/roles/{roleId}
     * Updates a role (ORG_ADMIN or GLOBAL_ADMIN only, cannot modify system roles).
     */
    @PutMapping("/{roleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable @Positive(message = "Organization ID must be positive") Long organizationId,
            @PathVariable @Positive(message = "Role ID must be positive") Long roleId,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        
        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        
        log.info("Updating role: {} in organization: {}", roleId, organizationId);
        RoleResponse response = roleManagementFacade.updateRole(organizationId, roleId, request, user, ipAddress);
        
        return ResponseEntity.ok(ApiResponse.success("Role updated successfully", response));
    }

    /**
     * DELETE: DELETE /api/organizations/{organizationId}/roles/{roleId}
     * Deletes a role (ORG_ADMIN or GLOBAL_ADMIN only, cannot delete system roles).
     */
    @DeleteMapping("/{roleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @PathVariable @Positive(message = "Organization ID must be positive") Long organizationId,
            @PathVariable @Positive(message = "Role ID must be positive") Long roleId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        
        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        
        log.info("Deleting role: {} in organization: {}", roleId, organizationId);
        roleManagementFacade.deleteRole(organizationId, roleId, user, ipAddress);
        
        return ResponseEntity.ok(ApiResponse.success("Role deleted successfully", null));
    }

    /**
     * POST: POST /api/organizations/{organizationId}/roles/{roleId}/permissions
     * Assign permissions to a role (ORG_ADMIN or GLOBAL_ADMIN only).
     */
    @PostMapping("/{roleId}/permissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RoleResponse>> assignPermissionsToRole(
            @PathVariable @Positive(message = "Organization ID must be positive") Long organizationId,
            @PathVariable @Positive(message = "Role ID must be positive") Long roleId,
            @Valid @RequestBody AssignPermissionsRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        
        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        
        log.info("Assigning permissions to role: {} in organization: {}", roleId, organizationId);
        RoleResponse response = roleManagementFacade.assignPermissionsToRole(organizationId, roleId, request, user, ipAddress);
        
        return ResponseEntity.ok(ApiResponse.success("Permissions assigned successfully", response));
    }

    /**
     * Helper method to extract User from UserPrincipal.
     * Eagerly fetches roles and permissions to avoid LazyInitializationException.
     */
    private User getUser(UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return userRepository.findByIdWithRolesAndPermissions(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
