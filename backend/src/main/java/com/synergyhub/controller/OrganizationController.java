package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.CreateOrganizationRequest;
import com.synergyhub.dto.request.JoinOrganizationRequest;
import com.synergyhub.dto.request.OrganizationEmailRequest;
import com.synergyhub.dto.request.UpdateOrganizationRequest;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.OrganizationResponse;
import com.synergyhub.dto.response.UserOrganizationResponse;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.organization.OrganizationService;
import com.synergyhub.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Slf4j
public class OrganizationController {

    private final OrganizationService organizationService;

    private final ClientIpResolver ipResolver;

    // Helper removed - fetching from principal directly


    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        OrganizationResponse response = organizationService.createOrganization(request, currentUser.getUser(), ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Organization created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganization(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        OrganizationResponse response = organizationService.getOrganization(id, currentUser.getUser());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateOrganization(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrganizationRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        OrganizationResponse response = organizationService.updateOrganization(id, request, currentUser.getUser(), ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Organization updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        organizationService.deleteOrganization(id, currentUser.getUser(), ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Organization deleted successfully", null));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<OrganizationResponse>> joinOrganization(
            @Valid @RequestBody JoinOrganizationRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        OrganizationResponse response = organizationService.joinOrganization(request.getInviteCode(), currentUser.getUser(), ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Joined organization successfully", response));
    }

    @PostMapping("/request-join")
    public ResponseEntity<ApiResponse<Void>> requestJoinOrganization(
            @Valid @RequestBody OrganizationEmailRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {
        
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        organizationService.requestJoinOrganization(request.getOrganizationEmail(), currentUser.getUser(), ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Join request sent successfully", null));
    }

    @PostMapping("/{id}/invite-code")
    public ResponseEntity<ApiResponse<String>> generateInviteCode(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        String inviteCode = organizationService.generateInviteCode(id, currentUser.getUser());
        return ResponseEntity.ok(ApiResponse.success("Invite code generated successfully", inviteCode));
    }
}
