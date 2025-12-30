package com.synergyhub.controller;

import com.synergyhub.domain.entity.UserSession;
import com.synergyhub.dto.request.*;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.LoginResponse;
import com.synergyhub.dto.response.UserResponse;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.auth.*;
import com.synergyhub.util.WebUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final LoginService loginService;
    private final RegistrationService registrationService;
    private final PasswordResetService passwordResetService;
    private final ChangePasswordService changePasswordService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final SessionService sessionService;
    // REMOVED: private final UserRepository userRepository; // ✅ Clean architecture

    /**
     * User login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = WebUtils.getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResponse response = loginService.login(request, ipAddress, userAgent);

        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * User registration
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = WebUtils.getClientIP(httpRequest);
        UserResponse response = registrationService.register(request, ipAddress);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Registration successful. Please check your email to verify your account.",
                        response
                ));
    }

    /**
     * Verify email with token
     */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam String token,
            HttpServletRequest httpRequest) {

        String ipAddress = WebUtils.getClientIP(httpRequest);
        registrationService.verifyEmail(token, ipAddress);

        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    /**
     * Resend verification email
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(
            @RequestParam String email,
            HttpServletRequest httpRequest) {

        String ipAddress = WebUtils.getClientIP(httpRequest);
        registrationService.resendVerificationEmail(email, ipAddress);

        return ResponseEntity.ok(ApiResponse.success("Verification email sent", null));
    }

    /**
     * Request password reset
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = WebUtils.getClientIP(httpRequest);
        passwordResetService.requestPasswordReset(request, ipAddress);

        return ResponseEntity.ok(ApiResponse.success(
                "If the email exists, a password reset link has been sent",
                null
        ));
    }

    /**
     * Validate password reset token
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<ApiResponse<Boolean>> validateResetToken(
            @RequestParam String token) {

        boolean isValid = passwordResetService.validateResetToken(token);
        return ResponseEntity.ok(ApiResponse.success(isValid));
    }

    /**
     * Reset password with token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetConfirmRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = WebUtils.getClientIP(httpRequest);
        passwordResetService.resetPassword(request, ipAddress);

        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully", null));
    }

    /**
     * Change password for authenticated user
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        String ipAddress = WebUtils.getClientIP(httpRequest);
        changePasswordService.changePassword(currentUser.getEmail(), request, ipAddress);

        return ResponseEntity.ok(ApiResponse.success(
                "Password changed successfully. Please login again with your new password.",
                null
        ));
    }

    /**
     * Enable two-factor authentication (2FA)
     */
    @PostMapping("/2fa/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> setupTwoFactor(
            @AuthenticationPrincipal UserPrincipal currentUser, 
            HttpServletRequest httpRequest) {
        
        String ipAddress = WebUtils.getClientIP(httpRequest);
        var response = twoFactorAuthService.setupTwoFactor(currentUser.getEmail(), ipAddress);
        return ResponseEntity.ok(ApiResponse.success("2FA setup initiated", response));
    }

    /**
     * Verify and enable 2FA
     */
    @PostMapping("/2fa/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> verifyAndEnableTwoFactor(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam String code,
            HttpServletRequest httpRequest) {
        
        String ipAddress = WebUtils.getClientIP(httpRequest);
        boolean enabled = twoFactorAuthService.verifyAndEnableTwoFactor(currentUser.getEmail(), code, ipAddress);
        
        if (enabled) {
            return ResponseEntity.ok(ApiResponse.success("2FA enabled successfully", null));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Invalid 2FA code"));
        }
    }

    /**
     * Disable 2FA
     */
    @PostMapping("/2fa/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> disableTwoFactor(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam String password,
            HttpServletRequest httpRequest) {
        
        String ipAddress = WebUtils.getClientIP(httpRequest);
        twoFactorAuthService.disableTwoFactor(currentUser.getEmail(), password, ipAddress);
        return ResponseEntity.ok(ApiResponse.success("2FA disabled successfully", null));
    }

    /**
     * Regenerate backup codes for 2FA
     */
    @PostMapping("/2fa/backup-codes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> regenerateBackupCodes(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam String verificationCode,
            HttpServletRequest httpRequest) {
        
        String ipAddress = WebUtils.getClientIP(httpRequest);
        // Uses the newly refactored service method that persists to DB
        var codes = twoFactorAuthService.regenerateBackupCodes(currentUser.getEmail(), verificationCode, ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Backup codes regenerated", codes));
    }

    /**
     * List all active sessions for the authenticated user
     * GET /api/auth/sessions
     */
    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UserSession>>> listActiveSessions(@AuthenticationPrincipal UserPrincipal currentUser) {
        // ✅ Refactored: Pass ID directly to service
        List<UserSession> sessions = sessionService.listActiveSessions(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * Revoke a specific session
     * POST /api/auth/sessions/revoke
     */
    @PostMapping("/sessions/revoke")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @RequestParam String tokenId, 
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        // ✅ Refactored: Pass User ID to ensure ownership check happens in Service
        sessionService.revokeSession(tokenId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Session revoked", null));
    }

    /**
     * Revoke all sessions for the authenticated user
     * POST /api/auth/sessions/revoke-all
     */
    @PostMapping("/sessions/revoke-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> revokeAllSessions(@AuthenticationPrincipal UserPrincipal currentUser) {
        // ✅ Refactored: Pass ID directly to service
        sessionService.revokeAllSessions(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("All sessions revoked", null));
    }
}