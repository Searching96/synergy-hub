package com.synergyhub.service.project;

import com.synergyhub.domain.entity.*;
import com.synergyhub.dto.mapper.ProjectMapper;
import com.synergyhub.dto.mapper.UserMapper;
import com.synergyhub.dto.request.AddMemberRequest;
import com.synergyhub.dto.request.CreateProjectRequest;
import com.synergyhub.dto.request.UpdateMemberRoleRequest;
import com.synergyhub.dto.request.UpdateProjectRequest;
import com.synergyhub.dto.response.ProjectDetailResponse;
import com.synergyhub.dto.response.ProjectMemberResponse;
import com.synergyhub.dto.response.ProjectResponse;
import com.synergyhub.exception.*;
import com.synergyhub.repository.ProjectMemberRepository;
import com.synergyhub.repository.ProjectRepository;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.service.security.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    /**
     * Create a new project
     */
    public ProjectResponse createProject(CreateProjectRequest request, User currentUser, String ipAddress) {
        log.info("Creating project: {} for organization: {}", request.getName(), currentUser.getOrganization().getId());

        // Validate project name uniqueness within organization
        if (projectRepository.existsByNameAndOrganizationId(request.getName(), currentUser.getOrganization().getId())) {
            throw new ProjectNameAlreadyExistsException(request.getName(), currentUser.getOrganization().getId());
        }

        // Create project entity
        Project project = projectMapper.toEntity(request);
        project.setOrganization(currentUser.getOrganization());
        project.setProjectLead(currentUser);
        project.setStatus("ACTIVE");

        // Save project
        Project savedProject = projectRepository.save(project);

        // Add project lead as member with "PROJECT_LEAD" role
        addProjectLeadAsMember(savedProject, currentUser);

        // Add additional members if provided
        if (request.getMembers() != null && !request.getMembers().isEmpty()) {
            for (CreateProjectRequest.MemberWithRole memberRequest : request.getMembers()) {
                addMemberInternal(savedProject, memberRequest.getUserId(), memberRequest.getRole(), currentUser);
            }
        }

        // Log project creation
        auditLogService.logProjectCreated(savedProject, currentUser, ipAddress);

        log.info("Project created successfully with ID: {}", savedProject.getId());
        return projectMapper.toProjectResponse(savedProject);
    }

    /**
     * Update an existing project
     */
    public ProjectResponse updateProject(Integer projectId, UpdateProjectRequest request, User currentUser, String ipAddress) {
        log.info("Updating project: {} by user: {}", projectId, currentUser.getId());

        Project project = getProjectById(projectId);

        // Verify user is project lead or admin
        verifyProjectLeadOrAdmin(project, currentUser);

        // Update project fields
        projectMapper.updateEntityFromRequest(request, project);

        // Save updated project
        Project updatedProject = projectRepository.save(project);

        // Log project update
        auditLogService.logProjectUpdated(updatedProject, currentUser, ipAddress);

        log.info("Project updated successfully: {}", projectId);
        return projectMapper.toProjectResponse(updatedProject);
    }

    /**
     * Delete (archive) a project
     */
    public void deleteProject(Integer projectId, User currentUser, String ipAddress) {
        log.info("Deleting project: {} by user: {}", projectId, currentUser.getId());

        Project project = getProjectById(projectId);

        // Verify user is project lead or admin
        verifyProjectLeadOrAdmin(project, currentUser);

        // Soft delete by setting status to ARCHIVED
        project.setStatus("ARCHIVED");
        projectRepository.save(project);

        // Log project deletion
        auditLogService.logProjectDeleted(project, currentUser, ipAddress);

        log.info("Project archived successfully: {}", projectId);
    }

    /**
     * Get project basic info
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProject(Integer projectId, User currentUser) {
        log.info("Getting project: {} for user: {}", projectId, currentUser.getId());

        Project project = getProjectById(projectId);

        // Verify user has access (is member or admin)
        verifyProjectAccess(project, currentUser);

        return projectMapper.toProjectResponse(project);
    }

    /**
     * Get project with full details including members
     */
    @Transactional(readOnly = true)
    public ProjectDetailResponse getProjectDetails(Integer projectId, User currentUser) {
        log.info("Getting project details: {} for user: {}", projectId, currentUser.getId());

        Project project = projectRepository.findByIdWithMembers(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        // Verify user has access
        verifyProjectAccess(project, currentUser);

        return projectMapper.toProjectDetailResponse(project);
    }

    /**
     * Get all projects where user is project lead or member
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getUserProjects(User currentUser) {
        log.info("Getting projects for user: {}", currentUser.getId());

        // Get projects where user is lead
        List<Project> leadProjects = projectRepository.findByProjectLeadId(currentUser.getId());

        // Get projects where user is member
        List<ProjectMember> memberProjects = projectMemberRepository.findByUserId(currentUser.getId());
        List<Project> memberProjectsList = memberProjects.stream()
                .map(ProjectMember::getProject)
                .filter(p -> !leadProjects.contains(p)) // Avoid duplicates
                .toList();

        // Combine both lists
        List<Project> allProjects = new java.util.ArrayList<>(leadProjects);
        allProjects.addAll(memberProjectsList);

        return allProjects.stream()
                .map(projectMapper::toProjectResponse)
                .collect(Collectors.toList());
    }

    /**
     * Add a member to the project
     */
    public void addMemberToProject(Integer projectId, AddMemberRequest request, User currentUser, String ipAddress) {
        log.info("Adding member {} to project: {}", request.getUserId(), projectId);

        Project project = getProjectById(projectId);

        // Verify user is project lead or admin
        verifyProjectLeadOrAdmin(project, currentUser);

        addMemberInternal(project, request.getUserId(), request.getRole(), currentUser);

        // Log member addition
        auditLogService.logProjectMemberAdded(project, request.getUserId(), currentUser, ipAddress);

        log.info("Member added successfully to project: {}", projectId);
    }

    /**
     * Remove a member from the project
     */
    public void removeMemberFromProject(Integer projectId, Integer userId, User currentUser, String ipAddress) {
        log.info("Removing member {} from project: {}", userId, projectId);

        Project project = getProjectById(projectId);

        // Verify user is project lead or admin
        verifyProjectLeadOrAdmin(project, currentUser);

        // Cannot remove project lead
        if (project.getProjectLead().getId().equals(userId)) {
            throw InvalidProjectMemberException.cannotRemoveProjectLead();
        }

        // Check if user is member
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw InvalidProjectMemberException.notAMember(userId, projectId);
        }

        // Remove member
        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);

        // Log member removal
        auditLogService.logProjectMemberRemoved(project, userId, currentUser, ipAddress);

        log.info("Member removed successfully from project: {}", projectId);
    }

    /**
     * Update member role in project
     */
    public void updateMemberRole(Integer projectId, Integer userId, UpdateMemberRoleRequest request,
                                 User currentUser, String ipAddress) {
        log.info("Updating role for member {} in project: {}", userId, projectId);

        Project project = getProjectById(projectId);

        // Verify user is project lead or admin
        verifyProjectLeadOrAdmin(project, currentUser);

        // Find member
        ProjectMember projectMember = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> InvalidProjectMemberException.notAMember(userId, projectId));

        // Update role
        projectMember.setRole(request.getRole());
        projectMemberRepository.save(projectMember);

        // Log role update
        auditLogService.logProjectMemberRoleUpdated(project, userId, request.getRole(), currentUser, ipAddress);

        log.info("Member role updated successfully in project: {}", projectId);
    }

    /**
     * Get all members of a project
     */
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getProjectMembers(Integer projectId, User currentUser) {
        log.info("Getting members for project: {}", projectId);

        Project project = getProjectById(projectId);

        // Verify user has access
        verifyProjectAccess(project, currentUser);

        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        return projectMapper.toProjectMemberResponseList(members);
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    private Project getProjectById(Integer projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private void verifyProjectAccess(Project project, User user) {
        // Admin has access to all projects
        if (isAdmin(user)) {
            return;
        }

        // Check if user is project lead
        if (project.getProjectLead().getId().equals(user.getId())) {
            return;
        }

        // Check if user is member
        if (!projectMemberRepository.existsByProjectIdAndUserId(project.getId(), user.getId())) {
            throw new UnauthorizedProjectAccessException(project.getId(), user.getId());
        }
    }

    private void verifyProjectLeadOrAdmin(Project project, User user) {
        if (isAdmin(user) || project.getProjectLead().getId().equals(user.getId())) {
            return;
        }
        throw new UnauthorizedProjectAccessException(
                "Only project lead or admin can perform this action"
        );
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getName()));
    }

    private void addProjectLeadAsMember(Project project, User projectLead) {
        ProjectMember.ProjectMemberId id = new ProjectMember.ProjectMemberId(
                project.getId(), projectLead.getId()
        );

        ProjectMember projectMember = ProjectMember.builder()
                .id(id)
                .project(project)
                .user(projectLead)
                .role("PROJECT_LEAD")
                .build();

        projectMemberRepository.save(projectMember);
    }

    private void addMemberInternal(Project project, Integer userId, String role, User currentUser) {
        // Verify user exists
        User memberUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        // Verify user is in same organization
        if (!memberUser.getOrganization().getId().equals(project.getOrganization().getId())) {
            throw InvalidProjectMemberException.differentOrganization();
        }

        // Check if already a member
        if (projectMemberRepository.existsByProjectIdAndUserId(project.getId(), userId)) {
            throw InvalidProjectMemberException.alreadyMember(userId, project.getId());
        }

        // Add member
        ProjectMember.ProjectMemberId id = new ProjectMember.ProjectMemberId(
                project.getId(), userId
        );

        ProjectMember projectMember = ProjectMember.builder()
                .id(id)
                .project(project)
                .user(memberUser)
                .role(role)
                .build();

        projectMemberRepository.save(projectMember);
    }
}
