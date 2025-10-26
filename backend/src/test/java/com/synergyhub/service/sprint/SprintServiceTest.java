package com.synergyhub.service.sprint;

import com.synergyhub.domain.entity.*;
import com.synergyhub.domain.enums.SprintStatus;
import com.synergyhub.dto.mapper.SprintMapper;
import com.synergyhub.dto.request.CreateSprintRequest;
import com.synergyhub.dto.request.UpdateSprintRequest;
import com.synergyhub.dto.response.SprintResponse;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SprintServiceTest {

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private SprintMapper sprintMapper;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private SprintService sprintService;

    private User testUser;
    private Project testProject;
    private Sprint testSprint;

    @BeforeEach
    void setUp() {
        Organization testOrganization = Organization.builder()
                .id(1)
                .name("Test Org")
                .build();

        testUser = User.builder()
                .id(1)
                .name("Test User")
                .email("test@example.com")
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
                .goal("Complete user authentication")
                .project(testProject)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(2))
                .status(SprintStatus.PLANNING)
                .build();
    }

    // ========== CREATE SPRINT TESTS ==========

    @Test
    void createSprint_WithValidData_ShouldReturnSprintResponse() {
        // Given
        CreateSprintRequest request = CreateSprintRequest.builder()
                .name("Sprint 1")
                .goal("Complete user stories")
                .projectId(1)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(2))
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(sprintRepository.findActiveSprintByProjectId(1)).thenReturn(Optional.empty());
        when(sprintRepository.findOverlappingSprints(anyInt(), any(), any())).thenReturn(List.of());
        when(sprintRepository.save(any(Sprint.class))).thenReturn(testSprint);
        when(sprintMapper.toSprintResponse(testSprint)).thenReturn(
                SprintResponse.builder().id(1).name("Sprint 1").build()
        );

        // When
        SprintResponse response = sprintService.createSprint(request, testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        verify(projectRepository).findById(1);
        verify(sprintRepository).save(any(Sprint.class));
    }

    @Test
    void createSprint_WithNonExistentProject_ShouldThrowException() {
        // Given
        CreateSprintRequest request = CreateSprintRequest.builder()
                .name("Sprint 1")
                .projectId(999)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(2))
                .build();

        when(projectRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sprintService.createSprint(request, testUser))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("999");

        verify(sprintRepository, never()).save(any());
    }

    @Test
    void createSprint_WhenUserNotProjectMember_ShouldThrowException() {
        // Given
        CreateSprintRequest request = CreateSprintRequest.builder()
                .name("Sprint 1")
                .projectId(1)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(2))
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(false);

        // When & Then
        // ✅ FIXED: Expect Spring Security's AccessDeniedException
        assertThatThrownBy(() -> sprintService.createSprint(request, testUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You don't have access to this project");

        verify(sprintRepository, never()).save(any());
    }

    @Test
    void createSprint_WithInvalidDateRange_ShouldThrowException() {
        // Given
        CreateSprintRequest request = CreateSprintRequest.builder()
                .name("Sprint 1")
                .projectId(1)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(5)) // End before start
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> sprintService.createSprint(request, testUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("End date must be after start date");

        verify(sprintRepository, never()).save(any());
    }

    @Test
    void createSprint_WithOverlappingActiveSprint_ShouldThrowException() {
        // Given
        CreateSprintRequest request = CreateSprintRequest.builder()
                .name("Sprint 2")
                .projectId(1)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(2))
                .build();

        Sprint activeSprint = Sprint.builder()
                .id(2)
                .name("Active Sprint")
                .project(testProject)
                .status(SprintStatus.ACTIVE)
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(sprintRepository.findActiveSprintByProjectId(1))
                .thenReturn(Optional.of(activeSprint));

        // When & Then
        assertThatThrownBy(() -> sprintService.createSprint(request, testUser))
                .isInstanceOf(SprintAlreadyActiveException.class)
                .hasMessageContaining("already has an active sprint");

        verify(sprintRepository, never()).save(any());
    }

    // ========== GET SPRINT TESTS ==========

    @Test
    void getSprintById_WithValidId_ShouldReturnSprint() {
        // Given
        when(sprintRepository.findById(1)).thenReturn(Optional.of(testSprint));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(sprintMapper.toSprintResponse(testSprint)).thenReturn(
                SprintResponse.builder().id(1).name("Sprint 1").build()
        );

        // When
        SprintResponse response = sprintService.getSprintById(1, testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        verify(sprintRepository).findById(1);
    }

    @Test
    void getSprintById_WithNonExistentId_ShouldThrowException() {
        // Given
        when(sprintRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sprintService.getSprintById(999, testUser))
                .isInstanceOf(SprintNotFoundException.class);
    }

    @Test
    void getSprintsByProject_ShouldReturnAllSprints() {
        // Given
        Sprint sprint2 = Sprint.builder()
                .id(2)
                .name("Sprint 2")
                .project(testProject)
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(sprintRepository.findByProjectIdOrderByStartDateDesc(1))
                .thenReturn(Arrays.asList(testSprint, sprint2));
        when(sprintMapper.toSprintResponseList(any()))
                .thenReturn(Arrays.asList(
                        SprintResponse.builder().id(1).name("Sprint 1").build(),
                        SprintResponse.builder().id(2).name("Sprint 2").build()
                ));

        // When
        List<SprintResponse> sprints = sprintService.getSprintsByProject(1, testUser);

        // Then
        assertThat(sprints).hasSize(2);
        assertThat(sprints).extracting("name")
                .containsExactly("Sprint 1", "Sprint 2");
    }

    // ========== UPDATE SPRINT TESTS ==========

    @Test
    void updateSprint_WithValidData_ShouldReturnUpdatedSprint() {
        // Given
        UpdateSprintRequest request = UpdateSprintRequest.builder()
                .name("Updated Sprint")
                .goal("Updated goal")
                .build();

        when(sprintRepository.findById(1)).thenReturn(Optional.of(testSprint));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(sprintRepository.save(any(Sprint.class))).thenReturn(testSprint);
        when(sprintMapper.toSprintResponse(any())).thenReturn(
                SprintResponse.builder().id(1).name("Updated Sprint").build()
        );

        // When
        SprintResponse response = sprintService.updateSprint(1, request, testUser);

        // Then
        assertThat(response.getName()).isEqualTo("Updated Sprint");
        verify(sprintRepository).save(testSprint);
    }

    @Test
    void updateSprint_WhenSprintIsCompleted_ShouldThrowException() {
        // Given
        testSprint.setStatus(SprintStatus.COMPLETED);
        UpdateSprintRequest request = UpdateSprintRequest.builder()
                .name("Updated Sprint")
                .build();

        when(sprintRepository.findById(1)).thenReturn(Optional.of(testSprint));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> sprintService.updateSprint(1, request, testUser))
                .isInstanceOf(InvalidSprintStateException.class)
                .hasMessageContaining("Cannot update completed sprint");

        verify(sprintRepository, never()).save(any());
    }

    // ========== START SPRINT TESTS ==========

    @Test
    void startSprint_WithValidSprint_ShouldActivateSprint() {
        // Given
        when(sprintRepository.findById(1)).thenReturn(Optional.of(testSprint));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(sprintRepository.findActiveSprintByProjectId(1)).thenReturn(Optional.empty());
        when(sprintRepository.save(any(Sprint.class))).thenReturn(testSprint);
        when(sprintMapper.toSprintResponse(any())).thenReturn(
                SprintResponse.builder().id(1).status(SprintStatus.ACTIVE).build()
        );

        // When
        SprintResponse response = sprintService.startSprint(1, testUser);

        // Then
        assertThat(response.getStatus()).isEqualTo(SprintStatus.ACTIVE);
        verify(sprintRepository).save(argThat(sprint ->
                sprint.getStatus() == SprintStatus.ACTIVE
        ));
    }

    @Test
    void startSprint_WhenAlreadyActive_ShouldThrowException() {
        // Given
        testSprint.setStatus(SprintStatus.ACTIVE);
        when(sprintRepository.findById(1)).thenReturn(Optional.of(testSprint));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> sprintService.startSprint(1, testUser))
                .isInstanceOf(InvalidSprintStateException.class)
                .hasMessageContaining("Sprint is already");
    }

    // ========== COMPLETE SPRINT TESTS ==========

    @Test
    void completeSprint_WithValidSprint_ShouldCompleteSprint() {
        // Given
        testSprint.setStatus(SprintStatus.ACTIVE);
        when(sprintRepository.findById(1)).thenReturn(Optional.of(testSprint));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);
        when(sprintRepository.save(any(Sprint.class))).thenReturn(testSprint);
        when(sprintMapper.toSprintResponse(any())).thenReturn(
                SprintResponse.builder().id(1).status(SprintStatus.COMPLETED).build()
        );

        // When
        SprintResponse response = sprintService.completeSprint(1, testUser);

        // Then
        assertThat(response.getStatus()).isEqualTo(SprintStatus.COMPLETED);
        verify(sprintRepository).save(argThat(sprint ->
                sprint.getStatus() == SprintStatus.COMPLETED
        ));
    }

    @Test
    void completeSprint_WhenNotActive_ShouldThrowException() {
        // Given
        testSprint.setStatus(SprintStatus.PLANNING);
        when(sprintRepository.findById(1)).thenReturn(Optional.of(testSprint));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> sprintService.completeSprint(1, testUser))
                .isInstanceOf(InvalidSprintStateException.class)
                .hasMessageContaining("Only active sprints can be completed");
    }

    // ========== DELETE SPRINT TESTS ==========

    @Test
    void deleteSprint_WithValidSprintInPlanning_ShouldDelete() {
        // Given
        when(sprintRepository.findById(1)).thenReturn(Optional.of(testSprint));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);

        // When
        sprintService.deleteSprint(1, testUser);

        // Then
        verify(sprintRepository).delete(testSprint);
    }

    @Test
    void deleteSprint_WithActiveSprint_ShouldThrowException() {
        // Given
        testSprint.setStatus(SprintStatus.ACTIVE);
        when(sprintRepository.findById(1)).thenReturn(Optional.of(testSprint));
        when(projectMemberRepository.hasAccessToTaskProject(1, 1)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> sprintService.deleteSprint(1, testUser))
                .isInstanceOf(InvalidSprintStateException.class)
                .hasMessageContaining("Cannot delete active or completed sprint");

        verify(sprintRepository, never()).delete(any());
    }
}
