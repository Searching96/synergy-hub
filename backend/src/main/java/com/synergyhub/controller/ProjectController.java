package com.synergyhub.controller;

import com.synergyhub.dto.request.AddMemberRequest;
import com.synergyhub.dto.request.CreateProjectRequest;
import com.synergyhub.dto.request.UpdateMemberRoleRequest;
import com.synergyhub.dto.request.UpdateProjectRequest;
import com.synergyhub.dto.response.*;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.project.ProjectService;
import com.synergyhub.service.sprint.SprintService;
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
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;
    private final SprintService sprintService;
    private final UserRepository userRepository;

    /**
     * Create a new project
     * POST /api/projects
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        log.info("Creating project for user: {}", currentUser.getId());

        var user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        String ipAddress = getClientIP(httpRequest);
        ProjectResponse project = projectService.createProject(request, user, ipAddress);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project created successfully", project));
    }

    /**
     * Get all projects for current user
     * GET /api/projects
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getUserProjects(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Getting projects for user: {}", currentUser.getId());

        var user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        List<ProjectResponse> projects = projectService.getUserProjects(user);

        return ResponseEntity.ok(
                ApiResponse.success("Projects retrieved successfully", projects)
        );
    }

    /**
     * Get project details by ID
     * GET /api/projects/{projectId}
     */
    @GetMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProjectDetailResponse>> getProjectDetails(
            @PathVariable Integer projectId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Getting project details: {} for user: {}", projectId, currentUser.getId());

        var user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        ProjectDetailResponse project = projectService.getProjectDetails(projectId, user);

        return ResponseEntity.ok(
                ApiResponse.success("Project details retrieved successfully", project)
        );
    }

    /**
     * Update project
     * PUT /api/projects/{projectId}
     */
    @PutMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable Integer projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        log.info("Updating project: {} by user: {}", projectId, currentUser.getId());

        var user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        String ipAddress = getClientIP(httpRequest);
        ProjectResponse project = projectService.updateProject(projectId, request, user, ipAddress);

        return ResponseEntity.ok(
                ApiResponse.success("Project updated successfully", project)
        );
    }

    /**
     * Delete (archive) project
     * DELETE /api/projects/{projectId}
     */
    @DeleteMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable Integer projectId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        log.info("Deleting project: {} by user: {}", projectId, currentUser.getId());

        var user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        String ipAddress = getClientIP(httpRequest);
        projectService.deleteProject(projectId, user, ipAddress);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success("Project deleted successfully", null));
    }

    /**
     * Add member to project
     * POST /api/projects/{projectId}/members
     */
    @PostMapping("/{projectId}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> addMember(
            @PathVariable Integer projectId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        log.info("Adding member to project: {} by user: {}", projectId, currentUser.getId());

        var user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        String ipAddress = getClientIP(httpRequest);
        projectService.addMemberToProject(projectId, request, user, ipAddress);

        return ResponseEntity.ok(
                ApiResponse.success("Member added successfully", null)
        );
    }

    /**
     * Remove member from project
     * DELETE /api/projects/{projectId}/members/{userId}
     */
    @DeleteMapping("/{projectId}/members/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Integer projectId,
            @PathVariable Integer userId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        log.info("Removing member {} from project: {}", userId, projectId);

        var user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        String ipAddress = getClientIP(httpRequest);
        projectService.removeMemberFromProject(projectId, userId, user, ipAddress);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success("Member removed successfully", null));
    }

    /**
     * Update member role in project
     * PUT /api/projects/{projectId}/members/{userId}/role
     */
    @PutMapping("/{projectId}/members/{userId}/role")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> updateMemberRole(
            @PathVariable Integer projectId,
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        log.info("Updating role for member {} in project: {}", userId, projectId);

        var user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        String ipAddress = getClientIP(httpRequest);
        projectService.updateMemberRole(projectId, userId, request, user, ipAddress);

        return ResponseEntity.ok(
                ApiResponse.success("Member role updated successfully", null)
        );
    }

    /**
     * Get all members of a project
     * GET /api/projects/{projectId}/members
     */
    @GetMapping("/{projectId}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ProjectMemberResponse>>> getProjectMembers(
            @PathVariable Integer projectId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Getting members for project: {}", projectId);

        var user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        List<ProjectMemberResponse> members = projectService.getProjectMembers(projectId, user);

        return ResponseEntity.ok(
                ApiResponse.success("Project members retrieved successfully", members)
        );
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Extract client IP address from request
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    @GetMapping("/{projectId}/sprints")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SprintResponse>>> getProjectSprints(
            @PathVariable Integer projectId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Getting sprints for project: {}", projectId);

        var user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        List<SprintResponse> sprints = sprintService.getSprintsByProject(projectId, user);

        return ResponseEntity.ok(
                ApiResponse.success("Sprints retrieved successfully", sprints)
        );
    }
}
