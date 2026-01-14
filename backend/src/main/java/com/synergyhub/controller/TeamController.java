package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.CreateTeamRequest;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.TeamResponse;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.team.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{organizationId}/teams")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TeamController {

    private final TeamService teamService;
    private final UserRepository userRepository;
    
    // ✅ NEW Injection
    // Using @PreAuthorize SPEL requires the bean to be available in context
    // The bean name is 'organizationSecurity'

    @PostMapping
    @PreAuthorize("@organizationSecurity.hasOrganizationAccess(#organizationId, #principal)")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateTeamRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userRepository.findByIdWithRolesAndPermissions(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        TeamResponse team = teamService.createTeam(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(team));
    }

    @GetMapping
    @PreAuthorize("@organizationSecurity.hasOrganizationAccess(#organizationId, #principal)")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getOrganizationTeams(
            @PathVariable Long organizationId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userRepository.findByIdWithRolesAndPermissions(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<TeamResponse> teams = teamService.getOrganizationTeams(organizationId, user);
        return ResponseEntity.ok(ApiResponse.success(teams));
    }

    @GetMapping("/{teamId}")
    @PreAuthorize("@organizationSecurity.hasOrganizationAccess(#organizationId, #principal)")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeam(
            @PathVariable Long organizationId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userRepository.findByIdWithRolesAndPermissions(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        TeamResponse team = teamService.getTeam(teamId, user);
        return ResponseEntity.ok(ApiResponse.success(team));
    }

    @DeleteMapping("/{teamId}")
    @PreAuthorize("@organizationSecurity.hasOrganizationAccess(#organizationId, #principal)")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(
            @PathVariable Long organizationId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userRepository.findByIdWithRolesAndPermissions(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        teamService.deleteTeam(teamId, user);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{teamId}/members/{userId}")
    @PreAuthorize("@organizationSecurity.hasOrganizationAccess(#organizationId, #principal)")
    public ResponseEntity<ApiResponse<TeamResponse>> addMember(
            @PathVariable Long organizationId,
            @PathVariable Long teamId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userRepository.findByIdWithRolesAndPermissions(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        TeamResponse team = teamService.addMember(teamId, userId, user);
        return ResponseEntity.ok(ApiResponse.success("Member added", team));
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    @PreAuthorize("@organizationSecurity.hasOrganizationAccess(#organizationId, #principal)")
    public ResponseEntity<ApiResponse<TeamResponse>> removeMember(
            @PathVariable Long organizationId,
            @PathVariable Long teamId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userRepository.findByIdWithRolesAndPermissions(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        TeamResponse team = teamService.removeMember(teamId, userId, user);
        return ResponseEntity.ok(ApiResponse.success("Member removed", team));
    }
}
