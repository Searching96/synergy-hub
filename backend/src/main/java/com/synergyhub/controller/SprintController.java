package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.CreateSprintRequest;
import com.synergyhub.dto.request.UpdateSprintRequest;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.SprintDetailResponse;
import com.synergyhub.dto.response.SprintResponse;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.UserPrincipal;
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
@RequestMapping("/api/sprints")
@RequiredArgsConstructor
@Slf4j
public class SprintController {

    private final SprintService sprintService;
    private final UserRepository userRepository;

    /**
     * Create a new sprint
     * POST /api/sprints
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SprintResponse>> createSprint(
            @Valid @RequestBody CreateSprintRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        log.info("Creating sprint: {} in project: {} by user: {}",
                request.getName(), request.getProjectId(), currentUser.getId());

        User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        SprintResponse sprint = sprintService.createSprint(request, user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Sprint created successfully", sprint));
    }

    /**
     * Get sprint by ID
     * GET /api/sprints/{sprintId}
     */
    @GetMapping("/{sprintId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SprintResponse>> getSprintById(
            @PathVariable Integer sprintId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Getting sprint: {} for user: {}", sprintId, currentUser.getId());

        User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        SprintResponse sprint = sprintService.getSprintById(sprintId, user);

        return ResponseEntity.ok(
                ApiResponse.success("Sprint retrieved successfully", sprint)
        );
    }

    /**
     * Get sprint details with tasks
     * GET /api/sprints/{sprintId}/details
     */
    @GetMapping("/{sprintId}/details")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SprintDetailResponse>> getSprintDetails(
            @PathVariable Integer sprintId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Getting sprint details: {} for user: {}", sprintId, currentUser.getId());

        User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        SprintDetailResponse sprint = sprintService.getSprintDetails(sprintId, user);

        return ResponseEntity.ok(
                ApiResponse.success("Sprint details retrieved successfully", sprint)
        );
    }

    /**
     * Update sprint
     * PUT /api/sprints/{sprintId}
     */
    @PutMapping("/{sprintId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SprintResponse>> updateSprint(
            @PathVariable Integer sprintId,
            @Valid @RequestBody UpdateSprintRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        log.info("Updating sprint: {} by user: {}", sprintId, currentUser.getId());

        User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        SprintResponse sprint = sprintService.updateSprint(sprintId, request, user);

        return ResponseEntity.ok(
                ApiResponse.success("Sprint updated successfully", sprint)
        );
    }

    /**
     * Start sprint (change status to ACTIVE)
     * POST /api/sprints/{sprintId}/start
     */
    @PostMapping("/{sprintId}/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SprintResponse>> startSprint(
            @PathVariable Integer sprintId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        log.info("Starting sprint: {} by user: {}", sprintId, currentUser.getId());

        User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        SprintResponse sprint = sprintService.startSprint(sprintId, user);

        return ResponseEntity.ok(
                ApiResponse.success("Sprint started successfully", sprint)
        );
    }

    /**
     * Complete sprint (change status to COMPLETED)
     * POST /api/sprints/{sprintId}/complete
     */
    @PostMapping("/{sprintId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SprintResponse>> completeSprint(
            @PathVariable Integer sprintId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        log.info("Completing sprint: {} by user: {}", sprintId, currentUser.getId());

        User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        SprintResponse sprint = sprintService.completeSprint(sprintId, user);

        return ResponseEntity.ok(
                ApiResponse.success("Sprint completed successfully", sprint)
        );
    }

    /**
     * Cancel sprint (change status to CANCELLED)
     * POST /api/sprints/{sprintId}/cancel
     */
    @PostMapping("/{sprintId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SprintResponse>> cancelSprint(
            @PathVariable Integer sprintId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        log.info("Cancelling sprint: {} by user: {}", sprintId, currentUser.getId());

        User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        SprintResponse sprint = sprintService.cancelSprint(sprintId, user);

        return ResponseEntity.ok(
                ApiResponse.success("Sprint cancelled successfully", sprint)
        );
    }

    /**
     * Delete sprint
     * DELETE /api/sprints/{sprintId}
     */
    @DeleteMapping("/{sprintId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteSprint(
            @PathVariable Integer sprintId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest httpRequest) {

        log.info("Deleting sprint: {} by user: {}", sprintId, currentUser.getId());

        User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        sprintService.deleteSprint(sprintId, user);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success("Sprint deleted successfully", null));
    }

    /**
     * Get active sprint for a project
     * GET /api/projects/{projectId}/sprints/active
     */
    @GetMapping("/projects/{projectId}/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SprintResponse>> getActiveSprint(
            @PathVariable Integer projectId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Getting active sprint for project: {}", projectId);

        User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        SprintResponse sprint = sprintService.getActiveSprint(projectId, user);

        return ResponseEntity.ok(
                ApiResponse.success("Active sprint retrieved successfully", sprint)
        );
    }

    /**
     * Get completed sprints for a project
     * GET /api/projects/{projectId}/sprints/completed
     */
    @GetMapping("/projects/{projectId}/completed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SprintResponse>>> getCompletedSprints(
            @PathVariable Integer projectId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Getting completed sprints for project: {}", projectId);

        User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                .orElseThrow();

        List<SprintResponse> sprints = sprintService.getCompletedSprints(projectId, user);

        return ResponseEntity.ok(
                ApiResponse.success("Completed sprints retrieved successfully", sprints)
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
}