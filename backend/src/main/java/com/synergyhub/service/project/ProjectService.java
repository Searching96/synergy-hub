package com.synergyhub.service.project;

import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.mapper.ProjectMapper;
import com.synergyhub.dto.request.*;
import com.synergyhub.dto.response.*;
import com.synergyhub.exception.ProjectNotFoundException;
import com.synergyhub.repository.ProjectRepository;
import com.synergyhub.security.ProjectSecurity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final ProjectLifecycleService lifecycleService;
    private final ProjectMembershipService membershipService;
    private final ProjectSecurity projectSecurity;

    public ProjectResponse createProject(CreateProjectRequest request, User currentUser, String ipAddress) {
        log.info("Creating project: {}", request.getName());
        
        Project project = lifecycleService.createProject(request, currentUser, ipAddress);
        
        return projectMapper.toProjectResponse(project);
    }

    // ✅ FIXED: Added ipAddress parameter
    public ProjectResponse updateProject(Long projectId, UpdateProjectRequest request, 
                                        User currentUser, String ipAddress) {
        Project project = getProjectById(projectId);
        projectSecurity.requireLeadOrAdmin(project, currentUser);
        
        // ✅ FIXED: Pass ipAddress to lifecycle service
        Project updated = lifecycleService.updateProject(project, request, currentUser, ipAddress);
        return projectMapper.toProjectResponse(updated);
    }

    // ✅ FIXED: Added ipAddress parameter
    public void deleteProject(Long projectId, User currentUser, String ipAddress) {
        Project project = getProjectById(projectId);
        projectSecurity.requireLeadOrAdmin(project, currentUser);
        
        // ✅ Permanently delete the project
        lifecycleService.deleteProjectPermanently(project, currentUser, ipAddress);
    }

    public void archiveProject(Long projectId, User currentUser, String ipAddress) {
        Project project = getProjectById(projectId);
        projectSecurity.requireLeadOrAdmin(project, currentUser);
        
        lifecycleService.archiveProject(project, currentUser, ipAddress);
    }

    public void unarchiveProject(Long projectId, User currentUser, String ipAddress) {
        Project project = getProjectById(projectId);
        projectSecurity.requireLeadOrAdmin(project, currentUser);
        
        lifecycleService.unarchiveProject(project, currentUser, ipAddress);
    }

    public void addMember(Long projectId, AddMemberRequest request, User currentUser, String ipAddress) {
        Project project = getProjectById(projectId);
        projectSecurity.requireLeadOrAdmin(project, currentUser);
        membershipService.addMemberByEmail(project, request.getEmail(), request.getRole(), currentUser, ipAddress);
    }

    public void removeMember(Long projectId, Long userId, User currentUser, String ipAddress) {
        Project project = getProjectById(projectId);
        projectSecurity.requireLeadOrAdmin(project, currentUser);
        membershipService.removeMember(project, userId, currentUser, ipAddress);
    }

    public void updateMemberRole(Long projectId, Long userId, UpdateMemberRoleRequest request, 
                                User currentUser, String ipAddress) {
        Project project = getProjectById(projectId);
        projectSecurity.requireLeadOrAdmin(project, currentUser);
        membershipService.updateMemberRole(project, userId, request.getRole(), currentUser, ipAddress);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long projectId, User currentUser) {
        Project project = getProjectById(projectId);
        projectSecurity.requireAccess(project, currentUser);
        return projectMapper.toProjectResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProjectDetails(Long projectId, User currentUser) {
        Project project = projectRepository.findByIdWithMembers(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        projectSecurity.requireAccess(project, currentUser);
        return projectMapper.toProjectDetailResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getUserProjects(User currentUser) {
        List<Project> projects = projectRepository.findProjectsForUser(currentUser.getId());
        return projectMapper.toProjectResponseList(projects);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ProjectResponse> getProjects(
            User currentUser, String search, String status, int page, int size) {
        
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by("updatedAt").descending());
                
        org.springframework.data.domain.Page<Project> projectPage = 
            projectRepository.findProjectsForUserWithFilter(currentUser.getId(), search, status, pageable);
            
        return projectPage.map(projectMapper::toProjectResponse);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getProjectMembers(Long projectId, User currentUser) {
        Project project = getProjectById(projectId);
        projectSecurity.requireAccess(project, currentUser);
        return membershipService.getProjectMembers(project, projectMapper::toProjectMemberResponseList);
    }

    private Project getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }
}