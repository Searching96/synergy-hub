package com.synergyhub.dto.mapper;

import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.ProjectMember;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.CreateProjectRequest;
import com.synergyhub.dto.request.UpdateProjectRequest;
import com.synergyhub.dto.response.ProjectDetailResponse;
import com.synergyhub.dto.response.ProjectLeadResponse;
import com.synergyhub.dto.response.ProjectMemberResponse;
import com.synergyhub.dto.response.ProjectResponse;
import com.synergyhub.dto.response.UserResponse;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public abstract class ProjectMapper {

    @Autowired
    protected UserMapper userMapper;

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "projectLead", source = ".", qualifiedByName = "projectToProjectLeadResponse")
    @Mapping(target = "memberCount", expression = "java(project.getProjectMembers() != null ? project.getProjectMembers().size() : 0)")
    @Mapping(target = "taskCount", expression = "java(project.getTasks() != null ? project.getTasks().size() : 0)")
    @Mapping(target = "completedTaskCount", expression = "java(project.getTasks() != null ? (int) project.getTasks().stream().filter(task -> \"DONE\".equals(task.getStatus())).count() : 0)")
    @Mapping(target = "teamId", source = "team.id")
    @Mapping(target = "teamName", source = "team.name")
    public abstract ProjectResponse toProjectResponse(Project project);

    public abstract List<ProjectResponse> toProjectResponseList(List<Project> projects);

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "projectLead", source = ".", qualifiedByName = "projectToProjectLeadResponse")
    @Mapping(target = "memberCount", expression = "java(project.getProjectMembers() != null ? project.getProjectMembers().size() : 0)")
    @Mapping(target = "members", source = "projectMembers")
    public abstract ProjectDetailResponse toProjectDetailResponse(Project project);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "projectLead", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "projectMembers", ignore = true)
    @Mapping(target = "sprints", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    public abstract Project toEntity(CreateProjectRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "projectLead", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "projectMembers", ignore = true)
    @Mapping(target = "sprints", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    public abstract void updateEntityFromRequest(UpdateProjectRequest request, @MappingTarget Project project);

    // Custom mapping to flatten user fields
    ProjectMemberResponse toProjectMemberResponse(ProjectMember projectMember) {
        if (projectMember == null || projectMember.getUser() == null) {
            return null;
        }
        return ProjectMemberResponse.builder()
                .userId(projectMember.getUser().getId())
                .name(projectMember.getUser().getName())
                .email(projectMember.getUser().getEmail())
                .role(projectMember.getRole())
                .build();
    }

    public abstract List<ProjectMemberResponse> toProjectMemberResponseList(List<ProjectMember> projectMembers);

    // Helper method to map project lead with role
    @Named("projectToProjectLeadResponse")
    protected ProjectLeadResponse projectToProjectLeadResponse(Project project) {
        if (project == null || project.getProjectLead() == null) {
            return null;
        }
        
        // Find the project member for the lead
        ProjectMember leadMember = project.getProjectMembers().stream()
                .filter(pm -> pm.getUser().getId().equals(project.getProjectLead().getId()))
                .findFirst()
                .orElse(null);
        
        User projectLead = project.getProjectLead();
        UserResponse userResponse = userMapper.toUserResponse(projectLead);
        
        return ProjectLeadResponse.builder()
                .user(userResponse)
                .role(leadMember != null ? leadMember.getRole() : null)
                .build();
    }
}

