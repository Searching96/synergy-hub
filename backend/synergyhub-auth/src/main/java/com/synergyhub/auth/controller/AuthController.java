package com.synergyhub.auth.controller;

import com.synergyhub.auth.dto.*;
import com.synergyhub.auth.security.JwtTokenProvider;
import com.synergyhub.auth.service.AuthenticationService;
import com.synergyhub.auth.service.EmailVerificationService;
import com.synergyhub.auth.service.PasswordResetService;
import com.synergyhub.auth.service.TwoFactorAuthService;
import com.synergyhub.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final EmailVerificationService emailVerificationService;
    private final JwtTokenProvider tokenProvider;

    /**
     * UC-IA-01: Login
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = authenticationService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * UC-IA-02: Register
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Registration successful. Please check your email to verify your account.", response));
    }

    /**
     * Logout current session
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        String tokenId = tokenProvider.getTokenId(token);
        authenticationService.logout(tokenId);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    /**
     * Logout from all devices
     * POST /api/auth/logout-all
     */
    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logoutAllDevices() {
        authenticationService.logoutAllDevices();
        return ResponseEntity.ok(ApiResponse.success("Logged out from all devices", null));
    }

    /**
     * UC-IA-03: Request password reset
     * POST /api/auth/password-reset/request
     */
    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(
            "If an account exists with this email, a password reset link has been sent.", null));
    }

    /**
     * UC-IA-03: Confirm password reset
     * POST /api/auth/password-reset/confirm
     */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirm request) {
        passwordResetService.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully", null));
    }

    /**
     * UC-IA-04: Setup 2FA
     * POST /api/auth/2fa/setup
     */
    @PostMapping("/2fa/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setup2FA() {
        TwoFactorSetupResponse response = twoFactorAuthService.setup2FA();
        return ResponseEntity.ok(ApiResponse.success("2FA setup initiated. Scan QR code with authenticator app.", response));
    }

    /**
     * UC-IA-04: Verify and enable 2FA
     * POST /api/auth/2fa/verify
     */
    @PostMapping("/2fa/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> verify2FA(@Valid @RequestBody TwoFactorVerifyRequest request) {
        twoFactorAuthService.verify2FASetup(request);
        return ResponseEntity.ok(ApiResponse.success("2FA enabled successfully", null));
    }

    /**
     * Disable 2FA
     * POST /api/auth/2fa/disable
     */
    @PostMapping("/2fa/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> disable2FA(@Valid @RequestBody TwoFactorVerifyRequest request) {
        twoFactorAuthService.disable2FA(request);
        return ResponseEntity.ok(ApiResponse.success("2FA disabled successfully", null));
    }

    /**
     * Get backup codes
     * GET /api/auth/2fa/backup-codes
     */
    @GetMapping("/2fa/backup-codes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<String>>> getBackupCodes() {
        List<String> codes = twoFactorAuthService.getBackupCodes();
        return ResponseEntity.ok(ApiResponse.success("Backup codes retrieved", codes));
    }

    /**
     * Regenerate backup codes
     * POST /api/auth/2fa/backup-codes/regenerate
     */
    @PostMapping("/2fa/backup-codes/regenerate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<String>>> regenerateBackupCodes(@Valid @RequestBody TwoFactorVerifyRequest request) {
        List<String> codes = twoFactorAuthService.regenerateBackupCodes(request);
        return ResponseEntity.ok(ApiResponse.success("Backup codes regenerated", codes));
    }

    /**
     * Verify email
     * GET /api/auth/verify-email
     */
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    /**
     * Resend verification email
     * POST /api/auth/verify-email/resend
     */
    @PostMapping("/verify-email/resend")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(@RequestParam String email) {
        emailVerificationService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success("Verification email sent", null));
    }

    /**
     * Get current user info
     * GET /api/auth/me
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDetails>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("User info retrieved", userDetails));
    }
}