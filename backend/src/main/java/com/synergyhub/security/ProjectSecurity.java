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

    public boolean hasProjectAccess(Long projectId, User user) {
        return user != null && hasProjectAccess(projectId, user.getId());
    }

    public boolean hasProjectAccess(Long projectId, Long userId) {
        if (projectId == null || userId == null) {
            log.warn("Project access check failed: projectId={}, userId={}", projectId, userId);
            return false;
        }
        boolean hasAccess = projectMemberRepository.hasAccessToTaskProject(projectId, userId);
        log.debug("User {} access to project {}: {}", userId, projectId, hasAccess);
        return hasAccess;
    }

    public boolean hasSprintAccess(Long sprintId, User user) {
        if (sprintId == null || user == null) return false;
        return sprintRepository.findById(sprintId)
            .map(sprint -> hasProjectAccess(sprint.getProject().getId(), user.getId()))
            .orElse(false);
    }

    // ✅ NEW METHOD for TaskService
    public boolean hasTaskAccess(Long taskId, User user) {
        return user != null && hasTaskAccess(taskId, user.getId());
    }
    
    public boolean hasTaskAccess(Long taskId, UserPrincipal principal) {
        return principal != null && hasTaskAccess(taskId, principal.getId());
    }

    public boolean hasTaskAccess(Long taskId, Long userId) {
        if (taskId == null || userId == null) {
            log.warn("Task access check failed: taskId={}, userId={}", taskId, userId);
            return false;
        }
        // Lookup task, get project ID, check access
        boolean hasAccess = taskRepository.findById(taskId)
                .map(task -> {
                    Long projectId = task.getProject().getId();
                    boolean access = hasProjectAccess(projectId, userId);
                    log.debug("Task {} access check for user {}: project={}, hasAccess={}", 
                              taskId, userId, projectId, access);
                    return access;
                })
                .orElse(false);
        
        if (!hasAccess) {
            log.warn("User {} denied access to task {}", userId, taskId);
        }
        return hasAccess;
    }

    public void requireAccess(Project project, User user) {
        if (!hasAccess(project, user)) {
            throw new UnauthorizedProjectAccessException(project.getId(), user.getId());
        }
    }

    public void requireLeadOrAdmin(Project project, User user) {
        boolean isAdminUser = isAdmin(user);
        boolean isLead = isProjectLead(project, user);
        
        log.debug("Permission check for user {} on project {}: isAdmin={}, isProjectLead={}", 
                  user.getId(), project.getId(), isAdminUser, isLead);
        
        if (!isAdminUser && !isLead) {
            log.warn("Access denied: User {} is neither admin nor project lead for project {}", 
                     user.getId(), project.getId());
            throw new UnauthorizedProjectAccessException("Only project lead or admin can perform this action");
        }
    }

    public boolean hasAccess(Project project, User user) {
        return isAdmin(user) || 
               isProjectLead(project, user) || 
               projectMemberRepository.existsByProjectIdAndUserId(project.getId(), user.getId());
    }

    private boolean isProjectLead(Project project, User user) {
        if (project.getProjectLead() == null) {
            log.warn("Project {} has no project lead set", project.getId());
            return false;
        }
        boolean isLead = project.getProjectLead().getId().equals(user.getId());
        log.debug("Checking if user {} is project lead for project {}: {}", 
                  user.getId(), project.getId(), isLead);
        return isLead;
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getName()));
    }
}