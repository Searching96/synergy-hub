package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.mapper.UserMapper;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.UserOrganizationResponse;
import com.synergyhub.dto.response.UserResponse;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.OrganizationContext;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.security.SessionManagementService;
import com.synergyhub.service.user.UserService; // ✅ Added
import com.synergyhub.service.auth.ChangePasswordService; // ✅ Added
import com.synergyhub.dto.request.UpdateProfileRequest; // ✅ Added
import com.synergyhub.dto.request.ChangePasswordRequest; // ✅ Added
import com.synergyhub.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid; // ✅ Added
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SessionManagementService sessionManagementService;
    private final ClientIpResolver ipResolver;
    private final UserService userService; // ✅ Added
    private final ChangePasswordService changePasswordService; // ✅ Added

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Long orgId = OrganizationContext.getcurrentOrgIdOrNull();
        User user;
        
        if (orgId != null) {
            // Use organization-scoped query when organization context is set
            user = userRepository.findByEmailWithRolesAndPermissionsInOrganization(currentUser.getEmail(), orgId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "User not found in current organization"
                    ));
        } else {
            // Fall back to global query when no organization context (new user, login flow)
            user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "User not found"
                    ));
        }

        UserResponse response = userMapper.toUserResponse(user);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Check if current user has an organization
     * GET /api/users/me/organization
     */
    @GetMapping("/me/organization")
    public ResponseEntity<ApiResponse<UserOrganizationResponse>> getCurrentUserOrganization(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        Long orgId = OrganizationContext.getcurrentOrgIdOrNull();
        User user;
        
        if (orgId != null) {
            // Use organization-scoped query when organization context is set
            user = userRepository.findByIdWithOrganizationInOrganization(principal.getId(), orgId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "User not found in current organization"
                    ));
        } else {
            // Fall back to global query when no organization context (new user)
            user = userRepository.findByIdWithOrganization(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "User not found"
                    ));
        }
        
        log.info("Checking organization for user: {}", user.getId());
        
        UserOrganizationResponse response = UserOrganizationResponse.builder()
                .userId(user.getId())
                .userName(user.getName())
                .userEmail(user.getEmail())
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                .organizationName(user.getOrganization() != null ? user.getOrganization().getName() : null)
                .hasOrganization(user.getOrganization() != null)
                .build();
        
        return ResponseEntity.ok(
            ApiResponse.success("User organization status retrieved", response)
        );
    }

    /**
     * Get all organizations the current user belongs to
     * GET /api/users/me/organizations
     */
    @GetMapping("/me/organizations")
    public ResponseEntity<ApiResponse<java.util.List<UserOrganizationResponse>>> getMyOrganizations(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        java.util.List<UserOrganizationResponse> orgs = user.getMemberships().stream()
                .map(membership -> UserOrganizationResponse.builder()
                        .userId(user.getId())
                        .userName(user.getName())
                        .userEmail(user.getEmail())
                        .organizationId(membership.getOrganization().getId())
                        .organizationName(membership.getOrganization().getName())
                        .roles(membership.getRole() != null ? java.util.Set.of(membership.getRole().getName()) : java.util.Collections.emptySet())
                        .hasOrganization(true)
                        .build())
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(orgs));
    }

    /**
     * Update user profile
     * PUT /api/users/me
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        UserResponse response = userService.updateProfile(user, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    /**
     * Change password
     * PUT /api/users/me/password
     */
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        // ChangePasswordService uses email to find user
        changePasswordService.changePassword(principal.getEmail(), request, ipAddress);

        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        String token = extractToken(httpRequest);
        String ipAddress = ipResolver.resolveClientIp(httpRequest);

        sessionManagementService.logout(currentUser, token, ipAddress);

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAllDevices(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        String ipAddress = ipResolver.resolveClientIp(httpRequest);

        sessionManagementService.logoutAllDevices(currentUser, ipAddress);

        return ResponseEntity.ok(ApiResponse.success(
                "Logged out from all devices successfully",
                null
        ));
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}