package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.CreateOrganizationRequest;
import com.synergyhub.dto.request.UpdateOrganizationRequest;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.OrganizationResponse;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.organization.OrganizationService;
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

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Slf4j
@Validated
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserRepository userRepository;
    private final ClientIpResolver ipResolver;

    /**
     * CREATE: POST /api/organizations
     * Creates a new organization (GLOBAL_ADMIN only).
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        
        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        
        log.info("Creating organization: {}", request.getName());
        OrganizationResponse response = organizationService.createOrganization(request, user, ipAddress);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Organization created successfully", response));
    }

    /**
     * READ: GET /api/organizations/{organizationId}
     * Retrieves an organization (ORG_ADMIN or GLOBAL_ADMIN of that org).
     */
    @GetMapping("/{organizationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganization(
            @PathVariable @Positive(message = "Organization ID must be positive") Integer organizationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        User user = getUser(principal);
        
        log.info("Fetching organization: {}", organizationId);
        OrganizationResponse response = organizationService.getOrganization(organizationId, user);
        
        return ResponseEntity.ok(ApiResponse.success("Organization retrieved successfully", response));
    }

    /**
     * UPDATE: PUT /api/organizations/{organizationId}
     * Updates an organization (ORG_ADMIN or GLOBAL_ADMIN of that org).
     */
    @PutMapping("/{organizationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateOrganization(
            @PathVariable @Positive(message = "Organization ID must be positive") Integer organizationId,
            @Valid @RequestBody UpdateOrganizationRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        
        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        
        log.info("Updating organization: {}", organizationId);
        OrganizationResponse response = organizationService.updateOrganization(organizationId, request, user, ipAddress);
        
        return ResponseEntity.ok(ApiResponse.success("Organization updated successfully", response));
    }

    /**
     * DELETE: DELETE /api/organizations/{organizationId}
     * Deletes an organization (ORG_ADMIN or GLOBAL_ADMIN of that org).
     */
    @DeleteMapping("/{organizationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(
            @PathVariable @Positive(message = "Organization ID must be positive") Integer organizationId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        
        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        
        log.info("Deleting organization: {}", organizationId);
        organizationService.deleteOrganization(organizationId, user, ipAddress);
        
        return ResponseEntity.ok(ApiResponse.success("Organization deleted successfully", null));
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
