package com.synergyhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synergyhub.config.TestSecurityConfig;
import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.AddMemberRequest;
import com.synergyhub.dto.request.CreateProjectRequest;
import com.synergyhub.dto.request.UpdateMemberRoleRequest;
import com.synergyhub.dto.request.UpdateProjectRequest;
import com.synergyhub.dto.response.*;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.project.ProjectService;
import com.synergyhub.service.sprint.SprintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private SprintService sprintService;

    @MockBean
    private UserRepository userRepository;

    private User testUser;
    private UserPrincipal userPrincipal;
    private ProjectResponse projectResponse;
    private ProjectDetailResponse projectDetailResponse;
    private UserResponse projectLeadResponse;

    @BeforeEach
    void setUp() {
        Organization testOrg = Organization.builder()
                .id(1)
                .name("Test Org")
                .build();

        testUser = User.builder()
                .id(1)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashed")
                .organization(testOrg)
                .emailVerified(true)
                .build();

        userPrincipal = UserPrincipal.create(testUser);

        // ✅ Create UserResponse for projectLead
        projectLeadResponse = UserResponse.builder()
                .id(1)
                .name("Test User")
                .email("test@example.com")
                .build();

        projectResponse = ProjectResponse.builder()
                .id(1)
                .name("Test Project")
                .description("Test description")
                .organizationId(1)
                .organizationName("Test Org")
                .projectLead(projectLeadResponse)  // ✅ Use UserResponse object
                .status("ACTIVE")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .memberCount(1)
                .build();

        projectDetailResponse = ProjectDetailResponse.builder()
                .id(1)
                .name("Test Project")
                .description("Test description")
                .projectLead(projectLeadResponse)  // ✅ Use UserResponse object
                .status("ACTIVE")
                .memberCount(1)
                .build();

        // Mock user repository
        when(userRepository.findByEmailWithRolesAndPermissions(anyString()))
                .thenReturn(Optional.of(testUser));
    }

    // ========== CREATE PROJECT TESTS ==========

    @Test
    @WithMockUser
    void createProject_WithValidData_ShouldReturn201() throws Exception {
        // Given
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("New Project")
                .description("Project description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .build();

        when(projectService.createProject(any(), any(), anyString()))
                .thenReturn(projectResponse);

        // When & Then
        mockMvc.perform(post("/api/projects")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Project created successfully"))
                .andExpect(jsonPath("$.data.name").value("Test Project"))
                .andExpect(jsonPath("$.data.projectLead.id").value(1))  // ✅ Updated path
                .andExpect(jsonPath("$.data.projectLead.name").value("Test User"))  // ✅ Updated path
                .andExpect(jsonPath("$.data.organizationId").value(1))
                .andExpect(jsonPath("$.data.organizationName").value("Test Org"));

        verify(projectService).createProject(any(), eq(testUser), anyString());
    }

    @Test
    @WithMockUser
    void createProject_WithInvalidData_ShouldReturn400() throws Exception {
        // Given - Empty name
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("")
                .description("Description")
                .build();

        // When & Then
        mockMvc.perform(post("/api/projects")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(projectService, never()).createProject(any(), any(), anyString());
    }

    // ========== GET PROJECTS TESTS ==========

    @Test
    @WithMockUser
    void getUserProjects_ShouldReturnListOfProjects() throws Exception {
        // Given
        List<ProjectResponse> projects = Arrays.asList(
                projectResponse,
                ProjectResponse.builder()
                        .id(2)
                        .name("Project 2")
                        .projectLead(projectLeadResponse)
                        .build()
        );

        when(projectService.getUserProjects(testUser))
                .thenReturn(projects);

        // When & Then
        mockMvc.perform(get("/api/projects")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("Test Project"))
                .andExpect(jsonPath("$.data[1].name").value("Project 2"));

        verify(projectService).getUserProjects(testUser);
    }

    @Test
    @WithMockUser
    void getProjectDetails_WithValidId_ShouldReturn200() throws Exception {
        // Given
        when(projectService.getProjectDetails(1, testUser))
                .thenReturn(projectDetailResponse);

        // When & Then
        mockMvc.perform(get("/api/projects/1")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Test Project"))
                .andExpect(jsonPath("$.data.projectLead.id").value(1))  // ✅ Updated path
                .andExpect(jsonPath("$.data.projectLead.name").value("Test User"));  // ✅ Updated path

        verify(projectService).getProjectDetails(1, testUser);
    }

    // ========== UPDATE PROJECT TESTS ==========

    @Test
    @WithMockUser
    void updateProject_WithValidData_ShouldReturn200() throws Exception {
        // Given
        UpdateProjectRequest request = UpdateProjectRequest.builder()
                .name("Updated Project")
                .description("Updated description")
                .build();

        ProjectResponse updatedResponse = ProjectResponse.builder()
                .id(1)
                .name("Updated Project")
                .description("Updated description")
                .projectLead(projectLeadResponse)
                .build();

        when(projectService.updateProject(eq(1), any(), eq(testUser), anyString()))
                .thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/api/projects/1")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Project"));

        verify(projectService).updateProject(eq(1), any(), eq(testUser), anyString());
    }

    // ========== DELETE PROJECT TESTS ==========

    @Test
    @WithMockUser
    void deleteProject_WithValidId_ShouldReturn204() throws Exception {
        // Given
        doNothing().when(projectService).deleteProject(1, testUser, "127.0.0.1");

        // When & Then
        mockMvc.perform(delete("/api/projects/1")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(projectService).deleteProject(eq(1), eq(testUser), anyString());
    }

    // ========== MEMBER MANAGEMENT TESTS ==========

    @Test
    @WithMockUser
    void addMember_WithValidData_ShouldReturn200() throws Exception {
        // Given
        AddMemberRequest request = AddMemberRequest.builder()
                .userId(2)
                .role("DEVELOPER")
                .build();

        doNothing().when(projectService).addMemberToProject(eq(1), any(), eq(testUser), anyString());

        // When & Then
        mockMvc.perform(post("/api/projects/1/members")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Member added successfully"));

        verify(projectService).addMemberToProject(eq(1), any(), eq(testUser), anyString());
    }

    @Test
    @WithMockUser
    void removeMember_WithValidId_ShouldReturn204() throws Exception {
        // Given
        doNothing().when(projectService).removeMemberFromProject(1, 2, testUser, "127.0.0.1");

        // When & Then
        mockMvc.perform(delete("/api/projects/1/members/2")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(projectService).removeMemberFromProject(eq(1), eq(2), eq(testUser), anyString());
    }

    @Test
    @WithMockUser
    void updateMemberRole_WithValidData_ShouldReturn200() throws Exception {
        // Given
        UpdateMemberRoleRequest request = UpdateMemberRoleRequest.builder()
                .role("PROJECT_MANAGER")
                .build();

        doNothing().when(projectService).updateMemberRole(eq(1), eq(2), any(), eq(testUser), anyString());

        // When & Then
        mockMvc.perform(put("/api/projects/1/members/2/role")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(projectService).updateMemberRole(eq(1), eq(2), any(), eq(testUser), anyString());
    }

    @Test
    @WithMockUser
    void getProjectMembers_ShouldReturnListOfMembers() throws Exception {
        // Given
        UserResponse user1 = UserResponse.builder()
                .id(1)
                .name("User 1")
                .email("user1@example.com")
                .build();

        UserResponse user2 = UserResponse.builder()
                .id(2)
                .name("User 2")
                .email("user2@example.com")
                .build();

        List<ProjectMemberResponse> members = Arrays.asList(
                ProjectMemberResponse.builder()
                        .user(user1)  // ✅ Use UserResponse object
                        .role("PROJECT_LEAD")
                        .build(),
                ProjectMemberResponse.builder()
                        .user(user2)  // ✅ Use UserResponse object
                        .role("DEVELOPER")
                        .build()
        );

        when(projectService.getProjectMembers(1, testUser))
                .thenReturn(members);

        // When & Then
        mockMvc.perform(get("/api/projects/1/members")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].user.id").value(1))  // ✅ Updated path
                .andExpect(jsonPath("$.data[0].user.name").value("User 1"))  // ✅ Updated path
                .andExpect(jsonPath("$.data[0].role").value("PROJECT_LEAD"))
                .andExpect(jsonPath("$.data[1].user.id").value(2))  // ✅ Updated path
                .andExpect(jsonPath("$.data[1].user.name").value("User 2"))  // ✅ Updated path
                .andExpect(jsonPath("$.data[1].role").value("DEVELOPER"));

        verify(projectService).getProjectMembers(1, testUser);
    }

    // ========== SPRINT ENDPOINT TESTS ==========

    @Test
    @WithMockUser
    void getProjectSprints_ShouldReturnListOfSprints() throws Exception {
        // Given
        List<SprintResponse> sprints = Arrays.asList(
                SprintResponse.builder().id(1).name("Sprint 1").build(),
                SprintResponse.builder().id(2).name("Sprint 2").build()
        );

        when(sprintService.getSprintsByProject(1, testUser))
                .thenReturn(sprints);

        // When & Then
        mockMvc.perform(get("/api/projects/1/sprints")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("Sprint 1"))
                .andExpect(jsonPath("$.data[1].name").value("Sprint 2"));

        verify(sprintService).getSprintsByProject(1, testUser);
    }
}