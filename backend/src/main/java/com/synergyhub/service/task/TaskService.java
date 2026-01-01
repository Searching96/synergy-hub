package com.synergyhub.service.task;

import com.synergyhub.domain.entity.*;
import com.synergyhub.domain.enums.TaskStatus;
import com.synergyhub.dto.mapper.TaskMapper;
import com.synergyhub.dto.request.CreateTaskRequest;
import com.synergyhub.dto.request.UpdateTaskRequest;
import com.synergyhub.dto.response.TaskResponse;
import com.synergyhub.exception.*;
import com.synergyhub.repository.*;
import com.synergyhub.service.security.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize; // ✅ Added
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
    private final AuditLogService auditLogService;

    @PreAuthorize("@projectSecurity.hasProjectAccess(#request.projectId, #currentUser)")
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, User currentUser) {
        log.info("Creating task: {} in project: {} by user: {}",
                request.getTitle(), request.getProjectId(), currentUser.getId());

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ProjectNotFoundException(request.getProjectId()));

        // Build task
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .project(project)
                .reporter(currentUser)
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

            // Business Logic: Assignee must be in project (Not a security check for
            // currentUser, but a validity check for assignee)
            if (!projectMemberRepository.hasAccessToTaskProject(project.getId(), assignee.getId())) {
                throw new TaskAssignmentException("User is not a member of this project");
            }
            task.setAssignee(assignee);
        }

        // Handle Subtasks
        if (request.getParentTaskId() != null) {
            Task parentTask = taskRepository.findById(request.getParentTaskId())
                    .orElseThrow(() -> new TaskNotFoundException(request.getParentTaskId()));

            // Validation 1: Parent must be in the same project
            if (!parentTask.getProject().getId().equals(project.getId())) {
                throw new BadRequestException("Subtask must belong to the same project as the parent task");
            }

            // Validation 2: Parent cannot be a subtask itself (Prevent infinite nesting for
            // MVP)
            if (parentTask.getParentTask() != null) {
                throw new BadRequestException("Cannot create a subtask of a subtask");
            }

            task.setParentTask(parentTask);
        }

        Task savedTask = taskRepository.save(task);
        log.info("Task created successfully: {}", savedTask.getId());

        auditLogService.createAuditLog(
                currentUser,
                "TASK_CREATED",
                String.format("Task '%s' (ID: %d) created in project '%s'",
                        savedTask.getTitle(), savedTask.getId(), project.getName()),
                null,
                null);

        return taskMapper.toTaskResponse(savedTask);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Integer taskId, User currentUser) {
        log.info("Getting task: {} for user: {}", taskId, currentUser.getId());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        return taskMapper.toTaskResponse(task);
    }

    @PreAuthorize("@projectSecurity.hasProjectAccess(#projectId, #currentUser)")
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(Integer projectId, User currentUser) {
        log.info("Getting tasks for project: {} by user: {}", projectId, currentUser.getId());

        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        List<Task> tasks = taskRepository.findByProjectIdWithDetails(projectId);
        return taskMapper.toTaskResponseList(tasks);
    }

    @PreAuthorize("@projectSecurity.hasSprintAccess(#sprintId, #currentUser)")
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksBySprint(Integer sprintId, User currentUser) {
        log.info("Getting tasks for sprint: {}", sprintId);

        if (!sprintRepository.existsById(sprintId)) {
            throw new SprintNotFoundException(sprintId);
        }

        List<Task> tasks = taskRepository.findBySprintIdOrderByPriorityDescCreatedAtAsc(sprintId);
        return taskMapper.toTaskResponseList(tasks);
    }

    @PreAuthorize("@projectSecurity.hasProjectAccess(#projectId, #currentUser)")
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksInBacklog(Integer projectId, User currentUser) {
        log.info("Getting backlog tasks for project: {}", projectId);

        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        List<Task> tasks = taskRepository
                .findByProjectIdAndSprintIsNullOrderByPriorityDescCreatedAtAsc(projectId);
        return taskMapper.toTaskResponseList(tasks);
    }

    // No specific project security needed here as it's "My Tasks",
    // but good practice to ensure the user exists or is active if needed.
    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(User currentUser) {
        log.info("Getting tasks assigned to user: {}", currentUser.getId());

        List<Task> tasks = taskRepository
                .findByAssigneeIdOrderByPriorityDescCreatedAtAsc(currentUser.getId());
        return taskMapper.toTaskResponseList(tasks);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional
    public TaskResponse updateTask(Integer taskId, UpdateTaskRequest request, User currentUser) {
        log.info("Updating task: {} by user: {}", taskId, currentUser.getId());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        StringBuilder changes = new StringBuilder();

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

        if (!changes.isEmpty()) {
            auditLogService.createAuditLog(
                    currentUser,
                    "TASK_UPDATED",
                    String.format("Task '%s' (ID: %d) updated: %s",
                            task.getTitle(), taskId, changes),
                    null,
                    null);
        }

        return taskMapper.toTaskResponse(updatedTask);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional
    public TaskResponse assignTask(Integer taskId, Integer assigneeId, User currentUser) {
        log.info("Assigning task: {} to user: {}", taskId, assigneeId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        if (!task.canBeAssigned()) {
            auditLogService.createAuditLog(
                    currentUser,
                    "TASK_ASSIGNMENT_FAILED",
                    String.format("Failed to assign task '%s' (ID: %d): Invalid state %s",
                            task.getTitle(), taskId, task.getStatus()),
                    null,
                    null);
            throw new InvalidTaskStateException("Task cannot be assigned in current state: " + task.getStatus());
        }

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", assigneeId));

        if (!projectMemberRepository.hasAccessToTaskProject(task.getProject().getId(), assignee.getId())) {
            auditLogService.createAuditLog(
                    currentUser,
                    "TASK_ASSIGNMENT_FAILED",
                    String.format("Failed to assign task '%s' (ID: %d) to user %d: Not a project member",
                            task.getTitle(), taskId, assigneeId),
                    null,
                    null);
            throw new TaskAssignmentException("User is not a member of this project");
        }

        String oldAssignee = task.getAssignee() != null ? task.getAssignee().getName() : "Unassigned";
        task.setAssignee(assignee);
        Task updatedTask = taskRepository.save(task);

        log.info("Task assigned successfully");

        auditLogService.createAuditLog(
                currentUser,
                "TASK_ASSIGNED",
                String.format("Task '%s' (ID: %d) assigned: %s → %s",
                        task.getTitle(), taskId, oldAssignee, assignee.getName()),
                null,
                null);

        return taskMapper.toTaskResponse(updatedTask);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional
    public void unassignTask(Integer taskId, User currentUser) {
        log.info("Unassigning task: {}", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        String oldAssignee = task.getAssignee() != null ? task.getAssignee().getName() : "Unassigned";
        task.setAssignee(null);
        taskRepository.save(task);

        log.info("Task unassigned successfully");

        auditLogService.createAuditLog(
                currentUser,
                "TASK_UNASSIGNED",
                String.format("Task '%s' (ID: %d) unassigned from %s",
                        task.getTitle(), taskId, oldAssignee),
                null,
                null);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional
    public TaskResponse moveTaskToSprint(Integer taskId, Integer sprintId, User currentUser) {
        log.info("Moving task: {} to sprint: {}", taskId, sprintId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

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
                auditLogService.createAuditLog(
                        currentUser,
                        "TASK_MOVE_FAILED",
                        String.format("Failed to move task '%s' (ID: %d) to sprint: Invalid state",
                                task.getTitle(), taskId),
                        null,
                        null);
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

        auditLogService.createAuditLog(
                currentUser,
                "TASK_MOVED",
                String.format("Task '%s' (ID: %d) moved: %s → %s",
                        task.getTitle(), taskId, oldLocation, newLocation),
                null,
                null);

        return taskMapper.toTaskResponse(updatedTask);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional
    public void deleteTask(Integer taskId, User currentUser) {
        log.info("Permanently deleting task: {} by user: {}", taskId, currentUser.getId());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        String taskTitle = task.getTitle();
        String projectName = task.getProject().getName();

        taskRepository.delete(task);
        log.info("Task permanently deleted: {}", taskId);

        auditLogService.createAuditLog(
                currentUser,
                "TASK_DELETED",
                String.format("Task '%s' (ID: %d) permanently deleted from project '%s'",
                        taskTitle, taskId, projectName),
                null,
                null);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional
    public void archiveTask(Integer taskId, User currentUser) {
        log.info("Archiving task: {} by user: {}", taskId, currentUser.getId());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        task.setArchived(true);
        taskRepository.save(task);
        log.info("Task archived successfully: {}", taskId);

        auditLogService.createAuditLog(
                currentUser,
                "TASK_ARCHIVED",
                String.format("Task '%s' (ID: %d) archived in project '%s'",
                        task.getTitle(), taskId, task.getProject().getName()),
                task.getProject().getId(),
                null);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional
    public void unarchiveTask(Integer taskId, User currentUser) {
        log.info("Unarchiving task: {} by user: {}", taskId, currentUser.getId());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        task.setArchived(false);
        taskRepository.save(task);
        log.info("Task unarchived successfully: {}", taskId);

        auditLogService.createAuditLog(
                currentUser,
                "TASK_UNARCHIVED",
                String.format("Task '%s' (ID: %d) unarchived in project '%s'",
                        task.getTitle(), taskId, task.getProject().getName()),
                task.getProject().getId(),
                null);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#parentTaskId, #currentUser)")
    @Transactional(readOnly = true)
    public List<TaskResponse> getSubtasks(Integer parentTaskId, User currentUser) {
        log.info("Fetching subtasks for task: {}", parentTaskId);
        
        if (!taskRepository.existsById(parentTaskId)) {
            throw new TaskNotFoundException(parentTaskId);
        }

        List<Task> subtasks = taskRepository.findByParentTaskId(parentTaskId);
        return taskMapper.toTaskResponseList(subtasks);
    }
}