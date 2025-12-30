package com.synergyhub.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synergyhub.domain.entity.*;
import com.synergyhub.dto.request.AddMemberRequest;
import com.synergyhub.dto.request.CreateProjectRequest;
import com.synergyhub.dto.request.UpdateProjectRequest;
import com.synergyhub.repository.*;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.util.EmailService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProjectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @MockBean
    private EmailService emailService;

    private User testUser;
    private User otherUser;
    private Organization testOrganization;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        // Clean up
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // Create organization
        testOrganization = Organization.builder()
                .name("Test Organization")
                .build();
        testOrganization = organizationRepository.save(testOrganization);

        // Create role
        Role userRole = Role.builder()
                .name("USER")
                .build();
        userRole = roleRepository.save(userRole);

        // Create test user - ❌ REMOVE .id(1) - let DB generate it
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))  // ✅ Fixed
                .name("Test User")
                .organization(testOrganization)
                .roles(new HashSet<>(List.of(userRole)))
                .emailVerified(true)
                .accountLocked(false)
                .twoFactorEnabled(false)
                .failedLoginAttempts(0)
                .build();
        testUser = userRepository.save(testUser);  // ✅ Save and get generated ID

        // Create other user
        otherUser = User.builder()
                .email("other@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .name("Other User")
                .organization(testOrganization)
                .roles(new HashSet<>(List.of(userRole)))
                .emailVerified(true)
                .accountLocked(false)
                .twoFactorEnabled(false)
                .failedLoginAttempts(0)
                .build();
        otherUser = userRepository.save(otherUser);

        userPrincipal = UserPrincipal.create(testUser);
    }

    // ... rest of the test methods remain the same ...

    @Test
    void createProject_WithValidData_ShouldReturn201() throws Exception {
        // Given
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("New Project")
                .description("New project description")
                .build();

        // When & Then
        mockMvc.perform(post("/api/projects")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Project created successfully"))
                .andExpect(jsonPath("$.data.name").value("New Project"))
                .andExpect(jsonPath("$.data.description").value("New project description"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // Verify project is created in database
        Project savedProject = projectRepository.findByNameAndOrganizationId("New Project", testOrganization.getId())
                .orElseThrow();
        assert savedProject.getName().equals("New Project");
        assert savedProject.getProjectLead().getId().equals(testUser.getId());

        // Verify project lead is added as member
        boolean isLeadMember = projectMemberRepository.existsByProjectIdAndUserId(
                savedProject.getId(), testUser.getId()
        );
        assert isLeadMember;
    }

    @Test
    void createProject_WithDuplicateName_ShouldReturn409() throws Exception {
        // Given
        Project existingProject = Project.builder()
                .name("Existing Project")
                .organization(testOrganization)
                .projectLead(testUser)
                .status("ACTIVE")
                .build();
        projectRepository.save(existingProject);

        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("Existing Project")
                .build();

        // When & Then
        mockMvc.perform(post("/api/projects")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createProject_Unauthenticated_ShouldReturn401() throws Exception {
        // Given
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("New Project")
                .build();

        // When & Then
        mockMvc.perform(post("/api/projects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserProjects_ShouldReturnUserProjects() throws Exception {
        // Given
        Project project1 = Project.builder()
                .name("Project 1")
                .organization(testOrganization)
                .projectLead(testUser)
                .status("ACTIVE")
                .build();
        project1 = projectRepository.save(project1);

        Project project2 = Project.builder()
                .name("Project 2")
                .organization(testOrganization)
                .projectLead(testUser)
                .status("ACTIVE")
                .build();
        project2 = projectRepository.save(project2);

        // Add testUser as member to both projects
        addMemberToProject(project1, testUser, "PROJECT_LEAD");
        addMemberToProject(project2, testUser, "PROJECT_LEAD");

        // When & Then
        mockMvc.perform(get("/api/projects")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].name", containsInAnyOrder("Project 1", "Project 2")));
    }

    @Test
    void getProjectDetails_AsMember_ShouldReturn200() throws Exception {
        // Given
        Project project = createTestProject("Test Project");
        addMemberToProject(project, testUser, "PROJECT_LEAD");
        addMemberToProject(project, otherUser, "DEVELOPER");

        entityManager.flush();
        entityManager.clear();

        // When & Then
        mockMvc.perform(get("/api/projects/" + project.getId())
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Project"))
                .andExpect(jsonPath("$.data.members", hasSize(2)))
                .andDo(result -> {
                    // ✅ Debug: Print response
                    System.out.println("\n=== DEBUG: API Response ===");
                    System.out.println(result.getResponse().getContentAsString());
                    System.out.println("===========================\n");
                });
    }


    @Test
    void getProjectDetails_AsNonMember_ShouldReturn403() throws Exception {
        // Given - Create project with otherUser as lead
        Project project = Project.builder()
                .name("Test Project")
                .description("Test description")
                .organization(testOrganization)
                .projectLead(otherUser)  // ✅ otherUser is lead, not testUser
                .status("ACTIVE")
                .build();
        project = projectRepository.save(project);

        addMemberToProject(project, otherUser, "PROJECT_LEAD");

        // When & Then - testUser tries to access but is NOT a member
        mockMvc.perform(get("/api/projects/" + project.getId())
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isForbidden());
    }


    @Test
    void updateProject_AsProjectLead_ShouldReturn200() throws Exception {
        // Given
        Project project = createTestProject("Original Project");
        addMemberToProject(project, testUser, "PROJECT_LEAD");

        UpdateProjectRequest request = UpdateProjectRequest.builder()
                .name("Updated Project")
                .description("Updated description")
                .build();

        // When & Then
        mockMvc.perform(put("/api/projects/" + project.getId())
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Project"));

        // Verify update in database
        Project updatedProject = projectRepository.findById(project.getId()).orElseThrow();
        assert updatedProject.getName().equals("Updated Project");
        assert updatedProject.getDescription().equals("Updated description");
    }

    @Test
    void updateProject_AsNonProjectLead_ShouldReturn403() throws Exception {
        // Given
        Project project = createTestProject("Test Project");
        project.setProjectLead(otherUser);  // ✅ Set otherUser as lead
        project = projectRepository.save(project);
        addMemberToProject(project, otherUser, "PROJECT_LEAD");
        addMemberToProject(project, testUser, "DEVELOPER");

        UpdateProjectRequest request = UpdateProjectRequest.builder()
                .name("Updated Project")
                .build();

        // When & Then
        mockMvc.perform(put("/api/projects/" + project.getId())
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProject_AsProjectLead_ShouldReturn204() throws Exception {
        // Given
        Project project = createTestProject("Test Project");
        addMemberToProject(project, testUser, "PROJECT_LEAD");

        // When & Then
        mockMvc.perform(delete("/api/projects/" + project.getId())
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isNoContent());

        // Verify project is archived
        Project archivedProject = projectRepository.findById(project.getId()).orElseThrow();
        assert archivedProject.getStatus().equals("ARCHIVED");
    }

    @Test
    void addMember_AsProjectLead_ShouldReturn200() throws Exception {
        // Given
        Project project = createTestProject("Test Project");
        addMemberToProject(project, testUser, "PROJECT_LEAD");

        AddMemberRequest request = AddMemberRequest.builder()
                .userId(otherUser.getId())
                .role("DEVELOPER")
                .build();

        // When & Then
        mockMvc.perform(post("/api/projects/" + project.getId() + "/members")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Member added successfully"));

        // Verify member is added
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(
                project.getId(), otherUser.getId()
        );
        assert isMember;
    }

    @Test
    void removeMember_AsProjectLead_ShouldReturn204() throws Exception {
        // Given
        Project project = createTestProject("Test Project");
        addMemberToProject(project, testUser, "PROJECT_LEAD");
        addMemberToProject(project, otherUser, "DEVELOPER");

        // When & Then
        mockMvc.perform(delete("/api/projects/" + project.getId() + "/members/" + otherUser.getId())
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isNoContent());

        // Verify member is removed
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(
                project.getId(), otherUser.getId()
        );
        assert !isMember;
    }

    @Test
    void removeMember_RemovingProjectLead_ShouldReturn400() throws Exception {
        // Given
        Project project = createTestProject("Test Project");
        addMemberToProject(project, testUser, "PROJECT_LEAD");

        // When & Then
        mockMvc.perform(delete("/api/projects/" + project.getId() + "/members/" + testUser.getId())
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProjectMembers_AsMember_ShouldReturn200() throws Exception {
        // Given
        Project project = createTestProject("Test Project");
        addMemberToProject(project, testUser, "PROJECT_LEAD");
        addMemberToProject(project, otherUser, "DEVELOPER");

        // When & Then
        mockMvc.perform(get("/api/projects/" + project.getId() + "/members")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].role", containsInAnyOrder("PROJECT_LEAD", "DEVELOPER")));
    }

    // ========================================
    // Helper Methods
    // ========================================

    private Project createTestProject(String name) {
        Project project = Project.builder()
                .name(name)
                .description("Test description")
                .organization(testOrganization)
                .projectLead(testUser)
                .status("ACTIVE")
                .build();
        return projectRepository.save(project);
    }

    private void addMemberToProject(Project project, User user, String role) {
        ProjectMember.ProjectMemberId id = new ProjectMember.ProjectMemberId(
                project.getId(), user.getId()
        );

        ProjectMember projectMember = ProjectMember.builder()
                .id(id)
                .project(project)
                .user(user)
                .role(role)
                .build();

        projectMemberRepository.save(projectMember);


    }
}
