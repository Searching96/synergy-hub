package com.synergyhub.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synergyhub.config.TestSecurityConfig;
import com.synergyhub.domain.entity.*;
import com.synergyhub.domain.enums.SprintStatus;
import com.synergyhub.domain.enums.TaskPriority;
import com.synergyhub.domain.enums.TaskStatus;
import com.synergyhub.dto.request.AssignTaskRequest;
import com.synergyhub.dto.request.CreateTaskRequest;
import com.synergyhub.dto.request.UpdateTaskRequest;
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
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@Transactional
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TaskRepository taskRepository;

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

    private User testUser;
    private User assigneeUser;
    private Project testProject;
    private Sprint testSprint;
    private UserPrincipal userPrincipal;
    private Organization testOrganization;

    @BeforeEach
    void setUp() {
        // Clean up
        taskRepository.deleteAll();
        sprintRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // Create test data
         testOrganization = organizationRepository.save(
                Organization.builder()
                        .name("Test Organization")
                        .address("Test Address")
                        .build()
        );

        testUser = userRepository.save(
                User.builder()
                        .name("Test User")
                        .email("test@example.com")
                        .passwordHash("hashed")
                        .organization(testOrganization)
                        .emailVerified(true)
                        .build()
        );

        assigneeUser = userRepository.save(
                User.builder()
                        .name("Assignee User")
                        .email("assignee@example.com")
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

        testSprint = sprintRepository.save(
                Sprint.builder()
                        .name("Sprint 1")
                        .goal("Test goal")
                        .project(testProject)
                        .startDate(LocalDate.now().plusDays(1))
                        .endDate(LocalDate.now().plusWeeks(2))
                        .status(SprintStatus.PLANNING)
                        .build()
        );

        // Add users as project members
        ProjectMember.ProjectMemberId memberId1 = new ProjectMember.ProjectMemberId(
                testProject.getId(), testUser.getId()
        );
        projectMemberRepository.save(
                ProjectMember.builder()
                        .id(memberId1)
                        .project(testProject)
                        .user(testUser)
                        .role("PROJECT_LEAD")
                        .build()
        );

        ProjectMember.ProjectMemberId memberId2 = new ProjectMember.ProjectMemberId(
                testProject.getId(), assigneeUser.getId()
        );
        projectMemberRepository.save(
                ProjectMember.builder()
                        .id(memberId2)
                        .project(testProject)
                        .user(assigneeUser)
                        .role("DEVELOPER")
                        .build()
        );

        entityManager.flush();
        entityManager.clear();

        userPrincipal = UserPrincipal.create(testUser);
    }

    // ========== CREATE TASK TESTS ==========

    @Test
    void createTask_WithValidData_ShouldReturn201() throws Exception {
        // Given
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("New Task")
                .description("Task description")
                .projectId(testProject.getId())
                .sprintId(testSprint.getId())
                .priority(TaskPriority.HIGH)
                .storyPoints(5)
                .dueDate(LocalDateTime.now().plusDays(7))
                .build();

        // When & Then
        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("New Task"))
                .andExpect(jsonPath("$.data.status").value("TO_DO"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.storyPoints").value(5));

        // Verify task was created in database
        assertThat(taskRepository.findAll()).hasSize(1);
    }

    @Test
    void createTask_WithAssignee_ShouldReturn201() throws Exception {
        // Given
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Assigned Task")
                .projectId(testProject.getId())
                .assigneeId(assigneeUser.getId())
                .priority(TaskPriority.MEDIUM)
                .build();

        // When & Then
        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignee.id").value(assigneeUser.getId()));
    }

    // ========== GET TASK TESTS ==========

    @Test
    void getTaskById_WithValidId_ShouldReturn200() throws Exception {
        // Given
        Task task = createTestTask("Test Task");

        // When & Then
        mockMvc.perform(get("/api/tasks/" + task.getId())
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(task.getId()))
                .andExpect(jsonPath("$.data.title").value("Test Task"));
    }

    @Test
    void getTasksByProject_ShouldReturnAllTasks() throws Exception {
        // Given
        createTestTask("Task 1");
        createTestTask("Task 2");
        entityManager.flush();
        entityManager.clear();

        // When & Then
        mockMvc.perform(get("/api/projects/" + testProject.getId() + "/tasks")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void getTasksBySprint_ShouldReturnSprintTasks() throws Exception {
        // Given
        createTestTask("Sprint Task");
        entityManager.flush();

        // When & Then
        mockMvc.perform(get("/api/sprints/" + testSprint.getId() + "/tasks")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void getBacklogTasks_ShouldReturnUnassignedTasks() throws Exception {
        // Given - Create task without sprint
        Task backlogTask = Task.builder()
                .title("Backlog Task")
                .project(testProject)
                .creator(testUser)
                .status(TaskStatus.BACKLOG)
                .priority(TaskPriority.LOW)
                .build();
        taskRepository.save(backlogTask);
        entityManager.flush();

        // When & Then
        mockMvc.perform(get("/api/projects/" + testProject.getId() + "/backlog")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Backlog Task"));
    }

    // ========== UPDATE TASK TESTS ==========

    @Test
    void updateTask_WithValidData_ShouldReturn200() throws Exception {
        // Given
        Task task = createTestTask("Original Task");
        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("Updated Task")
                .status(TaskStatus.IN_PROGRESS)
                .build();

        // When & Then
        mockMvc.perform(put("/api/tasks/" + task.getId())
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Task"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    // ========== ASSIGN TASK TESTS ==========

    @Test
    void updateTaskAssignee_AssignToUser_ShouldReturn200() throws Exception {
        // Given
        Task task = createTestTask("Unassigned Task");
        AssignTaskRequest request = AssignTaskRequest.builder()
                .assigneeId(assigneeUser.getId())
                .build();

        // When & Then
        mockMvc.perform(put("/api/tasks/" + task.getId() + "/assignee")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task assigned successfully"))
                .andExpect(jsonPath("$.data.assignee.id").value(assigneeUser.getId()));

        // Verify assignment in database
        Task updatedTask = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(updatedTask.getAssignee()).isNotNull();
        assertThat(updatedTask.getAssignee().getId()).isEqualTo(assigneeUser.getId());
    }

    @Test
    void updateTaskAssignee_UnassignTask_ShouldReturn200() throws Exception {
        // Given
        Task task = createTestTask("Assigned Task");
        task.setAssignee(assigneeUser);
        taskRepository.save(task);
        entityManager.flush();

        AssignTaskRequest request = AssignTaskRequest.builder()
                .assigneeId(null)
                .build();

        // When & Then
        mockMvc.perform(put("/api/tasks/" + task.getId() + "/assignee")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task unassigned successfully"));

        // Verify unassigned in database
        Task updatedTask = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(updatedTask.getAssignee()).isNull();
    }

    @Test
    void updateTaskAssignee_ReassignToAnotherUser_ShouldReturn200() throws Exception {
        // Given - Task initially assigned to assigneeUser
        Task task = createTestTask("Assigned Task");
        task.setAssignee(assigneeUser);
        taskRepository.save(task);
        entityManager.flush();

        // Create another user
        User anotherUser = userRepository.save(
                User.builder()
                        .name("Another User")
                        .email("another@example.com")
                        .passwordHash("hashed")
                        .organization(testOrganization)
                        .emailVerified(true)
                        .build()
        );

        // Add as project member
        ProjectMember.ProjectMemberId memberId = new ProjectMember.ProjectMemberId(
                testProject.getId(), anotherUser.getId()
        );
        projectMemberRepository.save(
                ProjectMember.builder()
                        .id(memberId)
                        .project(testProject)
                        .user(anotherUser)
                        .role("DEVELOPER")
                        .build()
        );
        entityManager.flush();

        // Reassign to another user
        AssignTaskRequest request = AssignTaskRequest.builder()
                .assigneeId(anotherUser.getId())
                .build();

        // When & Then
        mockMvc.perform(put("/api/tasks/" + task.getId() + "/assignee")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignee.id").value(anotherUser.getId()));

        // Verify reassignment
        Task updatedTask = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(updatedTask.getAssignee()).isNotNull();
        assertThat(updatedTask.getAssignee().getId()).isEqualTo(anotherUser.getId());
    }

    // ========== MOVE TASK TESTS ==========

    @Test
    void moveTaskToSprint_WithValidSprint_ShouldReturn200() throws Exception {
        // Given - Create task without sprint
        Task task = Task.builder()
                .title("Backlog Task")
                .project(testProject)
                .creator(testUser)
                .status(TaskStatus.BACKLOG)
                .priority(TaskPriority.MEDIUM)
                .build();
        taskRepository.save(task);
        entityManager.flush();

        // When & Then
        mockMvc.perform(post("/api/tasks/" + task.getId() + "/move-to-sprint/" + testSprint.getId())
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sprintId").value(testSprint.getId()));
    }

    @Test
    void moveTaskToBacklog_WithSprintTask_ShouldReturn200() throws Exception {
        // Given
        Task task = createTestTask("Sprint Task");

        // When & Then
        mockMvc.perform(post("/api/tasks/" + task.getId() + "/move-to-backlog")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sprintId").isEmpty());
    }

    // ========== DELETE TASK TESTS ==========

    @Test
    void deleteTask_WithValidTask_ShouldReturn204() throws Exception {
        // Given
        Task task = createTestTask("Task to delete");

        // When & Then
        mockMvc.perform(delete("/api/tasks/" + task.getId())
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isNoContent());

        // Verify deleted
        assertThat(taskRepository.findById(task.getId())).isEmpty();
    }

    // ========== HELPER METHODS ==========

    private Task createTestTask(String title) {
        Task task = Task.builder()
                .title(title)
                .description("Test description")
                .project(testProject)
                .sprint(testSprint)
                .creator(testUser)
                .status(TaskStatus.TO_DO)
                .priority(TaskPriority.MEDIUM)
                .storyPoints(3)
                .build();
        return taskRepository.save(task);
    }
}