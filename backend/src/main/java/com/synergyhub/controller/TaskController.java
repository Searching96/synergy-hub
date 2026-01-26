package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.AssignTaskRequest;
import com.synergyhub.dto.request.CreateTaskRequest;
import com.synergyhub.dto.request.UpdateTaskRequest;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.TaskResponse;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.UserContext;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.task.TaskService;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class TaskController {

        private final TaskService taskService;
        private final UserRepository userRepository;

        /**
         * Create a new task
         * POST /api/tasks
         */
        @PostMapping("/api/tasks") // ✅ Full path
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<TaskResponse>> createTask(
                        @Valid @RequestBody CreateTaskRequest request,
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        HttpServletRequest httpRequest) {

                log.info("Creating task: {} in project: {} by user: {}",
                                request.getTitle(), request.getProjectId(), currentUser.getId());

                User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                                .orElseThrow();

                TaskResponse task = taskService.createTask(request, user);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Task created successfully", task));
        }

        /**
         * Get task by ID
         * GET /api/tasks/{taskId}
         */
        @GetMapping("/api/tasks/{taskId}") // ✅ Full path
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(
                        @PathVariable @Positive(message = "Task ID must be positive") Long taskId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("Getting task: {} for user: {}", taskId, currentUser.getId());

                User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                                .orElseThrow();

                TaskResponse task = taskService.getTaskById(taskId, user);

                return ResponseEntity.ok(
                                ApiResponse.success("Task retrieved successfully", task));
        }

        /**
         * Get all tasks for a project
         * GET /api/projects/{projectId}/tasks
         */
        @GetMapping("/api/projects/{projectId}/tasks") // ✅ Correct path
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksByProject(
                        @PathVariable @Positive(message = "Project ID must be positive") Long projectId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("Getting tasks for project: {}", projectId);

                User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                                .orElseThrow();

                List<TaskResponse> tasks = taskService.getTasksByProject(projectId, user);

                return ResponseEntity.ok(
                                ApiResponse.success("Tasks retrieved successfully", tasks));
        }

        /**
         * Get all tasks for a sprint
         * GET /api/sprints/{sprintId}/tasks
         */
        @GetMapping("/api/sprints/{sprintId}/tasks") // ✅ Correct path
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksBySprint(
                        @PathVariable @Positive(message = "Sprint ID must be positive") Long sprintId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("Getting tasks for sprint: {}", sprintId);

                User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                                .orElseThrow();

                List<TaskResponse> tasks = taskService.getTasksBySprint(sprintId, user);

                return ResponseEntity.ok(
                                ApiResponse.success("Sprint tasks retrieved successfully", tasks));
        }

        /**
         * Get subtasks for a specific task
         * GET /api/tasks/{taskId}/subtasks
         */
        @GetMapping("/api/tasks/{taskId}/subtasks")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<List<TaskResponse>>> getSubtasks(
                        @PathVariable @Positive(message = "Task ID must be positive") Long taskId,
                        UserContext userContext) {

                User currentUser = new User();
                currentUser.setId(userContext.getId());

                List<TaskResponse> subtasks = taskService.getSubtasks(taskId, currentUser);

                return ResponseEntity.ok(ApiResponse.success(subtasks));
        }

        /**
         * Get backlog tasks for a project
         * GET /api/projects/{projectId}/backlog
         */
        @GetMapping("/api/projects/{projectId}/backlog") // ✅ Correct path
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<List<TaskResponse>>> getBacklogTasks(
                        @PathVariable @Positive(message = "Project ID must be positive") Long projectId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("Getting backlog tasks for project: {}", projectId);

                User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                                .orElseThrow();

                List<TaskResponse> tasks = taskService.getTasksInBacklog(projectId, user);

                return ResponseEntity.ok(
                                ApiResponse.success("Backlog tasks retrieved successfully", tasks));
        }

        /**
         * Get tasks assigned to current user
         * GET /api/tasks/my-tasks
         */
        @GetMapping("/api/tasks/my-tasks") // ✅ Full path
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<List<TaskResponse>>> getMyTasks(
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("Getting tasks for user: {}", currentUser.getId());

                User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                                .orElseThrow();

                List<TaskResponse> tasks = taskService.getMyTasks(user);

                return ResponseEntity.ok(
                                ApiResponse.success("Your tasks retrieved successfully", tasks));
        }

        /**
         * Update task
         * PUT /api/tasks/{taskId}
         */
        @PutMapping("/api/tasks/{taskId}") // ✅ Full path
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
                        @PathVariable @Positive(message = "Task ID must be positive") Long taskId,
                        @Valid @RequestBody UpdateTaskRequest request,
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        HttpServletRequest httpRequest) {

                log.info("Updating task: {} by user: {}", taskId, currentUser.getId());

                User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                                .orElseThrow();

                TaskResponse task = taskService.updateTask(taskId, request, user);

                return ResponseEntity.ok(
                                ApiResponse.success("Task updated successfully", task));
        }

        /**
         * Assign or unassign task
         * PUT /api/tasks/{taskId}/assignee
         */
        @PutMapping("/api/tasks/{taskId}/assignee")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<TaskResponse>> updateTaskAssignee(
                        @PathVariable @Positive(message = "Task ID must be positive") Long taskId,
                        @Valid @RequestBody AssignTaskRequest request,
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        HttpServletRequest httpRequest) {

                User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                                .orElseThrow();

                TaskResponse task;

                if (request.getAssigneeId() != null) {
                        log.info("Assigning task: {} to user: {}", taskId, request.getAssigneeId());
                        task = taskService.assignTask(taskId, request.getAssigneeId(), user);
                        return ResponseEntity.ok(
                                        ApiResponse.success("Task assigned successfully", task));
                } else {
                        log.info("Unassigning task: {}", taskId);
                        taskService.unassignTask(taskId, user);
                        task = taskService.getTaskById(taskId, user); // Get updated task
                        return ResponseEntity.ok(
                                        ApiResponse.success("Task unassigned successfully", task));
                }
        }

        /**
         * Move task to sprint
         * POST /api/tasks/{taskId}/move-to-sprint/{sprintId}
         */
        @PostMapping("/api/tasks/{taskId}/move-to-sprint/{sprintId}") // ✅ Full path
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<TaskResponse>> moveTaskToSprint(
                        @PathVariable @Positive(message = "Task ID must be positive") Long taskId,
                        @PathVariable @Positive(message = "Sprint ID must be positive") Long sprintId,
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        HttpServletRequest httpRequest) {

                log.info("Moving task: {} to sprint: {}", taskId, sprintId);

                User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                                .orElseThrow();

                TaskResponse task = taskService.moveTaskToSprint(taskId, sprintId, user);

                return ResponseEntity.ok(
                                ApiResponse.success("Task moved to sprint successfully", task));
        }

        /**
         * Move task to backlog
         * POST /api/tasks/{taskId}/move-to-backlog
         */
        @PostMapping("/api/tasks/{taskId}/move-to-backlog") // ✅ Full path
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<TaskResponse>> moveTaskToBacklog(
                        @PathVariable @Positive(message = "Task ID must be positive") Long taskId,
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        HttpServletRequest httpRequest) {

                log.info("Moving task: {} to backlog", taskId);

                User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                                .orElseThrow();

                TaskResponse task = taskService.moveTaskToSprint(taskId, null, user);

                return ResponseEntity.ok(
                                ApiResponse.success("Task moved to backlog successfully", task));
        }

        /**
         * Delete task
         * DELETE /api/tasks/{taskId}
         */
        @DeleteMapping("/api/tasks/{taskId}") // ✅ Full path
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<Void>> deleteTask(
                        @PathVariable @Positive(message = "Task ID must be positive") Long taskId,
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        HttpServletRequest httpRequest) {

                log.info("Permanently deleting task: {} by user: {}", taskId, currentUser.getId());

                User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                                .orElseThrow();

                taskService.deleteTask(taskId, user);

                return ResponseEntity
                                .status(HttpStatus.NO_CONTENT)
                                .body(ApiResponse.success("Task deleted successfully", null));
        }

        @PutMapping("/api/tasks/{taskId}/archive")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<Void>> archiveTask(
                        @PathVariable @Positive(message = "Task ID must be positive") Long taskId,
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        HttpServletRequest httpRequest) {

                log.info("Archiving task: {} by user: {}", taskId, currentUser.getId());

                User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                                .orElseThrow();

                taskService.archiveTask(taskId, user);

                return ResponseEntity.ok(
                                ApiResponse.success("Task archived successfully", null));
        }

        @PutMapping("/api/tasks/{taskId}/unarchive")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<Void>> unarchiveTask(
                        @PathVariable @Positive(message = "Task ID must be positive") Long taskId,
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        HttpServletRequest httpRequest) {

                log.info("Unarchiving task: {} by user: {}", taskId, currentUser.getId());

                User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                                .orElseThrow();

                taskService.unarchiveTask(taskId, user);

                return ResponseEntity.ok(
                                ApiResponse.success("Task unarchived successfully", null));
        }

        /**
         * Get all epics for a project
         * GET /api/projects/{projectId}/epics
         */
        @GetMapping("/api/projects/{projectId}/epics")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<List<TaskResponse>>> getProjectEpics(
                        @PathVariable @Positive(message = "Project ID must be positive") Long projectId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                                .orElseThrow();

                List<TaskResponse> epics = taskService.getProjectEpics(projectId, user);

                return ResponseEntity.ok(ApiResponse.success(epics));
        }

        /**
         * Get all issues belonging to an epic
         * GET /api/tasks/{epicId}/epic-children
         */
        @GetMapping("/api/tasks/{epicId}/epic-children")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<List<TaskResponse>>> getEpicChildren(
                        @PathVariable @Positive(message = "Epic ID must be positive") Long epicId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                                .orElseThrow();

                List<TaskResponse> children = taskService.getEpicChildren(epicId, user);

                return ResponseEntity.ok(ApiResponse.success(children));
        }

        /**
         * Link tasks
         * POST /api/tasks/{taskId}/links/{linkedTaskId}
         */
        @PostMapping("/api/tasks/{taskId}/links/{linkedTaskId}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<TaskResponse>> linkTasks(
                        @PathVariable @Positive(message = "Task ID must be positive") Long taskId,
                        @PathVariable @Positive(message = "Linked Task ID must be positive") Long linkedTaskId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("Linking task: {} to task: {} by user: {}", taskId, linkedTaskId, currentUser.getId());

                User user = userRepository.findByEmailWithRolesAndPermissions(currentUser.getEmail())
                                .orElseThrow();

                TaskResponse task = taskService.linkTasks(taskId, linkedTaskId, user);

                return ResponseEntity.ok(ApiResponse.success("Tasks linked successfully", task));
        }
}