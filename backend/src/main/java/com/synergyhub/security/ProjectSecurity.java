package com.synergyhub.security;

import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.User;
import com.synergyhub.exception.UnauthorizedProjectAccessException;
import com.synergyhub.repository.ProjectMemberRepository;
import com.synergyhub.repository.SprintRepository;
import com.synergyhub.repository.TaskRepository; // Add TaskRepository
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("projectSecurity")
@RequiredArgsConstructor
@Slf4j
public class ProjectSecurity {

    private final ProjectMemberRepository projectMemberRepository;
    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository; // ✅ Inject TaskRepository

    public boolean hasProjectAccess(Integer projectId, User user) {
        if (projectId == null || user == null) return false;
        return projectMemberRepository.hasAccessToTaskProject(projectId, user.getId());
    }

    public boolean hasSprintAccess(Integer sprintId, User user) {
        if (sprintId == null || user == null) return false;
        return sprintRepository.findById(sprintId)
                .map(sprint -> hasProjectAccess(sprint.getProject().getId(), user))
                .orElse(false);
    }

    // ✅ NEW METHOD for TaskService
    public boolean hasTaskAccess(Integer taskId, User user) {
        if (taskId == null || user == null) return false;
        // Lookup task, get project ID, check access
        return taskRepository.findById(taskId)
                .map(task -> hasProjectAccess(task.getProject().getId(), user))
                .orElse(false);
    }

    public void requireAccess(Project project, User user) {
        if (!hasAccess(project, user)) {
            throw new UnauthorizedProjectAccessException(project.getId(), user.getId());
        }
    }

    public void requireLeadOrAdmin(Project project, User user) {
        if (!isAdmin(user) && !isProjectLead(project, user)) {
            throw new UnauthorizedProjectAccessException("Only project lead or admin can perform this action");
        }
    }

    public boolean hasAccess(Project project, User user) {
        return isAdmin(user) || 
               isProjectLead(project, user) || 
               projectMemberRepository.existsByProjectIdAndUserId(project.getId(), user.getId());
    }

    private boolean isProjectLead(Project project, User user) {
        return project.getProjectLead().getId().equals(user.getId());
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getName()));
    }
}