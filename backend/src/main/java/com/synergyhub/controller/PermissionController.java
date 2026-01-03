package com.synergyhub.controller;

import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.PermissionResponse;
import com.synergyhub.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PermissionController {

    private final PermissionRepository permissionRepository;

    /**
     * READ: GET /api/permissions
     * Lists all available permissions (read-only).
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        
        log.info("Fetching all permissions");
        List<PermissionResponse> responses = permissionRepository.findAll().stream()
                .map(p -> new PermissionResponse(
                        p.getId(),
                        p.getName(),
                        p.getDescription()
                ))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Permissions retrieved successfully", responses));
    }
}
