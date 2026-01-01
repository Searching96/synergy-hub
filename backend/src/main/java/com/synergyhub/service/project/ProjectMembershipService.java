package com.synergyhub.service.project;

import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.ProjectMember;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.enums.ProjectRole;
import com.synergyhub.dto.response.ProjectMemberResponse;
import com.synergyhub.events.project.ProjectMemberAddedEvent;
import com.synergyhub.events.project.ProjectMemberRemovedEvent;
import com.synergyhub.events.project.ProjectMemberRoleUpdatedEvent;
import com.synergyhub.exception.InvalidProjectMemberException;
import com.synergyhub.exception.ResourceNotFoundException;
import com.synergyhub.repository.ProjectMemberRepository;
import com.synergyhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ProjectMembershipService {

    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void addMemberByEmail(Project project, String email, ProjectRole role, User actor, String ipAddress) {
        User memberUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        addMember(project, memberUser.getId(), role, actor, ipAddress);
    }

    @Transactional
    public void addMember(Project project, Integer userId, ProjectRole role, User actor, String ipAddress) {
        User memberUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!memberUser.getOrganization().getId().equals(project.getOrganization().getId())) {
            throw InvalidProjectMemberException.differentOrganization();
        }

        if (projectMemberRepository.existsByProjectIdAndUserId(project.getId(), userId)) {
            throw InvalidProjectMemberException.alreadyMember(memberUser.getEmail(), project.getName());
        }

        ProjectMember.ProjectMemberId id = new ProjectMember.ProjectMemberId(project.getId(), userId);
        ProjectMember projectMember = ProjectMember.builder()
            .id(id)
            .project(project)
            .user(memberUser)
            .role(role)
            .build();

        projectMemberRepository.save(projectMember);

        // ✅ FIXED: Remove 'this' from constructor
        eventPublisher.publishEvent(
            new ProjectMemberAddedEvent(project, memberUser, role, actor, ipAddress)
        );
    }

    @Transactional
    public void removeMember(Project project, Integer userId, User actor, String ipAddress) {
        User memberUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (project.getProjectLead().getId().equals(userId)) {
            throw InvalidProjectMemberException.cannotRemoveProjectLead();
        }

        if (!projectMemberRepository.existsByProjectIdAndUserId(project.getId(), userId)) {
            throw InvalidProjectMemberException.notAMember(memberUser.getEmail(), project.getName());
        }

        projectMemberRepository.deleteByProjectIdAndUserId(project.getId(), userId);

        // ✅ FIXED: Remove 'this' from constructor
        eventPublisher.publishEvent(
            new ProjectMemberRemovedEvent(project, userId, actor, ipAddress)
        );
    }

    @Transactional
    public void updateMemberRole(Project project, Integer userId, ProjectRole role, User actor, String ipAddress) {
        User memberUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        ProjectMember projectMember = projectMemberRepository.findByProjectIdAndUserId(project.getId(), userId)
                .orElseThrow(() -> InvalidProjectMemberException.notAMember(memberUser.getEmail(), project.getName()));

        projectMember.setRole(role);
        projectMemberRepository.save(projectMember);

        // ✅ FIXED: Remove 'this' from constructor
        eventPublisher.publishEvent(
            new ProjectMemberRoleUpdatedEvent(project, userId, role, actor, ipAddress)
        );
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getProjectMembers(Project project, 
                                                         Function<List<ProjectMember>, List<ProjectMemberResponse>> mapper) {
        List<ProjectMember> members = projectMemberRepository.findByProjectId(project.getId());
        return mapper.apply(members);
    }
}