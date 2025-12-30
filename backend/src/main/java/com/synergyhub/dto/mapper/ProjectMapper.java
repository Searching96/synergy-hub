package com.synergyhub.dto.mapper;

import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.ProjectMember;
import com.synergyhub.dto.request.CreateProjectRequest;
import com.synergyhub.dto.request.UpdateProjectRequest;
import com.synergyhub.dto.response.ProjectDetailResponse;
import com.synergyhub.dto.response.ProjectMemberResponse;
import com.synergyhub.dto.response.ProjectResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface ProjectMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "projectLead", source = "projectLead")
    @Mapping(target = "memberCount", expression = "java(project.getProjectMembers() != null ? project.getProjectMembers().size() : 0)")
    ProjectResponse toProjectResponse(Project project);

    List<ProjectResponse> toProjectResponseList(List<Project> projects);

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "projectLead", source = "projectLead")
    @Mapping(target = "memberCount", expression = "java(project.getProjectMembers() != null ? project.getProjectMembers().size() : 0)")
    @Mapping(target = "members", source = "projectMembers")
    ProjectDetailResponse toProjectDetailResponse(Project project);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "projectLead", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "projectMembers", ignore = true)
    @Mapping(target = "sprints", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    Project toEntity(CreateProjectRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "projectLead", ignore = true)
    @Mapping(target = "projectMembers", ignore = true)
    @Mapping(target = "sprints", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    void updateEntityFromRequest(UpdateProjectRequest request, @MappingTarget Project project);

    @Mapping(target = "user", source = "user")
    @Mapping(target = "role", source = "role")
    ProjectMemberResponse toProjectMemberResponse(ProjectMember projectMember);

    List<ProjectMemberResponse> toProjectMemberResponseList(List<ProjectMember> projectMembers);
}
