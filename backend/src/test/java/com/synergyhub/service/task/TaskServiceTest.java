package com.synergyhub.service.task;

import com.synergyhub.domain.entity.*;
import com.synergyhub.domain.enums.TaskPriority;
import com.synergyhub.domain.enums.TaskStatus;
import com.synergyhub.domain.enums.SprintStatus;
import com.synergyhub.dto.mapper.TaskMapper;
import com.synergyhub.dto.request.CreateTaskRequest;
import com.synergyhub.dto.request.UpdateTaskRequest;
import com.synergyhub.dto.response.TaskResponse;
import com.synergyhub.exception.*;
import com.synergyhub.repository.*;
import com.synergyhub.service.security.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    private User testUser;
    private User assignee;
    private Organization testOrganization;
    private Project testProject;
    private Sprint testSprint;
    private Task testTask;

    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
                .id(1)
                .name("Test Org")
                .build();

        testUser = User.builder()
                .id(1)
                .name("Test User")
                .email("test@example.com")
                .organization(testOrganization)
                .build();

        assignee = User.builder()
                .id(2)
                .name("Assignee User")
                .email("assignee@example.com")
                .organization(testOrganization)
                .build();

        testProject = Project.builder()
                .id(1)
                .name("Test Project")
                .organization(testOrganization)
                .projectLead(testUser)
                .status("ACTIVE")
                .build();

        testSprint = Sprint.builder()
                .id(1)
                .name("Sprint 1")
                .project(testProject)
                .status(SprintStatus.ACTIVE)
                .build();

        testTask = Task.builder()
                .id(1)
                .title("Test Task")
                .description("Test description")
                .project(testProject)
                .sprint(testSprint)
                .creator(testUser)
                .assignee(assignee)
                .status(TaskStatus.TO_DO)
                .priority(TaskPriority.MEDIUM)
                .storyPoints(5)
                .build();
    }

    // ========== CREATE TASK TESTS ==========

    @Test
    void createTask_WithValidData_ShouldReturnTaskResponse() {
        // Given
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("New Task")
                .description("Task description")
                .projectId(1)
                .priority(TaskPriority.HIGH)
                .storyPoints(3)
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(taskMapper.toTaskResponse(testTask)).thenReturn(
                TaskResponse.builder().id(1).title("New Task").build()
        );

        // When
        TaskResponse response = taskService.createTask(request, testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        verify(projectRepository).findById(1);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_WithNonExistentProject_ShouldThrowException() {
        // Given
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("New Task")
                .projectId(999)
                .priority(TaskPriority.HIGH)
                .build();

        when(projectRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskService.createTask(request, testUser))
                .isInstanceOf(ProjectNotFoundException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_WhenUserNotProjectMember_ShouldThrowException() {
        // Given
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("New Task")
                .projectId(1)
                .priority(TaskPriority.HIGH)
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> taskService.createTask(request, testUser))
                .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_WithSprintId_ShouldAssignToSprint() {
        // Given
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("New Task")
                .projectId(1)
                .sprintId(1)
                .priority(TaskPriority.HIGH)
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(sprintRepository.findById(1)).thenReturn(Optional.of(testSprint));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(taskMapper.toTaskResponse(any())).thenReturn(
                TaskResponse.builder().id(1).sprintId(1).build()
        );

        // When
        TaskResponse response = taskService.createTask(request, testUser);

        // Then
        assertThat(response.getSprintId()).isEqualTo(1);
        verify(sprintRepository).findById(1);
    }

    @Test
    void createTask_WithAssigneeId_ShouldAssignToUser() {
        // Given
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("New Task")
                .projectId(1)
                .assigneeId(2)
                .priority(TaskPriority.HIGH)
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(userRepository.findById(2)).thenReturn(Optional.of(assignee));
        when(projectMemberRepository.hasAccessToTaskProject(1, 2)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(taskMapper.toTaskResponse(any())).thenReturn(
                TaskResponse.builder().id(1).build()
        );

        // When
        TaskResponse response = taskService.createTask(request, testUser);

        // Then
        assertThat(response).isNotNull();
        verify(userRepository).findById(2);
    }

    // ========== GET TASK TESTS ==========

    @Test
    void getTaskById_WithValidId_ShouldReturnTask() {
        // Given
        when(taskRepository.findById(1)).thenReturn(Optional.of(testTask));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(taskMapper.toTaskResponse(testTask)).thenReturn(
                TaskResponse.builder().id(1).title("Test Task").build()
        );

        // When
        TaskResponse response = taskService.getTaskById(1, testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        verify(taskRepository).findById(1);
    }

    @Test
    void getTaskById_WithNonExistentId_ShouldThrowException() {
        // Given
        when(taskRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskService.getTaskById(999, testUser))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void getTasksByProject_ShouldReturnAllTasks() {
        // Given
        Task task2 = Task.builder()
                .id(2)
                .title("Task 2")
                .project(testProject)
                .creator(testUser)
                .status(TaskStatus.TO_DO)
                .priority(TaskPriority.HIGH)
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(taskRepository.findByProjectIdWithDetails(1))
                .thenReturn(Arrays.asList(testTask, task2));
        // ✅ FIXED: Correctly handle the List<Task> argument
        when(taskMapper.toTaskResponseList(anyList()))
                .thenAnswer(inv -> {
                    List<Task> taskList = inv.getArgument(0);  // Get the list
                    return taskList.stream()
                            .map(t -> TaskResponse.builder()
                                    .id(t.getId())
                                    .title(t.getTitle())
                                    .build())
                            .collect(Collectors.toList());
                });

        // When
        List<TaskResponse> tasks = taskService.getTasksByProject(1, testUser);

        // Then
        assertThat(tasks).hasSize(2);
        assertThat(tasks).extracting("title")
                .containsExactly("Test Task", "Task 2");
    }

    @Test
    void getTasksBySprint_ShouldReturnSprintTasks() {
        // Given
        when(sprintRepository.findById(1)).thenReturn(Optional.of(testSprint));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(taskRepository.findBySprintIdOrderByPriorityDescCreatedAtAsc(1))
                .thenReturn(List.of(testTask));
        when(taskMapper.toTaskResponseList(anyList())).thenReturn(
                List.of(TaskResponse.builder().id(1).build())
        );

        // When
        List<TaskResponse> tasks = taskService.getTasksBySprint(1, testUser);

        // Then
        assertThat(tasks).hasSize(1);
        verify(taskRepository).findBySprintIdOrderByPriorityDescCreatedAtAsc(1);
    }

    // ========== UPDATE TASK TESTS ==========

    @Test
    void updateTask_WithValidData_ShouldReturnUpdatedTask() {
        // Given
        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("Updated Title")
                .description("Updated description")
                .status(TaskStatus.IN_PROGRESS)
                .build();

        when(taskRepository.findById(1)).thenReturn(Optional.of(testTask));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(taskMapper.toTaskResponse(any())).thenReturn(
                TaskResponse.builder().id(1).title("Updated Title").build()
        );

        // When
        TaskResponse response = taskService.updateTask(1, request, testUser);

        // Then
        assertThat(response.getTitle()).isEqualTo("Updated Title");
        verify(taskRepository).save(testTask);
    }

    @Test
    void updateTask_ChangeAssignee_ShouldUpdateAssignee() {
        // Given
        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .assigneeId(2)
                .build();

        when(taskRepository.findById(1)).thenReturn(Optional.of(testTask));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(userRepository.findById(2)).thenReturn(Optional.of(assignee));
        when(projectMemberRepository.hasAccessToTaskProject(1, 2)).thenReturn(true);
        when(taskRepository.save(any())).thenReturn(testTask);
        when(taskMapper.toTaskResponse(any())).thenReturn(
                TaskResponse.builder().id(1).build()
        );

        // When
        TaskResponse response = taskService.updateTask(1, request, testUser);

        // Then
        assertThat(response).isNotNull();
        verify(userRepository).findById(2);
    }

    // ========== ASSIGN TASK TESTS ==========

    @Test
    void assignTask_WithValidUser_ShouldAssignTask() {
        // Given
        when(taskRepository.findById(1)).thenReturn(Optional.of(testTask));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(userRepository.findById(2)).thenReturn(Optional.of(assignee));
        when(projectMemberRepository.hasAccessToTaskProject(1, 2)).thenReturn(true);
        when(taskRepository.save(any())).thenReturn(testTask);
        when(taskMapper.toTaskResponse(any())).thenReturn(
                TaskResponse.builder().id(1).build()
        );

        // When
        TaskResponse response = taskService.assignTask(1, 2, testUser);

        // Then
        assertThat(response).isNotNull();
        verify(taskRepository).save(testTask);
    }

    @Test
    void assignTask_WhenAssigneeNotProjectMember_ShouldThrowException() {
        // Given
        when(taskRepository.findById(1)).thenReturn(Optional.of(testTask));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(userRepository.findById(2)).thenReturn(Optional.of(assignee));
        when(projectMemberRepository.hasAccessToTaskProject(1, 2)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> taskService.assignTask(1, 2, testUser))
                .isInstanceOf(TaskAssignmentException.class);

        verify(taskRepository, never()).save(any());
    }

    // ========== DELETE TASK TESTS ==========

    @Test
    void deleteTask_WithValidTask_ShouldDelete() {
        // Given
        when(taskRepository.findById(1)).thenReturn(Optional.of(testTask));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);

        // When
        taskService.deleteTask(1, testUser);

        // Then
        verify(taskRepository).delete(testTask);
    }

    @Test
    void deleteTask_WhenNotAuthorized_ShouldThrowException() {
        // Given
        when(taskRepository.findById(1)).thenReturn(Optional.of(testTask));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> taskService.deleteTask(1, testUser))
                .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).delete(any());
    }

    // ========== MOVE TO SPRINT TESTS ==========

    @Test
    void moveTaskToSprint_WithValidSprint_ShouldMoveTask() {
        // Given
        when(taskRepository.findById(1)).thenReturn(Optional.of(testTask));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(sprintRepository.findById(1)).thenReturn(Optional.of(testSprint));
        when(taskRepository.save(any())).thenReturn(testTask);
        when(taskMapper.toTaskResponse(any())).thenReturn(
                TaskResponse.builder().id(1).sprintId(1).build()
        );

        // When
        TaskResponse response = taskService.moveTaskToSprint(1, 1, testUser);

        // Then
        assertThat(response.getSprintId()).isEqualTo(1);
        verify(taskRepository).save(testTask);
    }

    @Test
    void moveTaskToBacklog_ShouldRemoveSprint() {
        // Given
        when(taskRepository.findById(1)).thenReturn(Optional.of(testTask));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(taskRepository.save(any())).thenReturn(testTask);
        when(taskMapper.toTaskResponse(any())).thenReturn(
                TaskResponse.builder().id(1).sprintId(null).build()
        );

        // When
        TaskResponse response = taskService.moveTaskToSprint(1, null, testUser);

        // Then
        assertThat(response.getSprintId()).isNull();
        verify(taskRepository).save(testTask);
    }
}
