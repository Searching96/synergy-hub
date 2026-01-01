package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.*;
import com.synergyhub.dto.response.*;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.project.ProjectService;
import com.synergyhub.service.sprint.SprintService;
import com.synergyhub.util.ClientIpResolver; // ✅ Import new resolver
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ProjectController {

    private final ProjectService projectService;
    private final SprintService sprintService;
    private final UserRepository userRepository;
    private final ClientIpResolver ipResolver; // ✅ Inject IP resolver

    // ===================================================================================
    // 1. PROJECT LIFECYCLE (Create, Update, Delete)
    // ===================================================================================

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest); // ✅ Use resolver

        ProjectResponse response = projectService.createProject(request, user, ipAddress);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project created successfully", response));
    }

    @PutMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable @Positive(message = "Project ID must be positive") Integer projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) { // ✅ Added HttpServletRequest

        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest); // ✅ Get IP address

        // ✅ Pass ipAddress to service
        ProjectResponse response = projectService.updateProject(projectId, request, user, ipAddress);

        return ResponseEntity.ok(
                ApiResponse.success("Project updated successfully", response));
    }

    @DeleteMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable @Positive(message = "Project ID must be positive") Integer projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) { // ✅ Added HttpServletRequest

        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest); // ✅ Get IP address

        // ✅ Pass ipAddress to service
        projectService.deleteProject(projectId, user, ipAddress);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success("Project deleted successfully", null));
    }

    @PutMapping("/{projectId}/archive")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> archiveProject(
            @PathVariable @Positive(message = "Project ID must be positive") Integer projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest);

        projectService.archiveProject(projectId, user, ipAddress);

        return ResponseEntity.ok(
                ApiResponse.success("Project archived successfully", null));
    }

    @PutMapping("/{projectId}/unarchive")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> unarchiveProject(
            @PathVariable @Positive(message = "Project ID must be positive") Integer projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest);

        projectService.unarchiveProject(projectId, user, ipAddress);

        return ResponseEntity.ok(
                ApiResponse.success("Project unarchived successfully", null));
    }

    // ===================================================================================
    // 2. DATA RETRIEVAL (Reads)
    // ===================================================================================

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getUserProjects(
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = getUser(principal);
        List<ProjectResponse> projects = projectService.getUserProjects(user);

        return ResponseEntity.ok(
                ApiResponse.success("Projects retrieved successfully", projects));
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProjectDetailResponse>> getProjectDetails(
            @PathVariable @Positive(message = "Project ID must be positive") Integer projectId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = getUser(principal);
        ProjectDetailResponse project = projectService.getProjectDetails(projectId, user);

        return ResponseEntity.ok(
                ApiResponse.success("Project details retrieved successfully", project));
    }

    // ===================================================================================
    // 3. MEMBERSHIP MANAGEMENT
    // ===================================================================================

    @PostMapping("/{projectId}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> addMember(
            @PathVariable @Positive(message = "Project ID must be positive") Integer projectId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest); // ✅ Use resolver

        projectService.addMember(projectId, request, user, ipAddress);

        return ResponseEntity.ok(
                ApiResponse.success("Member added successfully", null));
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable @Positive(message = "Project ID must be positive") Integer projectId,
            @PathVariable @Positive(message = "User ID must be positive") Integer userId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest); // ✅ Use resolver

        projectService.removeMember(projectId, userId, user, ipAddress);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success("Member removed successfully", null));
    }

    @PutMapping("/{projectId}/members/{userId}/role")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> updateMemberRole(
            @PathVariable @Positive(message = "Project ID must be positive") Integer projectId,
            @PathVariable @Positive(message = "User ID must be positive") Integer userId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        User user = getUser(principal);
        String ipAddress = ipResolver.resolveClientIp(httpRequest); // ✅ Use resolver

        projectService.updateMemberRole(projectId, userId, request, user, ipAddress);

        return ResponseEntity.ok(
                ApiResponse.success("Member role updated successfully", null));
    }

    @GetMapping("/{projectId}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ProjectMemberResponse>>> getProjectMembers(
            @PathVariable @Positive(message = "Project ID must be positive") Integer projectId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = getUser(principal);
        List<ProjectMemberResponse> members = projectService.getProjectMembers(projectId, user);

        return ResponseEntity.ok(
                ApiResponse.success("Project members retrieved successfully", members));
    }

    @GetMapping("/{projectId}/sprints")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SprintResponse>>> getProjectSprints(
            @PathVariable @Positive(message = "Project ID must be positive") Integer projectId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Getting sprints for project: {}", projectId);

        var user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        List<SprintResponse> sprints = sprintService.getSprintsByProject(projectId, user);

        return ResponseEntity.ok(
                ApiResponse.success("Sprints retrieved successfully", sprints));
    }

    // ===================================================================================
    // Helper Methods
    // ===================================================================================

    /**
     * Extracts the Domain User entity from the Security Principal.
     * Consolidates the lookup logic that was previously repeated in every method.
     */
    private User getUser(UserPrincipal principal) {
        return userRepository.findByEmailWithRolesAndPermissions(principal.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User not found"));
    }
}