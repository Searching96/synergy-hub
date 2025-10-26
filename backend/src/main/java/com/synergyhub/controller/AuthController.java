package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.*;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.LoginResponse;
import com.synergyhub.dto.response.UserResponse;
import com.synergyhub.exception.ResourceNotFoundException;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.auth.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;
    private final PasswordResetService passwordResetService;
    private final ChangePasswordService changePasswordService;
    private final UserRepository userRepository;

    /**
     * User login
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResponse response = authenticationService.login(request, ipAddress, userAgent);

        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * User registration
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIP(httpRequest);
        UserResponse response = registrationService.register(request, ipAddress);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Registration successful. Please check your email to verify your account.",
                        response
                ));
    }

    /**
     * Verify email with token
     * POST /api/auth/verify-email
     */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam String token,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIP(httpRequest);
        registrationService.verifyEmail(token, ipAddress);

        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    /**
     * Resend verification email
     * POST /api/auth/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(
            @RequestParam String email,
            HttpServletRequest httpRequest) {  // ✅ Added HttpServletRequest

        String ipAddress = getClientIP(httpRequest);  // ✅ Extract IP address
        registrationService.resendVerificationEmail(email, ipAddress);  // ✅ Pass IP address

        return ResponseEntity.ok(ApiResponse.success("Verification email sent", null));
    }

    /**
     * Request password reset
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIP(httpRequest);
        passwordResetService.requestPasswordReset(request, ipAddress);

        // Generic message for security (don't reveal if email exists)
        return ResponseEntity.ok(ApiResponse.success(
                "If the email exists, a password reset link has been sent",
                null
        ));
    }

    /**
     * Validate password reset token
     * GET /api/auth/validate-reset-token
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<ApiResponse<Boolean>> validateResetToken(
            @RequestParam String token) {

        boolean isValid = passwordResetService.validateResetToken(token);

        return ResponseEntity.ok(ApiResponse.success(isValid));
    }

    /**
     * Reset password with token
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetConfirmRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIP(httpRequest);
        passwordResetService.resetPassword(request, ipAddress);

        return ResponseEntity.ok(ApiResponse.success(
                "Password has been reset successfully",
                null
        ));
    }

    /**
     * Change password for authenticated user
     * POST /api/auth/change-password
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")  // ✅ Added security annotation
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIP(httpRequest);

        // Fetch the User entity
        User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())  // ✅ Changed to use email
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUser.getEmail()));

        changePasswordService.changePassword(user, request, ipAddress);

        return ResponseEntity.ok(ApiResponse.success(
                "Password changed successfully. Please login again with your new password.",
                null
        ));
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Extract client IP address from request
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {  // ✅ Added empty check
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();  // ✅ Added trim()
    }
}
