package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.TwoFactorDisableRequest;
import com.synergyhub.dto.request.TwoFactorVerifyRequest;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.TwoFactorSetupResponse;
import com.synergyhub.dto.response.TwoFactorStatusResponse;
import com.synergyhub.exception.ResourceNotFoundException;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.auth.TwoFactorAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
@Slf4j
public class TwoFactorController {

    private final TwoFactorAuthService twoFactorAuthService;
    private final UserRepository userRepository;

    /**
     * Setup two-factor authentication
     * POST /api/auth/2fa/setup
     */
    @PostMapping("/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setup2FA(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request) {

        User user = getUserFromPrincipal(currentUser);
        String ipAddress = getClientIP(request);

        TwoFactorSetupResponse response = twoFactorAuthService.setupTwoFactor(user, ipAddress);

        return ResponseEntity.ok(ApiResponse.success(
                "Two-factor authentication setup initiated. Scan the QR code and verify.",
                response
        ));
    }

    /**
     * Verify and enable two-factor authentication
     * POST /api/auth/2fa/verify
     */
    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> verify2FA(
            @Valid @RequestBody TwoFactorVerifyRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        User user = getUserFromPrincipal(currentUser);
        String ipAddress = getClientIP(httpRequest);

        boolean isValid = twoFactorAuthService.verifyAndEnableTwoFactor(user, request.getCode(), ipAddress);

        if (!isValid) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid verification code"));
        }

        return ResponseEntity.ok(ApiResponse.success(
                "Two-factor authentication enabled successfully",
                null
        ));
    }

    /**
     * Disable two-factor authentication
     * POST /api/auth/2fa/disable
     */
    @PostMapping("/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> disable2FA(
            @Valid @RequestBody TwoFactorDisableRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        User user = getUserFromPrincipal(currentUser);
        String ipAddress = getClientIP(httpRequest);

        twoFactorAuthService.disableTwoFactor(user, request.getPassword(), ipAddress);

        return ResponseEntity.ok(ApiResponse.success(
                "Two-factor authentication disabled",
                null
        ));
    }

    /**
     * Regenerate backup codes
     * POST /api/auth/2fa/regenerate-backup-codes
     */
    @PostMapping("/regenerate-backup-codes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<String>>> regenerateBackupCodes(
            @Valid @RequestBody TwoFactorVerifyRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        User user = getUserFromPrincipal(currentUser);
        String ipAddress = getClientIP(httpRequest);

        List<String> newBackupCodes = twoFactorAuthService.regenerateBackupCodes(
                user,
                request.getCode(),
                ipAddress
        );

        return ResponseEntity.ok(ApiResponse.success(
                "New backup codes generated. Please save them in a secure location.",
                newBackupCodes
        ));
    }

    /**
     * Get two-factor authentication status
     * GET /api/auth/2fa/status
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TwoFactorStatusResponse>> get2FAStatus(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        User user = getUserFromPrincipal(currentUser);

        TwoFactorStatusResponse status = TwoFactorStatusResponse.builder()
                .enabled(user.getTwoFactorEnabled())
                .build();

        return ResponseEntity.ok(ApiResponse.success(status));
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Get User entity from UserPrincipal
     */
    private User getUserFromPrincipal(UserPrincipal principal) {
        return userRepository.findByEmailWithRolesAndPermissions(principal.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
