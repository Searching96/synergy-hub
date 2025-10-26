package com.synergyhub.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synergyhub.config.TestSecurityConfig;
import com.synergyhub.domain.entity.*;
import com.synergyhub.domain.enums.SprintStatus;
import com.synergyhub.dto.request.CreateSprintRequest;
import com.synergyhub.dto.request.UpdateSprintRequest;
import com.synergyhub.repository.*;
import com.synergyhub.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@Transactional
class SprintControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    private Project testProject;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        // Clean up
        sprintRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // Create test data
        Organization testOrganization = organizationRepository.save(
                Organization.builder()
                        .name("Test Organization")
                        .address("Test Address")
                        .build()
        );

        User testUser = userRepository.save(
                User.builder()
                        .name("Test User")
                        .email("test@example.com")
                        .passwordHash("hashed")
                        .organization(testOrganization)
                        .emailVerified(true)
                        .build()
        );

        testProject = projectRepository.save(
                Project.builder()
                        .name("Test Project")
                        .description("Test description")
                        .organization(testOrganization)
                        .projectLead(testUser)
                        .status("ACTIVE")
                        .build()
        );

        // Add user as project member
        ProjectMember.ProjectMemberId memberId = new ProjectMember.ProjectMemberId(
                testProject.getId(), testUser.getId()
        );
        projectMemberRepository.save(
                ProjectMember.builder()
                        .id(memberId)
                        .project(testProject)
                        .user(testUser)
                        .role("PROJECT_LEAD")
                        .build()
        );

        entityManager.flush();
        entityManager.clear();

        userPrincipal = UserPrincipal.create(testUser);
    }

    // ========== CREATE SPRINT TESTS ==========

    @Test
    void createSprint_WithValidData_ShouldReturn201() throws Exception {
        // Given
        CreateSprintRequest request = CreateSprintRequest.builder()
                .name("Sprint 1")
                .goal("Complete authentication module")
                .projectId(testProject.getId())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(2))
                .build();

        // When & Then
        mockMvc.perform(post("/api/sprints")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Sprint 1"))
                .andExpect(jsonPath("$.data.goal").value("Complete authentication module"))
                .andExpect(jsonPath("$.data.status").value("PLANNING"))
                .andExpect(jsonPath("$.data.projectId").value(testProject.getId()));
    }

    @Test
    void createSprint_WithInvalidDateRange_ShouldReturn400() throws Exception {
        // Given
        CreateSprintRequest request = CreateSprintRequest.builder()
                .name("Sprint 1")
                .projectId(testProject.getId())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().minusDays(1))
                .build();

        // When & Then
        mockMvc.perform(post("/api/sprints")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createSprint_WithNonExistentProject_ShouldReturn404() throws Exception {
        // Given
        CreateSprintRequest request = CreateSprintRequest.builder()
                .name("Sprint 1")
                .projectId(999)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(2))
                .build();

        // When & Then
        mockMvc.perform(post("/api/sprints")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // ========== GET SPRINT TESTS ==========

    @Test
    void getSprintById_WithValidId_ShouldReturn200() throws Exception {
        // Given
        Sprint sprint = createTestSprint("Sprint 1");

        // When & Then
        mockMvc.perform(get("/api/sprints/" + sprint.getId())
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(sprint.getId()))
                .andExpect(jsonPath("$.data.name").value("Sprint 1"));
    }

    @Test
    void getSprintsByProject_ShouldReturnAllSprints() throws Exception {
        // Given
        createTestSprint("Sprint 1");
        createTestSprint("Sprint 2");
        entityManager.flush();
        entityManager.clear();

        // When & Then
        mockMvc.perform(get("/api/projects/" + testProject.getId() + "/sprints")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    // ========== UPDATE SPRINT TESTS ==========

    @Test
    void updateSprint_WithValidData_ShouldReturn200() throws Exception {
        // Given
        Sprint sprint = createTestSprint("Original Name");
        UpdateSprintRequest request = UpdateSprintRequest.builder()
                .name("Updated Name")
                .goal("Updated goal")
                .build();

        // When & Then
        mockMvc.perform(put("/api/sprints/" + sprint.getId())
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Name"))
                .andExpect(jsonPath("$.data.goal").value("Updated goal"));
    }

    // ========== START SPRINT TESTS ==========

    @Test
    void startSprint_WithValidSprint_ShouldReturn200() throws Exception {
        // Given
        Sprint sprint = createTestSprint("Sprint 1");

        // When & Then
        mockMvc.perform(post("/api/sprints/" + sprint.getId() + "/start")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void startSprint_WhenAnotherIsActive_ShouldReturn400() throws Exception {
        // Given
        Sprint sprint1 = createTestSprint("Sprint 1");
        sprint1.setStatus(SprintStatus.ACTIVE);
        sprintRepository.save(sprint1);

        Sprint sprint2 = createTestSprint("Sprint 2");
        entityManager.flush();

        // When & Then
        mockMvc.perform(post("/api/sprints/" + sprint2.getId() + "/start")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ========== COMPLETE SPRINT TESTS ==========

    @Test
    void completeSprint_WithActiveSprint_ShouldReturn200() throws Exception {
        // Given
        Sprint sprint = createTestSprint("Sprint 1");
        sprint.setStatus(SprintStatus.ACTIVE);
        sprintRepository.save(sprint);
        entityManager.flush();

        // When & Then
        mockMvc.perform(post("/api/sprints/" + sprint.getId() + "/complete")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    // ========== DELETE SPRINT TESTS ==========

    @Test
    void deleteSprint_WithValidSprint_ShouldReturn204() throws Exception {
        // Given
        Sprint sprint = createTestSprint("Sprint to delete");

        // When & Then
        mockMvc.perform(delete("/api/sprints/" + sprint.getId())
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isNoContent());

        // Verify deleted
        assertThat(sprintRepository.findById(sprint.getId())).isEmpty();
    }

    @Test
    void deleteSprint_WithActiveSprint_ShouldReturn400() throws Exception {
        // Given
        Sprint sprint = createTestSprint("Active Sprint");
        sprint.setStatus(SprintStatus.ACTIVE);
        sprintRepository.save(sprint);
        entityManager.flush();

        // When & Then
        mockMvc.perform(delete("/api/sprints/" + sprint.getId())
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ========== HELPER METHODS ==========

    private Sprint createTestSprint(String name) {
        Sprint sprint = Sprint.builder()
                .name(name)
                .goal("Test goal")
                .project(testProject)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(2))
                .status(SprintStatus.PLANNING)
                .build();
        return sprintRepository.save(sprint);
    }
}
