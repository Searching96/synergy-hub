package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.RegisterSsoProviderRequest;
import com.synergyhub.dto.request.UpdateSsoProviderRequest;
import com.synergyhub.dto.request.RotateSsoSecretRequest;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.SsoProviderResponse;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.sso.SsoConfigurationService;
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

import java.util.List;

/**
 * REST Controller for SSO Provider Management.
 *
 * Architectural Rule: Zero Business Logic
 * - Pure DTO orchestration: Extract → Validate → Delegate → Return
 * - No security checks (delegated to SsoSecurity)
 * - No database queries (delegated to SsoConfigurationService)
 * - No validation logic (delegated to @Valid)
 *
 * Endpoints:
 * POST   /api/organizations/{orgId}/sso/providers              - Register new SSO provider
 * GET    /api/organizations/{orgId}/sso/providers              - List SSO providers
 * GET    /api/organizations/{orgId}/sso/providers/{id}         - Get SSO provider
 * PUT    /api/organizations/{orgId}/sso/providers/{id}         - Update SSO provider
 * POST   /api/organizations/{orgId}/sso/providers/{id}/rotate  - Rotate secret
 * PUT    /api/organizations/{orgId}/sso/providers/{id}/enable  - Enable provider
 * PUT    /api/organizations/{orgId}/sso/providers/{id}/disable - Disable provider
 * DELETE /api/organizations/{orgId}/sso/providers/{id}         - Delete provider
 */
@RestController
@RequestMapping("/api/organizations/{organizationId}/sso/providers")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SsoProviderController {

    private final SsoConfigurationService ssoConfigurationService;
    private final UserRepository userRepository;
    private final ClientIpResolver ipResolver;

    // ========== CREATE ==========

    /**
     * POST: Register a new SSO provider
     * Authorization: ORG_ADMIN or GLOBAL_ADMIN
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SsoProviderResponse>> registerSsoProvider(
            @PathVariable @Positive(message = "Organization ID must be positive") Integer organizationId,
            @Valid @RequestBody RegisterSsoProviderRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        
        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        
        log.info("Registering SSO provider in organization: {}", organizationId);
        SsoProviderResponse response = ssoConfigurationService.registerSsoProvider(
                organizationId, request, user, ipAddress
        );
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("SSO provider registered successfully", response));
    }

    // ========== READ ==========

    /**
     * GET: List all SSO providers for an organization
     * Authorization: ORG_ADMIN or GLOBAL_ADMIN
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SsoProviderResponse>>> listSsoProviders(
            @PathVariable @Positive(message = "Organization ID must be positive") Integer organizationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        User user = getUser(principal);
        
        log.info("Listing SSO providers for organization: {}", organizationId);
        List<SsoProviderResponse> responses = ssoConfigurationService.getSsoProviders(organizationId, user);
        
        return ResponseEntity.ok(ApiResponse.success("SSO providers retrieved successfully", responses));
    }

    /**
     * GET: Get a specific SSO provider by ID
     * Authorization: ORG_ADMIN or GLOBAL_ADMIN
     */
    @GetMapping("/{providerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SsoProviderResponse>> getSsoProvider(
            @PathVariable @Positive(message = "Organization ID must be positive") Integer organizationId,
            @PathVariable @Positive(message = "Provider ID must be positive") Integer providerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        User user = getUser(principal);
        
        log.info("Fetching SSO provider {} from organization: {}", providerId, organizationId);
        SsoProviderResponse response = ssoConfigurationService.getSsoProvider(organizationId, providerId, user);
        
        return ResponseEntity.ok(ApiResponse.success("SSO provider retrieved successfully", response));
    }

    // ========== UPDATE ==========

    /**
     * PUT: Update SSO provider configuration
     * Authorization: ORG_ADMIN or GLOBAL_ADMIN
     */
    @PutMapping("/{providerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SsoProviderResponse>> updateSsoProvider(
            @PathVariable @Positive(message = "Organization ID must be positive") Integer organizationId,
            @PathVariable @Positive(message = "Provider ID must be positive") Integer providerId,
            @Valid @RequestBody UpdateSsoProviderRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        
        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        
        log.info("Updating SSO provider {} in organization: {}", providerId, organizationId);
        SsoProviderResponse response = ssoConfigurationService.updateSsoProvider(
                organizationId, providerId, request, user, ipAddress
        );
        
        return ResponseEntity.ok(ApiResponse.success("SSO provider updated successfully", response));
    }

    // ========== SECRET ROTATION ==========

    /**
     * POST: Rotate SSO provider client secret
     * Authorization: ORG_ADMIN or GLOBAL_ADMIN
     * Security Critical: This operation is audited as a critical security event
     */
    @PostMapping("/{providerId}/rotate-secret")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SsoProviderResponse>> rotateSsoSecret(
            @PathVariable @Positive(message = "Organization ID must be positive") Integer organizationId,
            @PathVariable @Positive(message = "Provider ID must be positive") Integer providerId,
            @Valid @RequestBody RotateSsoSecretRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        
        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        
        log.warn("SECURITY: Rotating secret for SSO provider {} in organization: {}", providerId, organizationId);
        SsoProviderResponse response = ssoConfigurationService.rotateSsoSecret(
                organizationId, providerId, request, user, ipAddress
        );
        
        return ResponseEntity.ok(ApiResponse.success("SSO provider secret rotated successfully", response));
    }

    // ========== STATE MANAGEMENT ==========

    /**
     * PUT: Enable SSO provider
     * Authorization: ORG_ADMIN or GLOBAL_ADMIN
     */
    @PutMapping("/{providerId}/enable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SsoProviderResponse>> enableSsoProvider(
            @PathVariable @Positive(message = "Organization ID must be positive") Integer organizationId,
            @PathVariable @Positive(message = "Provider ID must be positive") Integer providerId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        
        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        
        log.info("Enabling SSO provider {} in organization: {}", providerId, organizationId);
        SsoProviderResponse response = ssoConfigurationService.enableSsoProvider(
                organizationId, providerId, user, ipAddress
        );
        
        return ResponseEntity.ok(ApiResponse.success("SSO provider enabled successfully", response));
    }

    /**
     * PUT: Disable SSO provider
     * Authorization: ORG_ADMIN or GLOBAL_ADMIN
     */
    @PutMapping("/{providerId}/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SsoProviderResponse>> disableSsoProvider(
            @PathVariable @Positive(message = "Organization ID must be positive") Integer organizationId,
            @PathVariable @Positive(message = "Provider ID must be positive") Integer providerId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        
        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        
        log.info("Disabling SSO provider {} in organization: {}", providerId, organizationId);
        SsoProviderResponse response = ssoConfigurationService.disableSsoProvider(
                organizationId, providerId, user, ipAddress
        );
        
        return ResponseEntity.ok(ApiResponse.success("SSO provider disabled successfully", response));
    }

    // ========== DELETE ==========

    /**
     * DELETE: Delete SSO provider
     * Authorization: ORG_ADMIN or GLOBAL_ADMIN
     */
    @DeleteMapping("/{providerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteSsoProvider(
            @PathVariable @Positive(message = "Organization ID must be positive") Integer organizationId,
            @PathVariable @Positive(message = "Provider ID must be positive") Integer providerId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        
        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        
        log.info("Deleting SSO provider {} from organization: {}", providerId, organizationId);
        ssoConfigurationService.deleteSsoProvider(organizationId, providerId, user, ipAddress);
        
        return ResponseEntity.ok(ApiResponse.success("SSO provider deleted successfully", null));
    }

    // ========== HELPER METHODS ==========

    /**
     * Extract authenticated user from principal.
     * Throws exception if user not found in database.
     * Eagerly fetches roles and permissions to avoid LazyInitializationException.
     */
    private User getUser(UserPrincipal principal) {
        return userRepository.findByIdWithRolesAndPermissions(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
