package com.synergyhub.service.task;

import com.synergyhub.domain.entity.*;
import com.synergyhub.domain.enums.TaskStatus;
import com.synergyhub.domain.enums.TaskType;
import com.synergyhub.dto.mapper.TaskMapper;
import com.synergyhub.dto.request.CreateTaskRequest;
import com.synergyhub.dto.request.UpdateTaskRequest;
import com.synergyhub.dto.response.TaskResponse;
import com.synergyhub.exception.*;
import com.synergyhub.repository.*;
import com.synergyhub.security.OrganizationContext;
import com.synergyhub.service.security.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.TO_DO)
                .priority(request.getPriority())
                .type(request.getType() != null ? request.getType() : TaskType.TASK)
                .storyPoints(request.getStoryPoints())
                .dueDate(request.getDueDate() != null ? request.getDueDate().atTime(23, 59, 59) : null)
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

            // Business Logic: Assignee must be in project
            Long orgId = OrganizationContext.getcurrentOrgId();
            if (!taskRepository.hasAccessToTaskProjectInOrganization(project.getId(), assignee.getId(), orgId)) {
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

            // Validation 2: Parent cannot be a subtask itself
            if (parentTask.getParentTask() != null) {
                throw new BadRequestException("Cannot create a subtask of a subtask");
            }

            task.setParentTask(parentTask);
        }

        if (request.getStartDate() != null) {
            task.setStartDate(request.getStartDate().atStartOfDay());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate().atTime(23, 59, 59));
        }
        task.setEstimatedHours(request.getEstimatedHours());
        if (request.getLabels() != null) {
            task.setLabels(new java.util.ArrayList<>(request.getLabels()));
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

        return mapToResponseWithWatching(savedTask, currentUser);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long taskId, User currentUser) {
        log.info("Getting task: {} for user: {}", taskId, currentUser.getId());

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        return mapToResponseWithWatching(task, currentUser);
    }

    @PreAuthorize("@projectSecurity.hasProjectAccess(#projectId, #currentUser)")
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(Long projectId, User currentUser) {
        log.info("Getting tasks for project: {} by user: {}", projectId, currentUser.getId());

        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        Long orgId = OrganizationContext.getcurrentOrgId();
        List<Task> tasks = taskRepository.findByProjectIdWithDetailsInOrganization(projectId, orgId);
        return tasks.stream().map(t -> mapToResponseWithWatching(t, currentUser)).toList();
    }

    @PreAuthorize("@projectSecurity.hasSprintAccess(#sprintId, #currentUser)")
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksBySprint(Long sprintId, User currentUser) {
        log.info("Getting tasks for sprint: {}", sprintId);

        if (!sprintRepository.existsById(sprintId)) {
            throw new SprintNotFoundException(sprintId);
        }

        Long orgId = OrganizationContext.getcurrentOrgId();
        List<Task> tasks = taskRepository.findBySprintIdOrderByPriorityDescCreatedAtAscInOrganization(sprintId, orgId);
        return tasks.stream().map(t -> mapToResponseWithWatching(t, currentUser)).toList();
    }

    @PreAuthorize("@projectSecurity.hasProjectAccess(#projectId, #currentUser)")
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksInBacklog(Long projectId, User currentUser) {
        log.info("Getting backlog tasks for project: {}", projectId);

        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        Long orgId = OrganizationContext.getcurrentOrgId();
        List<Task> tasks = taskRepository
                .findByProjectIdAndSprintIsNullOrderByPriorityDescCreatedAtAscInOrganization(projectId, orgId);
        return tasks.stream().map(t -> mapToResponseWithWatching(t, currentUser)).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(User currentUser) {
        log.info("Getting tasks assigned to user: {}", currentUser.getId());

        Long orgId = OrganizationContext.getcurrentOrgId();
        List<Task> tasks = taskRepository
                .findByAssigneeIdOrderByPriorityDescCreatedAtAscInOrganization(currentUser.getId(), orgId);
        return tasks.stream().map(t -> mapToResponseWithWatching(t, currentUser)).toList();
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request, User currentUser) {
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
        if (request.getType() != null && !request.getType().equals(task.getType())) {
            changes.append(String.format("Type: %s → %s; ", task.getType(), request.getType()));
            task.setType(request.getType());
        }
        if (request.getStoryPoints() != null) {
            task.setStoryPoints(request.getStoryPoints());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate().atTime(23, 59, 59));
        }
        if (request.getStartDate() != null) {
            task.setStartDate(request.getStartDate().atStartOfDay());
        }
        if (request.getEstimatedHours() != null) {
            task.setEstimatedHours(request.getEstimatedHours());
        }
        if (request.getLabels() != null) {
            task.setLabels(new java.util.ArrayList<>(request.getLabels()));
        }

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssigneeId()));

            Long orgId = OrganizationContext.getcurrentOrgId();
            if (!taskRepository.hasAccessToTaskProjectInOrganization(task.getProject().getId(), assignee.getId(), orgId)) {
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

        if (request.getEpicId() != null) {
            Task epic = taskRepository.findById(request.getEpicId())
                    .orElseThrow(() -> new TaskNotFoundException(request.getEpicId()));

            if (epic.getType() != TaskType.EPIC) {
                throw new BadRequestException("Selected task is not an epic");
            }
            if (!epic.getProject().getId().equals(task.getProject().getId())) {
                throw new BadRequestException("Epic does not belong to task's project");
            }
            
            if (task.getEpic() == null || !task.getEpic().getId().equals(epic.getId())) {
                String oldEpic = task.getEpic() != null ? task.getEpic().getTitle() : "None";
                changes.append(String.format("Epic: %s → %s; ", oldEpic, epic.getTitle()));
                task.setEpic(epic);
            }
        }

        Task savedTask = taskRepository.save(task);
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

        return mapToResponseWithWatching(savedTask, currentUser);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional
    public TaskResponse assignTask(Long taskId, Long assigneeId, User currentUser) {
        log.info("Assigning task: {} to user: {}", taskId, assigneeId);

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        if (!task.canBeAssigned()) {
            throw new InvalidTaskStateException("Task cannot be assigned in current state: " + task.getStatus());
        }

        User assignee = userRepository.findById(assigneeId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", assigneeId));

        Long orgId = OrganizationContext.getcurrentOrgId();
        if (!taskRepository.hasAccessToTaskProjectInOrganization(task.getProject().getId(), assignee.getId(), orgId)) {
            throw new TaskAssignmentException("User is not a member of this project");
        }

        String oldAssignee = task.getAssignee() != null ? task.getAssignee().getName() : "Unassigned";
        task.setAssignee(assignee);
        Task updatedTask = taskRepository.save(task);

        auditLogService.createAuditLog(
                currentUser,
                "TASK_ASSIGNED",
                String.format("Task '%s' (ID: %d) assigned: %s → %s",
                        task.getTitle(), taskId, oldAssignee, assignee.getName()),
                null,
                null);

        return mapToResponseWithWatching(updatedTask, currentUser);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional
    public void unassignTask(Long taskId, User currentUser) {
        log.info("Unassigning task: {}", taskId);

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        String oldAssignee = task.getAssignee() != null ? task.getAssignee().getName() : "Unassigned";
        task.setAssignee(null);
        taskRepository.save(task);

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
    public TaskResponse moveTaskToSprint(Long taskId, Long sprintId, User currentUser) {
        log.info("Moving task: {} to sprint: {}", taskId, sprintId);

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        String oldLocation;
        String newLocation;

        if (sprintId == null) {
            oldLocation = task.getSprint() != null ? task.getSprint().getName() : "Backlog";
            task.setSprint(null);
            task.setStatus(TaskStatus.BACKLOG);
            newLocation = "Backlog";
        } else {
            Sprint sprint = sprintRepository.findById(sprintId)
                    .orElseThrow(() -> new SprintNotFoundException(sprintId));

            if (!java.util.Objects.equals(sprint.getProject().getId(), task.getProject().getId())) {
                throw new BadRequestException("Sprint does not belong to task's project");
            }

            if (!task.canBeMovedToSprint()) {
                throw new InvalidTaskStateException("Task cannot be moved to sprint in current state");
            }

            oldLocation = task.getSprint() != null ? task.getSprint().getName() : "Backlog";
            task.setSprint(sprint);
            if (task.getStatus() == TaskStatus.BACKLOG) {
                task.setStatus(TaskStatus.TO_DO);
            }
            newLocation = sprint.getName();
        }

        Task savedTask = taskRepository.save(task);

        auditLogService.createAuditLog(
                currentUser,
                "TASK_MOVED",
                String.format("Task '%s' (ID: %d) moved: %s → %s",
                        task.getTitle(), taskId, oldLocation, newLocation),
                null,
                null);

        return mapToResponseWithWatching(savedTask, currentUser);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional
    public void deleteTask(Long taskId, User currentUser) {
        log.info("Permanently deleting task: {} by user: {}", taskId, currentUser.getId());

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        // Handle epic children and subtasks pointers before deletion
        log.info("Nullifying references to task {} before deletion", taskId);
        taskRepository.nullifyEpicReferences(taskId);
        taskRepository.nullifyParentTaskReferences(taskId);
        taskRepository.deleteIncomingLinks(taskId);
        
        // Note: subtasks themselves are deleted due to CascadeType.ALL on parentTask relationship,
        // but we nullify pointers just in case there's a non-cascade path involved or to be safe.

        String taskTitle = task.getTitle();
        String projectName = task.getProject().getName();

        taskRepository.delete(task);

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
    public void archiveTask(Long taskId, User currentUser) {
        log.info("Archiving task: {} by user: {}", taskId, currentUser.getId());

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        task.setArchived(true);
        taskRepository.save(task);

        auditLogService.createAuditLog(
                currentUser,
                "TASK_ARCHIVED",
                String.format("Task '%s' (ID: %d) archived in project '%s'",
                        task.getTitle(), taskId, task.getProject().getName()),
                task.getProject().getId().toString(),
                null);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional
    public void unarchiveTask(Long taskId, User currentUser) {
        log.info("Unarchiving task: {} by user: {}", taskId, currentUser.getId());

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        task.setArchived(false);
        taskRepository.save(task);

        auditLogService.createAuditLog(
                currentUser,
                "TASK_UNARCHIVED",
                String.format("Task '%s' (ID: %d) unarchived in project '%s'",
                        task.getTitle(), taskId, task.getProject().getName()),
                task.getProject().getId().toString(),
                null);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#parentTaskId, #currentUser)")
    @Transactional(readOnly = true)
    public List<TaskResponse> getSubtasks(Long parentTaskId, User currentUser) {
        log.info("Fetching subtasks for task: {}", parentTaskId);
        
        if (!taskRepository.existsById(parentTaskId)) {
            throw new TaskNotFoundException(parentTaskId);
        }

        Long orgId = OrganizationContext.getcurrentOrgId();
        List<Task> subtasks = taskRepository.findByParentTaskIdInOrganization(parentTaskId, orgId);
        return subtasks.stream().map(t -> mapToResponseWithWatching(t, currentUser)).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getProjectEpics(Long projectId, User currentUser) {
        log.info("Getting epics for project: {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        Long orgId = OrganizationContext.getcurrentOrgId();
        if (!taskRepository.hasAccessToTaskProjectInOrganization(projectId, currentUser.getId(), orgId)) {
            throw new UnauthorizedException("User does not have access to this project");
        }

        List<Task> epics = taskRepository.findByProjectIdAndTypeInOrganization(projectId, TaskType.EPIC, orgId);
        return epics.stream().map(t -> mapToResponseWithWatching(t, currentUser)).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getEpicChildren(Long epicId, User currentUser) {
        log.info("Getting children for epic: {}", epicId);

        Task epic = taskRepository.findById(epicId)
            .orElseThrow(() -> new TaskNotFoundException(epicId));

        if (epic.getType() != TaskType.EPIC) {
            throw new BadRequestException("Task is not an epic");
        }

        Long orgId = OrganizationContext.getcurrentOrgId();
        if (!taskRepository.hasAccessToTaskProjectInOrganization(epic.getProject().getId(), currentUser.getId(), orgId)) {
            throw new UnauthorizedException("User does not have access to this project");
        }

        List<Task> children = taskRepository.findByEpicIdInOrganization(epicId, orgId);
        return children.stream().map(t -> mapToResponseWithWatching(t, currentUser)).toList();
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional
    public TaskResponse watchTask(Long taskId, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        task.getWatchers().add(currentUser);
        task = taskRepository.save(task);
        return mapToResponseWithWatching(task, currentUser);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional
    public TaskResponse unwatchTask(Long taskId, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        task.getWatchers().removeIf(u -> u.getId().equals(currentUser.getId()));
        task = taskRepository.save(task);
        return mapToResponseWithWatching(task, currentUser);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional
    public TaskResponse linkTasks(Long taskId, Long linkedTaskId, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        Task linkedTask = taskRepository.findById(linkedTaskId)
                .orElseThrow(() -> new TaskNotFoundException(linkedTaskId));

        if (!task.getProject().getId().equals(linkedTask.getProject().getId())) {
            throw new BadRequestException("Linked task must belong to the same project");
        }

        // Add bi-directional linking
        task.getLinkedTasks().add(linkedTask);
        linkedTask.getLinkedTasks().add(task);

        taskRepository.save(task);
        taskRepository.save(linkedTask);

        return mapToResponseWithWatching(task, currentUser);
    }

    private TaskResponse mapToResponseWithWatching(Task task, User user) {
        TaskResponse response = taskMapper.toTaskResponse(task);
        response.setWatching(task.getWatchers().stream().anyMatch(w -> w.getId().equals(user.getId())));
        return response;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks(User currentUser) {
        Long orgId = OrganizationContext.getcurrentOrgId();
        if (orgId == null) {
            throw new IllegalStateException("Organization context is missing for this request.");
        }
        List<Task> tasks = taskRepository.findAllByOrganizationId(orgId);
        return tasks.stream().map(t -> mapToResponseWithWatching(t, currentUser)).toList();
    }
}
