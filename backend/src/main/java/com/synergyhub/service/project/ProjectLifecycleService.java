package com.synergyhub.service.project;

import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.enums.ProjectRole;
import com.synergyhub.domain.enums.ProjectStatus;
import com.synergyhub.dto.request.CreateProjectRequest;
import com.synergyhub.dto.request.UpdateProjectRequest;
import com.synergyhub.events.project.ProjectArchivedEvent;
import com.synergyhub.events.project.ProjectCreatedEvent;
import com.synergyhub.events.project.ProjectUpdatedEvent;
import com.synergyhub.exception.ProjectNameAlreadyExistsException;
import com.synergyhub.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectLifecycleService {

    private final ProjectRepository projectRepository;
    private final ProjectMembershipService membershipService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Project createProject(CreateProjectRequest request, User owner, String ipAddress) {
        if (projectRepository.existsByNameAndOrganizationId(request.getName(), owner.getOrganization().getId())) {
            throw new ProjectNameAlreadyExistsException(request.getName(), owner.getOrganization().getId());
        }

        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOrganization(owner.getOrganization());
        project.setProjectLead(owner);
        project.setStatus(ProjectStatus.ACTIVE);

        Project savedProject = projectRepository.save(project);

        // Add owner as lead member
        membershipService.addMember(savedProject, owner.getId(), ProjectRole.PROJECT_LEAD, owner, ipAddress);

        // Add initial members if provided in DTO
        if (request.getMembers() != null) {
            request.getMembers().forEach(m -> 
                membershipService.addMember(savedProject, m.getUserId(), m.getRole(), owner, ipAddress)
            );
        }

        // ✅ FIXED: Remove 'this' from constructor
        eventPublisher.publishEvent(new ProjectCreatedEvent(savedProject, owner, ipAddress));
        return savedProject;
    }

    @Transactional
    public Project updateProject(Project project, UpdateProjectRequest request, User actor, String ipAddress) {
        if (request.getName() != null) project.setName(request.getName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getStatus() != null) project.setStatus(request.getStatus());

        Project updated = projectRepository.save(project);
        
        // ✅ FIXED: Remove 'this' from constructor
        eventPublisher.publishEvent(new ProjectUpdatedEvent(updated, actor, ipAddress));
        return updated;
    }

    @Transactional
    public Project archiveProject(Project project, User actor, String ipAddress) {
        project.setStatus(ProjectStatus.ARCHIVED);
        Project archived = projectRepository.save(project);
        
        // ✅ FIXED: Remove 'this' from constructor
        eventPublisher.publishEvent(new ProjectArchivedEvent(archived, actor, ipAddress));
        return archived;
    }
}