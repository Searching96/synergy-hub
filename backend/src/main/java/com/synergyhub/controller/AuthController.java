package com.synergyhub.controller;

import com.synergyhub.dto.request.*;
import com.synergyhub.dto.response.*;
import com.synergyhub.security.JwtTokenProvider;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.auth.*;
import com.synergyhub.util.ClientIpResolver;
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
    
    private final JwtTokenProvider jwtTokenProvider;
    private final ClientIpResolver ipResolver;

    // ===================================================================================
    // PUBLIC AUTHENTICATION ENDPOINTS
    // ===================================================================================

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResponse response = loginService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        UserResponse response = registrationService.register(request, ipAddress);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Registration successful. Please check your email to verify your account.",
                        response
                ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        try {
            // Validate the refresh token and generate new access token
            LoginResponse response = loginService.refreshToken(request.getRefreshToken(), ipAddress, userAgent);
            return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
        } catch (Exception ex) {
            log.error("Token refresh failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid or expired refresh token"));
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        registrationService.verifyEmail(request.getToken(), ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(
            @Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        registrationService.resendVerificationEmail(request.getEmail(), ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Verification email sent", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        passwordResetService.requestPasswordReset(request, ipAddress);

        return ResponseEntity.ok(ApiResponse.success(
                "If the email exists, a password reset link has been sent",
                null
        ));
    }

    @PostMapping("/validate-reset-token")
    public ResponseEntity<ApiResponse<Boolean>> validateResetToken(
            @Valid @RequestBody ValidateResetTokenRequest request) {
        
        boolean isValid = passwordResetService.validateResetToken(request.getToken());
        return ResponseEntity.ok(ApiResponse.success(isValid));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetConfirmRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        passwordResetService.resetPassword(request, ipAddress);

        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully", null));
    }

    // ===================================================================================
    // PROTECTED ENDPOINTS (REQUIRE AUTH)
    // ===================================================================================

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        changePasswordService.changePassword(currentUser.getEmail(), request, ipAddress);

        return ResponseEntity.ok(ApiResponse.success(
                "Password changed successfully. Please login again with your new password.",
                null
        ));
    }

    // --- Two-Factor Authentication (2FA) ---

    @PostMapping("/2fa/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> setupTwoFactor(
            @AuthenticationPrincipal UserPrincipal currentUser, 
            HttpServletRequest httpRequest) {
        
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        var response = twoFactorAuthService.setupTwoFactor(currentUser.getEmail(), ipAddress);
        return ResponseEntity.ok(ApiResponse.success("2FA setup initiated", response));
    }

    @PostMapping("/2fa/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> verifyAndEnableTwoFactor(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody TwoFactorVerifyRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        boolean enabled = twoFactorAuthService.verifyAndEnableTwoFactor(
            currentUser.getEmail(), request.getCode(), ipAddress);
        
        if (enabled) {
            return ResponseEntity.ok(ApiResponse.success("2FA enabled successfully", null));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Invalid 2FA code"));
        }
    }

    @PostMapping("/2fa/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> disableTwoFactor(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody TwoFactorDisableRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        twoFactorAuthService.disableTwoFactor(
            currentUser.getEmail(), request.getPassword(), ipAddress);
        return ResponseEntity.ok(ApiResponse.success("2FA disabled successfully", null));
    }

    @PostMapping("/2fa/backup-codes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> regenerateBackupCodes(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody RegenerateBackupCodesRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        // Regenerates codes and saves HASHED versions to DB. Returns PLAINTEXT codes once.
        var codes = twoFactorAuthService.regenerateBackupCodes(
            currentUser.getEmail(), 
            request.getVerificationCode(), 
            ipAddress
        );
        return ResponseEntity.ok(ApiResponse.success("Backup codes regenerated", codes));
    }

    // --- Session Management ---

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UserSessionResponse>>> listActiveSessions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request) {
        
        String currentToken = jwtTokenProvider.getJwtFromRequest(request);
        String currentTokenId = currentToken != null ? 
            jwtTokenProvider.getTokenIdFromToken(currentToken) : null;
        
        List<UserSessionResponse> sessions = sessionService.listActiveSessions(
            currentUser.getId(), currentTokenId);
        
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @PostMapping("/sessions/revoke")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @Valid @RequestBody RevokeSessionRequest request, 
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        sessionService.revokeSession(request.getTokenId(), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Session revoked", null));
    }

    @PostMapping("/sessions/revoke-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> revokeAllSessions(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        sessionService.revokeAllSessions(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("All sessions revoked", null));
    }
}