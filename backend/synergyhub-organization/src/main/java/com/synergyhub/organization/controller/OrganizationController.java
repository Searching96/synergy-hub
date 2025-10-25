package com.synergyhub.organization.controller;

import com.synergyhub.common.dto.ApiResponse;
import com.synergyhub.organization.dto.OrganizationCreateRequest;
import com.synergyhub.organization.dto.OrganizationResponse;
import com.synergyhub.organization.dto.OrganizationUpdateRequest;
import com.synergyhub.organization.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(
            @Valid @RequestBody OrganizationCreateRequest request) {
        OrganizationResponse response = organizationService.createOrganization(request);
        // Fix: correct parameter order - (data, message)
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Organization created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganization(@PathVariable Integer id) {
        OrganizationResponse response = organizationService.getOrganizationById(id);
        // Fix: just pass data, or (data, message)
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganizationBySlug(@PathVariable String slug) {
        OrganizationResponse response = organizationService.getOrganizationBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> getAllOrganizations() {
        List<OrganizationResponse> response = organizationService.getAllOrganizations();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateOrganization(
            @PathVariable Integer id,
            @Valid @RequestBody OrganizationUpdateRequest request) {
        OrganizationResponse response = organizationService.updateOrganization(id, request);
        return ResponseEntity.ok(ApiResponse.success("Organization updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(@PathVariable Integer id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.ok(ApiResponse.success("Organization deleted successfully", null));
    }
}