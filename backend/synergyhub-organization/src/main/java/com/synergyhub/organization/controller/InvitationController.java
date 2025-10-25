package com.synergyhub.organization.controller;

import com.synergyhub.common.dto.ApiResponse;
import com.synergyhub.organization.dto.InvitationRequest;
import com.synergyhub.organization.entity.OrganizationInvitation;
import com.synergyhub.organization.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{organizationId}/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationInvitation>> createInvitation(
            @PathVariable Integer organizationId,
            @Valid @RequestBody InvitationRequest request,
            @RequestHeader("X-User-Id") Integer invitedBy) {
        OrganizationInvitation invitation = invitationService.createInvitation(organizationId, request, invitedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(invitation, "Invitation sent successfully"));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<OrganizationInvitation>>> getPendingInvitations(
            @PathVariable Integer organizationId) {
        List<OrganizationInvitation> invitations = invitationService.getPendingInvitations(organizationId);
        return ResponseEntity.ok(ApiResponse.success(invitations));
    }

    @PostMapping("/accept/{token}")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(@PathVariable String token) {
        invitationService.acceptInvitation(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation accepted successfully"));
    }

    @DeleteMapping("/{invitationId}")
    public ResponseEntity<ApiResponse<Void>> revokeInvitation(
            @PathVariable Integer organizationId,
            @PathVariable Integer invitationId) {
        invitationService.revokeInvitation(invitationId);
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation revoked successfully"));
    }
}