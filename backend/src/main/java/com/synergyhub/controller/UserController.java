package com.synergyhub.controller;

import com.synergyhub.dto.mapper.UserMapper;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.UserResponse;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.security.SessionManagementService;
import com.synergyhub.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
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

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        var user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        UserResponse response = userMapper.toUserResponse(user);

        return ResponseEntity.ok(ApiResponse.success(response));
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