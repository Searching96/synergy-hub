package com.synergyhub.service.project;

import com.synergyhub.domain.entity.*;
import com.synergyhub.dto.mapper.ProjectMapper;
import com.synergyhub.dto.mapper.UserMapper;
import com.synergyhub.dto.request.AddMemberRequest;
import com.synergyhub.dto.request.CreateProjectRequest;
import com.synergyhub.dto.request.UpdateProjectRequest;
import com.synergyhub.dto.response.ProjectDetailResponse;
import com.synergyhub.dto.response.ProjectMemberResponse;
import com.synergyhub.dto.response.ProjectResponse;
import com.synergyhub.exception.*;
import com.synergyhub.repository.ProjectMemberRepository;
import com.synergyhub.repository.ProjectRepository;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.service.security.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ProjectService projectService;

    private User testUser;
    private Organization testOrganization;
    private Project testProject;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
                .id(1)
                .name("Test Organization")
                .build();

        userRole = Role.builder()
                .id(1)
                .name("USER")
                .build();

        adminRole = Role.builder()
                .id(2)
                .name("ADMIN")
                .build();

        testUser = User.builder()
                .id(1)
                .email("test@example.com")
                .name("Test User")
                .organization(testOrganization)
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .build();

        testProject = Project.builder()
                .id(1)
                .name("Test Project")
                .description("Test Description")
                .organization(testOrganization)
                .projectLead(testUser)
                .status("ACTIVE")
                .projectMembers(new HashSet<>())
                .build();
    }

    @Test
    void createProject_WithValidData_ShouldReturnProjectResponse() {
        // Given
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("New Project")
                .description("New Description")
                .build();

        ProjectResponse expectedResponse = ProjectResponse.builder()
                .id(1)
                .name("New Project")
                .build();

        when(projectRepository.existsByNameAndOrganizationId(anyString(), anyInt())).thenReturn(false);
        when(projectMapper.toEntity(any(CreateProjectRequest.class))).thenReturn(testProject);
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(new ProjectMember());
        when(projectMapper.toProjectResponse(any(Project.class))).thenReturn(expectedResponse);
        doNothing().when(auditLogService).logProjectCreated(any(), any(), anyString());

        // When
        ProjectResponse result = projectService.createProject(request, testUser, "127.0.0.1");

        // Then
        assertNotNull(result);
        assertEquals("New Project", result.getName());
        verify(projectRepository).existsByNameAndOrganizationId("New Project", 1);
        verify(projectRepository).save(any(Project.class));
        verify(projectMemberRepository).save(any(ProjectMember.class)); // Project lead added
        verify(auditLogService).logProjectCreated(any(), eq(testUser), eq("127.0.0.1"));
    }

    @Test
    void getProjectDetails_AsMember_ShouldReturnProjectWithMembers() {
        // Given
        User member1 = User.builder()
                .id(2)
                .email("member1@example.com")
                .name("Member 1")
                .build();

        ProjectMember projectMember1 = ProjectMember.builder()
                .id(new ProjectMember.ProjectMemberId(1, 1))
                .project(testProject)
                .user(testUser)
                .role("PROJECT_LEAD")
                .build();

        ProjectMember projectMember2 = ProjectMember.builder()
                .id(new ProjectMember.ProjectMemberId(1, 2))
                .project(testProject)
                .user(member1)
                .role("DEVELOPER")
                .build();

        testProject.setProjectMembers(new HashSet<>(Arrays.asList(projectMember1, projectMember2)));

        ProjectMemberResponse memberResponse1 = ProjectMemberResponse.builder()
                .role("PROJECT_LEAD")
                .build();

        ProjectMemberResponse memberResponse2 = ProjectMemberResponse.builder()
                .role("DEVELOPER")
                .build();

        ProjectDetailResponse expectedResponse = ProjectDetailResponse.builder()
                .id(1)
                .name("Test Project")
                .members(Arrays.asList(memberResponse1, memberResponse2))
                .build();

        when(projectRepository.findByIdWithMembers(1)).thenReturn(Optional.of(testProject));
        // ❌ REMOVED: when(projectMemberRepository.existsByProjectIdAndUserId(1, 1)).thenReturn(true);
        when(projectMapper.toProjectDetailResponse(testProject)).thenReturn(expectedResponse);

        // When
        ProjectDetailResponse result = projectService.getProjectDetails(1, testUser);

        // Then
        assertNotNull(result);
        assertEquals("Test Project", result.getName());
        assertNotNull(result.getMembers());
        assertEquals(2, result.getMembers().size());
        verify(projectRepository).findByIdWithMembers(1);
        verify(projectMapper).toProjectDetailResponse(testProject);
    }


    @Test
    void getProjectDetails_AsNonMember_ShouldThrowException() {
        // Given
        User nonMember = User.builder()
                .id(2)
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .build();

        when(projectRepository.findByIdWithMembers(1)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.existsByProjectIdAndUserId(1, 2)).thenReturn(false);

        // When & Then
        assertThrows(UnauthorizedProjectAccessException.class, () -> {
            projectService.getProjectDetails(1, nonMember);
        });
    }

    @Test
    void getProjectMembers_AsMember_ShouldReturnAllMembers() {
        // Given
        User member1 = User.builder()
                .id(2)
                .email("member1@example.com")
                .build();

        ProjectMember projectMember1 = ProjectMember.builder()
                .id(new ProjectMember.ProjectMemberId(1, 1))
                .project(testProject)
                .user(testUser)
                .role("PROJECT_LEAD")
                .build();

        ProjectMember projectMember2 = ProjectMember.builder()
                .id(new ProjectMember.ProjectMemberId(1, 2))
                .project(testProject)
                .user(member1)
                .role("DEVELOPER")
                .build();

        ProjectMemberResponse memberResponse1 = ProjectMemberResponse.builder()
                .role("PROJECT_LEAD")
                .build();

        ProjectMemberResponse memberResponse2 = ProjectMemberResponse.builder()
                .role("DEVELOPER")
                .build();

        // Mock only what's actually called
        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.findByProjectId(1))
                .thenReturn(Arrays.asList(projectMember1, projectMember2));
        when(projectMapper.toProjectMemberResponseList(anyList()))
                .thenReturn(Arrays.asList(memberResponse1, memberResponse2));

        // When
        List<ProjectMemberResponse> result = projectService.getProjectMembers(1, testUser);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // Add these verification to see what's actually called:
        verify(projectRepository).findById(1);  // ADD THIS
        verify(projectMemberRepository).findByProjectId(1);
        verify(projectMapper).toProjectMemberResponseList(anyList());
    }

    @Test
    void createProject_WithDuplicateName_ShouldThrowException() {
        // Given
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("Existing Project")
                .build();

        when(projectRepository.existsByNameAndOrganizationId(anyString(), anyInt())).thenReturn(true);

        // When & Then
        assertThrows(ProjectNameAlreadyExistsException.class, () -> {
            projectService.createProject(request, testUser, "127.0.0.1");
        });

        verify(projectRepository, never()).save(any());
    }

    @Test
    void updateProject_AsProjectLead_ShouldUpdateSuccessfully() {
        // Given
        UpdateProjectRequest request = UpdateProjectRequest.builder()
                .name("Updated Project")
                .description("Updated Description")
                .build();

        ProjectResponse expectedResponse = ProjectResponse.builder()
                .id(1)
                .name("Updated Project")
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        when(projectMapper.toProjectResponse(any(Project.class))).thenReturn(expectedResponse);
        doNothing().when(projectMapper).updateEntityFromRequest(any(), any());
        doNothing().when(auditLogService).logProjectUpdated(any(), any(), anyString());

        // When
        ProjectResponse result = projectService.updateProject(1, request, testUser, "127.0.0.1");

        // Then
        assertNotNull(result);
        assertEquals("Updated Project", result.getName());
        verify(projectMapper).updateEntityFromRequest(request, testProject);
        verify(projectRepository).save(testProject);
        verify(auditLogService).logProjectUpdated(testProject, testUser, "127.0.0.1");
    }

    @Test
    void updateProject_AsNonProjectLead_ShouldThrowException() {
        // Given
        User otherUser = User.builder()
                .id(2)
                .email("other@example.com")
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .build();

        UpdateProjectRequest request = UpdateProjectRequest.builder()
                .name("Updated Project")
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));

        // When & Then
        assertThrows(UnauthorizedProjectAccessException.class, () -> {
            projectService.updateProject(1, request, otherUser, "127.0.0.1");
        });

        verify(projectRepository, never()).save(any());
    }

    @Test
    void updateProject_AsAdmin_ShouldUpdateSuccessfully() {
        // Given
        User adminUser = User.builder()
                .id(2)
                .email("admin@example.com")
                .roles(new HashSet<>(Collections.singletonList(adminRole)))
                .build();

        UpdateProjectRequest request = UpdateProjectRequest.builder()
                .name("Updated Project")
                .build();

        ProjectResponse expectedResponse = ProjectResponse.builder()
                .id(1)
                .name("Updated Project")
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        when(projectMapper.toProjectResponse(any(Project.class))).thenReturn(expectedResponse);
        doNothing().when(projectMapper).updateEntityFromRequest(any(), any());
        doNothing().when(auditLogService).logProjectUpdated(any(), any(), anyString());

        // When
        ProjectResponse result = projectService.updateProject(1, request, adminUser, "127.0.0.1");

        // Then
        assertNotNull(result);
        verify(projectRepository).save(testProject);
    }

    @Test
    void deleteProject_AsProjectLead_ShouldArchiveProject() {
        // Given
        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        doNothing().when(auditLogService).logProjectDeleted(any(), any(), anyString());

        // When
        projectService.deleteProject(1, testUser, "127.0.0.1");

        // Then
        assertEquals("ARCHIVED", testProject.getStatus());
        verify(projectRepository).save(testProject);
        verify(auditLogService).logProjectDeleted(testProject, testUser, "127.0.0.1");
    }

    @Test
    void addMemberToProject_WithValidUser_ShouldAddMember() {
        // Given
        User newMember = User.builder()
                .id(2)
                .email("member@example.com")
                .organization(testOrganization)
                .build();

        AddMemberRequest request = AddMemberRequest.builder()
                .userId(2)
                .role("DEVELOPER")
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(2)).thenReturn(Optional.of(newMember));
        when(projectMemberRepository.existsByProjectIdAndUserId(1, 2)).thenReturn(false);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(new ProjectMember());
        doNothing().when(auditLogService).logProjectMemberAdded(any(), anyInt(), any(), anyString());

        // When
        projectService.addMemberToProject(1, request, testUser, "127.0.0.1");

        // Then
        verify(projectMemberRepository).save(any(ProjectMember.class));
        verify(auditLogService).logProjectMemberAdded(testProject, 2, testUser, "127.0.0.1");
    }

    @Test
    void addMemberToProject_AlreadyMember_ShouldThrowException() {
        // Given
        AddMemberRequest request = AddMemberRequest.builder()
                .userId(2)
                .role("DEVELOPER")
                .build();

        User existingMember = User.builder()
                .id(2)
                .organization(testOrganization)
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(2)).thenReturn(Optional.of(existingMember));
        when(projectMemberRepository.existsByProjectIdAndUserId(1, 2)).thenReturn(true);

        // When & Then
        assertThrows(InvalidProjectMemberException.class, () -> {
            projectService.addMemberToProject(1, request, testUser, "127.0.0.1");
        });

        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void addMemberToProject_DifferentOrganization_ShouldThrowException() {
        // Given
        Organization otherOrg = Organization.builder()
                .id(2)
                .name("Other Organization")
                .build();

        User otherOrgUser = User.builder()
                .id(2)
                .organization(otherOrg)
                .build();

        AddMemberRequest request = AddMemberRequest.builder()
                .userId(2)
                .role("DEVELOPER")
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(2)).thenReturn(Optional.of(otherOrgUser));

        // When & Then
        assertThrows(InvalidProjectMemberException.class, () -> {
            projectService.addMemberToProject(1, request, testUser, "127.0.0.1");
        });
    }

    @Test
    void removeMemberFromProject_ShouldRemoveMember() {
        // Given
        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.existsByProjectIdAndUserId(1, 2)).thenReturn(true);
        doNothing().when(projectMemberRepository).deleteByProjectIdAndUserId(1, 2);
        doNothing().when(auditLogService).logProjectMemberRemoved(any(), anyInt(), any(), anyString());

        // When
        projectService.removeMemberFromProject(1, 2, testUser, "127.0.0.1");

        // Then
        verify(projectMemberRepository).deleteByProjectIdAndUserId(1, 2);
        verify(auditLogService).logProjectMemberRemoved(testProject, 2, testUser, "127.0.0.1");
    }

    @Test
    void removeMemberFromProject_RemovingProjectLead_ShouldThrowException() {
        // Given
        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));

        // When & Then
        assertThrows(InvalidProjectMemberException.class, () -> {
            projectService.removeMemberFromProject(1, 1, testUser, "127.0.0.1"); // testUser.id = 1 = projectLead.id
        });

        verify(projectMemberRepository, never()).deleteByProjectIdAndUserId(anyInt(), anyInt());
    }

    @Test
    void getProject_AsMember_ShouldReturnProject() {
        // Given
        ProjectResponse expectedResponse = ProjectResponse.builder()
                .id(1)
                .name("Test Project")
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectMapper.toProjectResponse(any(Project.class))).thenReturn(expectedResponse);

        // When
        ProjectResponse result = projectService.getProject(1, testUser);

        // Then
        assertNotNull(result);
        assertEquals("Test Project", result.getName());
        verify(projectRepository).findById(1);  // ✅ Verify it WAS called
        verify(projectMapper).toProjectResponse(any(Project.class));
    }

    @Test
    void getProject_AsNonMember_ShouldThrowException() {
        // Given
        User nonMember = User.builder()
                .id(2)
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.existsByProjectIdAndUserId(1, 2)).thenReturn(false);

        // When & Then
        assertThrows(UnauthorizedProjectAccessException.class, () -> {
            projectService.getProject(1, nonMember);
        });
    }

    @Test
    void getProject_AsAdmin_ShouldReturnProject() {
        // Given
        User adminUser = User.builder()
                .id(2)
                .roles(new HashSet<>(Collections.singletonList(adminRole)))
                .build();

        ProjectResponse expectedResponse = ProjectResponse.builder()
                .id(1)
                .name("Test Project")
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectMapper.toProjectResponse(testProject)).thenReturn(expectedResponse);

        // When
        ProjectResponse result = projectService.getProject(1, adminUser);

        // Then
        assertNotNull(result);
        verify(projectRepository).findById(1);
    }
}
