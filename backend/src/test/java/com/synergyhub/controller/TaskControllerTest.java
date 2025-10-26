package com.synergyhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synergyhub.config.TestSecurityConfig;
import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.enums.TaskPriority;
import com.synergyhub.domain.enums.TaskStatus;
import com.synergyhub.dto.request.AssignTaskRequest;
import com.synergyhub.dto.request.CreateTaskRequest;
import com.synergyhub.dto.request.UpdateTaskRequest;
import com.synergyhub.dto.response.TaskResponse;
import com.synergyhub.dto.response.UserResponse;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.task.TaskService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
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
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private UserRepository userRepository;

    private User testUser;
    private UserPrincipal userPrincipal;
    private TaskResponse taskResponse;
    private UserResponse assigneeResponse;

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

        assigneeResponse = UserResponse.builder()
                .id(2)
                .name("Assignee User")
                .email("assignee@example.com")
                .build();

        UserResponse creatorResponse = UserResponse.builder()
                .id(1)
                .name("Test User")
                .email("test@example.com")
                .build();

        taskResponse = TaskResponse.builder()
                .id(1)
                .title("Test Task")
                .description("Test description")
                .projectId(1)
                .projectName("Test Project")
                .sprintId(1)
                .sprintName("Sprint 1")
                .status(TaskStatus.TO_DO)
                .priority(TaskPriority.MEDIUM)
                .assignee(assigneeResponse)
                .creator(creatorResponse)
                .storyPoints(5)
                .dueDate(LocalDateTime.now().plusDays(7))
                .build();

        when(userRepository.findByEmailWithRolesAndPermissions(anyString()))
                .thenReturn(Optional.of(testUser));
    }

    // ========== CREATE TASK TESTS ==========

    @Test
    @WithMockUser
    void createTask_WithValidData_ShouldReturn201() throws Exception {
        // Given
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("New Task")
                .description("Task description")
                .projectId(1)
                .priority(TaskPriority.HIGH)
                .storyPoints(3)
                .build();

        when(taskService.createTask(any(), eq(testUser)))
                .thenReturn(taskResponse);

        // When & Then
        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task created successfully"))
                .andExpect(jsonPath("$.data.title").value("Test Task"))
                .andExpect(jsonPath("$.data.status").value("TO_DO"));

        verify(taskService).createTask(any(), eq(testUser));
    }

    @Test
    @WithMockUser
    void createTask_WithInvalidData_ShouldReturn400() throws Exception {
        // Given - Empty title
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("")
                .projectId(1)
                .build();

        // When & Then
        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createTask(any(), any());
    }

    // ========== GET TASK TESTS ==========

    @Test
    @WithMockUser
    void getTaskById_WithValidId_ShouldReturn200() throws Exception {
        // Given
        when(taskService.getTaskById(1, testUser))
                .thenReturn(taskResponse);

        // When & Then
        mockMvc.perform(get("/api/tasks/1")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Test Task"));

        verify(taskService).getTaskById(1, testUser);
    }

    @Test
    @WithMockUser
    void getTasksByProject_ShouldReturnListOfTasks() throws Exception {
        // Given
        List<TaskResponse> tasks = Arrays.asList(
                taskResponse,
                TaskResponse.builder().id(2).title("Task 2").build()
        );

        when(taskService.getTasksByProject(1, testUser))
                .thenReturn(tasks);

        // When & Then
        mockMvc.perform(get("/api/projects/1/tasks")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)));

        verify(taskService).getTasksByProject(1, testUser);
    }

    @Test
    @WithMockUser
    void getTasksBySprint_ShouldReturnListOfTasks() throws Exception {
        // Given
        List<TaskResponse> tasks = List.of(taskResponse);

        when(taskService.getTasksBySprint(1, testUser))
                .thenReturn(tasks);

        // When & Then
        mockMvc.perform(get("/api/sprints/1/tasks")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(taskService).getTasksBySprint(1, testUser);
    }

    @Test
    @WithMockUser
    void getBacklogTasks_ShouldReturnListOfTasks() throws Exception {
        // Given
        List<TaskResponse> backlogTasks = List.of(taskResponse);

        when(taskService.getTasksInBacklog(1, testUser))
                .thenReturn(backlogTasks);

        // When & Then
        mockMvc.perform(get("/api/projects/1/backlog")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(taskService).getTasksInBacklog(1, testUser);
    }

    @Test
    @WithMockUser
    void getMyTasks_ShouldReturnListOfTasks() throws Exception {
        // Given
        List<TaskResponse> myTasks = List.of(taskResponse);

        when(taskService.getMyTasks(testUser))
                .thenReturn(myTasks);

        // When & Then
        mockMvc.perform(get("/api/tasks/my-tasks")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(taskService).getMyTasks(testUser);
    }

    // ========== UPDATE TASK TESTS ==========

    @Test
    @WithMockUser
    void updateTask_WithValidData_ShouldReturn200() throws Exception {
        // Given
        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("Updated Task")
                .description("Updated description")
                .status(TaskStatus.IN_PROGRESS)
                .build();

        TaskResponse updatedResponse = TaskResponse.builder()
                .id(1)
                .title("Updated Task")
                .status(TaskStatus.IN_PROGRESS)
                .build();

        when(taskService.updateTask(eq(1), any(), eq(testUser)))
                .thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/api/tasks/1")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Task"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        verify(taskService).updateTask(eq(1), any(), eq(testUser));
    }

    // ========== ASSIGN TASK TESTS ==========

    @Test
    @WithMockUser
    void updateTaskAssignee_AssignToUser_ShouldReturn200() throws Exception {
        // Given
        AssignTaskRequest request = AssignTaskRequest.builder()
                .assigneeId(2)
                .build();

        when(taskService.assignTask(1, 2, testUser))
                .thenReturn(taskResponse);

        // When & Then
        mockMvc.perform(put("/api/tasks/1/assignee")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task assigned successfully"))
                .andExpect(jsonPath("$.data").exists());

        verify(taskService).assignTask(1, 2, testUser);
    }

    @Test
    @WithMockUser
    void updateTaskAssignee_UnassignTask_ShouldReturn200() throws Exception {
        // Given
        AssignTaskRequest request = AssignTaskRequest.builder()
                .assigneeId(null)
                .build();

        doNothing().when(taskService).unassignTask(1, testUser);
        when(taskService.getTaskById(1, testUser)).thenReturn(taskResponse);

        // When & Then
        mockMvc.perform(put("/api/tasks/1/assignee")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task unassigned successfully"))
                .andExpect(jsonPath("$.data").exists());

        verify(taskService).unassignTask(1, testUser);
        verify(taskService).getTaskById(1, testUser);
    }

    // ========== MOVE TASK TESTS ==========

    @Test
    @WithMockUser
    void moveTaskToSprint_WithValidSprintId_ShouldReturn200() throws Exception {
        // Given
        when(taskService.moveTaskToSprint(1, 2, testUser))
                .thenReturn(taskResponse);

        // When & Then
        mockMvc.perform(post("/api/tasks/1/move-to-sprint/2")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task moved to sprint successfully"));

        verify(taskService).moveTaskToSprint(1, 2, testUser);
    }

    @Test
    @WithMockUser
    void moveTaskToBacklog_WithValidId_ShouldReturn200() throws Exception {
        // Given
        when(taskService.moveTaskToSprint(1, null, testUser))
                .thenReturn(taskResponse);

        // When & Then
        mockMvc.perform(post("/api/tasks/1/move-to-backlog")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task moved to backlog successfully"));

        verify(taskService).moveTaskToSprint(1, null, testUser);
    }

    // ========== DELETE TASK TESTS ==========

    @Test
    @WithMockUser
    void deleteTask_WithValidId_ShouldReturn204() throws Exception {
        // Given
        doNothing().when(taskService).deleteTask(1, testUser);

        // When & Then
        mockMvc.perform(delete("/api/tasks/1")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask(1, testUser);
    }
}