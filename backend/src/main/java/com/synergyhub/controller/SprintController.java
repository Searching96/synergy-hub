package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.CreateSprintRequest;
import com.synergyhub.dto.request.UpdateSprintRequest;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.SprintDetailResponse;
import com.synergyhub.dto.response.SprintResponse;
import com.synergyhub.security.UserContext;
import com.synergyhub.service.sprint.SprintService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sprints")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SprintController {

        private final SprintService sprintService;

        /**
         * Create a new sprint
         * POST /api/sprints
         */
        @PostMapping
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<SprintResponse>> createSprint(
                        @Valid @RequestBody CreateSprintRequest request,
                        UserContext userContext,
                        HttpServletRequest httpRequest) {

                log.info("Creating sprint: {} in project: {} by user: {}",
                                request.getName(), request.getProjectId(), userContext.getId());

                User user = new User();
                user.setId(userContext.getId());
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
                        @PathVariable @Positive(message = "Sprint ID must be positive") Long sprintId,
                        UserContext userContext) {

                log.info("Getting sprint: {} for user: {}", sprintId, userContext.getId());
                SprintResponse sprint = sprintService.getSprintById(sprintId, toUser(userContext));
                return ResponseEntity.ok(
                                ApiResponse.success("Sprint retrieved successfully", sprint));
        }

        /**
         * Get sprint details with tasks
         * GET /api/sprints/{sprintId}/details
         */
        @GetMapping("/{sprintId}/details")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<SprintDetailResponse>> getSprintDetails(
                        @PathVariable @Positive(message = "Sprint ID must be positive") Long sprintId,
                        UserContext userContext) {

                log.info("Getting sprint details: {} for user: {}", sprintId, userContext.getId());
                SprintDetailResponse sprint = sprintService.getSprintDetails(sprintId, toUser(userContext));
                return ResponseEntity.ok(
                                ApiResponse.success("Sprint details retrieved successfully", sprint));
        }

        /**
         * Update sprint
         * PUT /api/sprints/{sprintId}
         */
        @PutMapping("/{sprintId}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<SprintResponse>> updateSprint(
                        @PathVariable @Positive(message = "Sprint ID must be positive") Long sprintId,
                        @Valid @RequestBody UpdateSprintRequest request,
                        UserContext userContext,
                        HttpServletRequest httpRequest) {

                log.info("Updating sprint: {} by user: {}", sprintId, userContext.getId());
                SprintResponse updatedSprint = sprintService.updateSprint(sprintId, request, toUser(userContext));
                return ResponseEntity.ok(
                                ApiResponse.success("Sprint updated successfully", updatedSprint));
        }

        /**
         * Start sprint (change status to ACTIVE)
         * POST /api/sprints/{sprintId}/start
         */
        @PostMapping("/{sprintId}/start")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<SprintResponse>> startSprint(
                        @PathVariable @Positive(message = "Sprint ID must be positive") Long sprintId,
                        UserContext userContext,
                        HttpServletRequest httpRequest) {

                log.info("Starting sprint: {} by user: {}", sprintId, userContext.getId());
                SprintResponse startedSprint = sprintService.startSprint(sprintId, toUser(userContext));
                return ResponseEntity.ok(
                                ApiResponse.success("Sprint started successfully", startedSprint));
        }

        /**
         * Complete sprint (change status to COMPLETED)
         * POST /api/sprints/{sprintId}/complete
         */
        @PostMapping("/{sprintId}/complete")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<SprintResponse>> completeSprint(
                        @PathVariable @Positive(message = "Sprint ID must be positive") Long sprintId,
                        UserContext userContext,
                        HttpServletRequest httpRequest) {

                log.info("Completing sprint: {} by user: {}", sprintId, userContext.getId());
                SprintResponse completedSprint = sprintService.completeSprint(sprintId, toUser(userContext));
                return ResponseEntity.ok(
                                ApiResponse.success("Sprint completed successfully", completedSprint));
        }

        /**
         * Cancel sprint (change status to CANCELLED)
         * POST /api/sprints/{sprintId}/cancel
         */
        @PostMapping("/{sprintId}/cancel")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<SprintResponse>> cancelSprint(
                        @PathVariable @Positive(message = "Sprint ID must be positive") Long sprintId,
                        UserContext userContext,
                        HttpServletRequest httpRequest) {

                log.info("Cancelling sprint: {} by user: {}", sprintId, userContext.getId());
                SprintResponse cancelledSprint = sprintService.cancelSprint(sprintId, toUser(userContext));
                return ResponseEntity.ok(
                                ApiResponse.success("Sprint cancelled successfully", cancelledSprint));
        }

        /**
         * Delete sprint
         * DELETE /api/sprints/{sprintId}
         */
        @DeleteMapping("/{sprintId}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<Void>> deleteSprint(
                        @PathVariable @Positive(message = "Sprint ID must be positive") Long sprintId,
                        UserContext userContext,
                        HttpServletRequest httpRequest) {

                log.info("Deleting sprint: {} by user: {}", sprintId, userContext.getId());
                sprintService.deleteSprint(sprintId, toUser(userContext));
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
                        @PathVariable @Positive(message = "Project ID must be positive") Long projectId,
                        UserContext userContext) {

                log.info("Getting active sprint for project: {}", projectId);
                SprintResponse sprint = sprintService.getActiveSprint(projectId, toUser(userContext));
                return ResponseEntity.ok(
                                ApiResponse.success("Active sprint retrieved successfully", sprint));
        }

        /**
         * Get completed sprints for a project
         * GET /api/projects/{projectId}/sprints/completed
         */
        @GetMapping("/projects/{projectId}/completed")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<List<SprintResponse>>> getCompletedSprints(
                        @PathVariable @Positive(message = "Project ID must be positive") Long projectId,
                        UserContext userContext) {

                log.info("Getting completed sprints for project: {}", projectId);
                List<SprintResponse> sprints = sprintService.getCompletedSprints(projectId, toUser(userContext));
                return ResponseEntity.ok(
                                ApiResponse.success("Completed sprints retrieved successfully", sprints));
        }

        // ========================================
        // Helper Methods
        // ========================================

        /**
         * Extract client IP address from request
         */

        private User toUser(UserContext userContext) {
                User user = new User();
                user.setId(userContext.getId());
                user.setEmail(userContext.getEmail());
                // Fallback name to prevent NPE in AuditLogService
                user.setName(userContext.getEmail());
                return user;
        }
}