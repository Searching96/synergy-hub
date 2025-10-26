package com.synergyhub.service.task;

import com.synergyhub.domain.entity.*;
import com.synergyhub.domain.enums.TaskStatus;
import com.synergyhub.dto.mapper.TaskMapper;
import com.synergyhub.dto.request.CreateTaskRequest;
import com.synergyhub.dto.request.UpdateTaskRequest;
import com.synergyhub.dto.response.TaskResponse;
import com.synergyhub.exception.*;
import com.synergyhub.repository.*;
import com.synergyhub.service.security.AuditLogService;  // ✅ Added
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskMapper taskMapper;
    private final AuditLogService auditLogService;  // ✅ Added

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, User currentUser) {
        log.info("Creating task: {} in project: {} by user: {}",
                request.getTitle(), request.getProjectId(), currentUser.getId());

        // Verify project exists
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ProjectNotFoundException(request.getProjectId()));

        // Verify user has access to project
        verifyProjectAccess(project, currentUser);

        // Build task
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .project(project)
                .creator(currentUser)
                .status(TaskStatus.TO_DO)
                .priority(request.getPriority())
                .storyPoints(request.getStoryPoints())
                .dueDate(request.getDueDate())
                .build();

        // Assign to sprint if provided
        if (request.getSprintId() != null) {
            Sprint sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new SprintNotFoundException(request.getSprintId()));

            if (!sprint.getProject().getId().equals(project.getId())) {
                throw new BadRequestException("Sprint does not belong to this project");
            }

            task.setSprint(sprint);
        }

        // Assign to user if provided
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssigneeId()));

            if (!projectMemberRepository.hasAccessToTaskProject(project.getId(), assignee.getId())) {
                throw new TaskAssignmentException("User is not a member of this project");
            }

            task.setAssignee(assignee);
        }

        Task savedTask = taskRepository.save(task);
        log.info("Task created successfully: {}", savedTask.getId());

        // ✅ Audit log for task creation
        auditLogService.createAuditLog(
                currentUser,
                "TASK_CREATED",
                String.format("Task '%s' (ID: %d) created in project '%s'",
                        savedTask.getTitle(), savedTask.getId(), project.getName()),
                null,  // IP address not available in service layer
                null
        );

        return taskMapper.toTaskResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Integer taskId, User currentUser) {
        log.info("Getting task: {} for user: {}", taskId, currentUser.getId());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        verifyProjectAccess(task.getProject(), currentUser);

        return taskMapper.toTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(Integer projectId, User currentUser) {
        log.info("Getting tasks for project: {} by user: {}", projectId, currentUser.getId());

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        verifyProjectAccess(project, currentUser);

        List<Task> tasks = taskRepository.findByProjectIdWithDetails(projectId);
        return taskMapper.toTaskResponseList(tasks);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksBySprint(Integer sprintId, User currentUser) {
        log.info("Getting tasks for sprint: {}", sprintId);

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));

        verifyProjectAccess(sprint.getProject(), currentUser);

        List<Task> tasks = taskRepository.findBySprintIdOrderByPriorityDescCreatedAtAsc(sprintId);
        return taskMapper.toTaskResponseList(tasks);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksInBacklog(Integer projectId, User currentUser) {
        log.info("Getting backlog tasks for project: {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        verifyProjectAccess(project, currentUser);

        List<Task> tasks = taskRepository
                .findByProjectIdAndSprintIsNullOrderByPriorityDescCreatedAtAsc(projectId);
        return taskMapper.toTaskResponseList(tasks);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(User currentUser) {
        log.info("Getting tasks assigned to user: {}", currentUser.getId());

        List<Task> tasks = taskRepository
                .findByAssigneeIdOrderByPriorityDescCreatedAtAsc(currentUser.getId());
        return taskMapper.toTaskResponseList(tasks);
    }

    @Transactional
    public TaskResponse updateTask(Integer taskId, UpdateTaskRequest request, User currentUser) {
        log.info("Updating task: {} by user: {}", taskId, currentUser.getId());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        verifyProjectAccess(task.getProject(), currentUser);

        // ✅ Track what changed for audit log
        StringBuilder changes = new StringBuilder();
        String oldStatus = task.getStatus() != null ? task.getStatus().name() : null;

        // Update fields
        if (request.getTitle() != null && !request.getTitle().equals(task.getTitle())) {
            changes.append(String.format("Title: '%s' → '%s'; ", task.getTitle(), request.getTitle()));
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null && !request.getStatus().equals(task.getStatus())) {
            changes.append(String.format("Status: %s → %s; ", task.getStatus(), request.getStatus()));
            task.setStatus(request.getStatus());
        }
        if (request.getPriority() != null && !request.getPriority().equals(task.getPriority())) {
            changes.append(String.format("Priority: %s → %s; ", task.getPriority(), request.getPriority()));
            task.setPriority(request.getPriority());
        }
        if (request.getStoryPoints() != null) {
            task.setStoryPoints(request.getStoryPoints());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }

        // Update assignee
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssigneeId()));

            if (!projectMemberRepository.hasAccessToTaskProject(task.getProject().getId(), assignee.getId())) {
                throw new TaskAssignmentException("User is not a member of this project");
            }

            if (!assignee.equals(task.getAssignee())) {
                String oldAssignee = task.getAssignee() != null ? task.getAssignee().getName() : "Unassigned";
                changes.append(String.format("Assignee: %s → %s; ", oldAssignee, assignee.getName()));
            }
            task.setAssignee(assignee);
        }

        // Update sprint
        if (request.getSprintId() != null) {
            Sprint sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new SprintNotFoundException(request.getSprintId()));

            if (!sprint.getProject().getId().equals(task.getProject().getId())) {
                throw new BadRequestException("Sprint does not belong to task's project");
            }

            task.setSprint(sprint);
        }

        Task updatedTask = taskRepository.save(task);
        log.info("Task updated successfully: {}", taskId);

        // ✅ Audit log for task update
        if (changes.length() > 0) {
            auditLogService.createAuditLog(
                    currentUser,
                    "TASK_UPDATED",
                    String.format("Task '%s' (ID: %d) updated: %s",
                            task.getTitle(), taskId, changes.toString()),
                    null,
                    null
            );
        }

        return taskMapper.toTaskResponse(updatedTask);
    }

    @Transactional
    public TaskResponse assignTask(Integer taskId, Integer assigneeId, User currentUser) {
        log.info("Assigning task: {} to user: {}", taskId, assigneeId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        verifyProjectAccess(task.getProject(), currentUser);

        if (!task.canBeAssigned()) {
            // ✅ Audit log for failed assignment
            auditLogService.createAuditLog(
                    currentUser,
                    "TASK_ASSIGNMENT_FAILED",
                    String.format("Failed to assign task '%s' (ID: %d): Invalid state %s",
                            task.getTitle(), taskId, task.getStatus()),
                    null,
                    null
            );
            throw new InvalidTaskStateException("Task cannot be assigned in current state: " + task.getStatus());
        }

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", assigneeId));

        if (!projectMemberRepository.hasAccessToTaskProject(task.getProject().getId(), assignee.getId())) {
            // ✅ Audit log for unauthorized assignment
            auditLogService.createAuditLog(
                    currentUser,
                    "TASK_ASSIGNMENT_FAILED",
                    String.format("Failed to assign task '%s' (ID: %d) to user %d: Not a project member",
                            task.getTitle(), taskId, assigneeId),
                    null,
                    null
            );
            throw new TaskAssignmentException("User is not a member of this project");
        }

        String oldAssignee = task.getAssignee() != null ? task.getAssignee().getName() : "Unassigned";
        task.setAssignee(assignee);
        Task updatedTask = taskRepository.save(task);

        log.info("Task assigned successfully");

        // ✅ Audit log for successful assignment
        auditLogService.createAuditLog(
                currentUser,
                "TASK_ASSIGNED",
                String.format("Task '%s' (ID: %d) assigned: %s → %s",
                        task.getTitle(), taskId, oldAssignee, assignee.getName()),
                null,
                null
        );

        return taskMapper.toTaskResponse(updatedTask);
    }

    @Transactional
    public void unassignTask(Integer taskId, User currentUser) {
        log.info("Unassigning task: {}", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        verifyProjectAccess(task.getProject(), currentUser);

        String oldAssignee = task.getAssignee() != null ? task.getAssignee().getName() : "Unassigned";
        task.setAssignee(null);
        taskRepository.save(task);

        log.info("Task unassigned successfully");

        // ✅ Audit log for unassignment
        auditLogService.createAuditLog(
                currentUser,
                "TASK_UNASSIGNED",
                String.format("Task '%s' (ID: %d) unassigned from %s",
                        task.getTitle(), taskId, oldAssignee),
                null,
                null
        );
    }

    @Transactional
    public TaskResponse moveTaskToSprint(Integer taskId, Integer sprintId, User currentUser) {
        log.info("Moving task: {} to sprint: {}", taskId, sprintId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        verifyProjectAccess(task.getProject(), currentUser);

        String oldLocation;
        String newLocation;

        if (sprintId == null) {
            // Move to backlog
            oldLocation = task.getSprint() != null ? task.getSprint().getName() : "Backlog";
            task.setSprint(null);
            task.setStatus(TaskStatus.BACKLOG);
            newLocation = "Backlog";
        } else {
            Sprint sprint = sprintRepository.findById(sprintId)
                    .orElseThrow(() -> new SprintNotFoundException(sprintId));

            if (!sprint.getProject().getId().equals(task.getProject().getId())) {
                throw new BadRequestException("Sprint does not belong to task's project");
            }

            if (!task.canBeMovedToSprint()) {
                // ✅ Audit log for failed move
                auditLogService.createAuditLog(
                        currentUser,
                        "TASK_MOVE_FAILED",
                        String.format("Failed to move task '%s' (ID: %d) to sprint: Invalid state",
                                task.getTitle(), taskId),
                        null,
                        null
                );
                throw new InvalidTaskStateException("Task cannot be moved to sprint in current state");
            }

            oldLocation = task.getSprint() != null ? task.getSprint().getName() : "Backlog";
            task.setSprint(sprint);
            if (task.getStatus() == TaskStatus.BACKLOG) {
                task.setStatus(TaskStatus.TO_DO);
            }
            newLocation = sprint.getName();
        }

        Task updatedTask = taskRepository.save(task);
        log.info("Task moved successfully");

        // ✅ Audit log for task move
        auditLogService.createAuditLog(
                currentUser,
                "TASK_MOVED",
                String.format("Task '%s' (ID: %d) moved: %s → %s",
                        task.getTitle(), taskId, oldLocation, newLocation),
                null,
                null
        );

        return taskMapper.toTaskResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(Integer taskId, User currentUser) {
        log.info("Deleting task: {} by user: {}", taskId, currentUser.getId());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        verifyProjectAccess(task.getProject(), currentUser);

        String taskTitle = task.getTitle();
        String projectName = task.getProject().getName();

        taskRepository.delete(task);
        log.info("Task deleted successfully: {}", taskId);

        // ✅ Audit log for task deletion
        auditLogService.createAuditLog(
                currentUser,
                "TASK_DELETED",
                String.format("Task '%s' (ID: %d) deleted from project '%s'",
                        taskTitle, taskId, projectName),
                null,
                null
        );
    }

    private void verifyProjectAccess(Project project, User user) {
        if (!projectMemberRepository.hasAccessToTaskProject(project.getId(), user.getId())) {
            // ✅ Audit log for access denied
            auditLogService.createAuditLog(
                    user,
                    "TASK_ACCESS_DENIED",
                    String.format("Access denied to project '%s' (ID: %d)",
                            project.getName(), project.getId()),
                    null,
                    null
            );
            throw new AccessDeniedException("You don't have access to this project");
        }
    }
}
