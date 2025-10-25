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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/setup")
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setup2FA(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        User user = getUserFromPrincipal(currentUser);
        TwoFactorSetupResponse response = twoFactorAuthService.setupTwoFactor(user);

        return ResponseEntity.ok(ApiResponse.success(
                "Two-factor authentication setup initiated. Scan the QR code and verify.",
                response
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify2FA(
            @Valid @RequestBody TwoFactorVerifyRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        User user = getUserFromPrincipal(currentUser);
        boolean isValid = twoFactorAuthService.verifyAndEnableTwoFactor(user, request.getCode());

        if (!isValid) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid verification code"));
        }

        return ResponseEntity.ok(ApiResponse.success(
                "Two-factor authentication enabled successfully",
                null
        ));
    }

    @PostMapping("/disable")
    public ResponseEntity<ApiResponse<Void>> disable2FA(
            @Valid @RequestBody TwoFactorDisableRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        User user = getUserFromPrincipal(currentUser);
        twoFactorAuthService.disableTwoFactor(user, request.getPassword());

        return ResponseEntity.ok(ApiResponse.success(
                "Two-factor authentication disabled",
                null
        ));
    }

    @PostMapping("/regenerate-backup-codes")
    public ResponseEntity<ApiResponse<List<String>>> regenerateBackupCodes(
            @Valid @RequestBody TwoFactorVerifyRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        User user = getUserFromPrincipal(currentUser);
        List<String> newBackupCodes = twoFactorAuthService.regenerateBackupCodes(user, request.getCode());

        return ResponseEntity.ok(ApiResponse.success(
                "New backup codes generated. Please save them in a secure location.",
                newBackupCodes
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<TwoFactorStatusResponse>> get2FAStatus(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        User user = getUserFromPrincipal(currentUser);

        TwoFactorStatusResponse status = TwoFactorStatusResponse.builder()
                .enabled(user.getTwoFactorEnabled())
                .build();

        return ResponseEntity.ok(ApiResponse.success(status));
    }

    private User getUserFromPrincipal(UserPrincipal principal) {
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
    }
}