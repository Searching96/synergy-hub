package com.synergyhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synergyhub.config.TestSecurityConfig;
import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.enums.SprintStatus;
import com.synergyhub.dto.request.CreateSprintRequest;
import com.synergyhub.dto.request.UpdateSprintRequest;
import com.synergyhub.dto.response.SprintDetailResponse;
import com.synergyhub.dto.response.SprintResponse;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.UserPrincipal;
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
class SprintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SprintService sprintService;

    @MockBean
    private UserRepository userRepository;

    private User testUser;
    private UserPrincipal userPrincipal;
    private SprintResponse sprintResponse;

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

        sprintResponse = SprintResponse.builder()
                .id(1)
                .name("Sprint 1")
                .goal("Complete features")
                .projectId(1)
                .projectName("Test Project")
                .status(SprintStatus.PLANNING)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusWeeks(2))
                .build();

        // Mock user repository
        when(userRepository.findByEmailWithRolesAndPermissions(anyString()))
                .thenReturn(Optional.of(testUser));
    }

    // ========== CREATE SPRINT TESTS ==========

    @Test
    @WithMockUser
    void createSprint_WithValidData_ShouldReturn201() throws Exception {
        // Given
        CreateSprintRequest request = CreateSprintRequest.builder()
                .name("Sprint 1")
                .goal("Complete authentication")
                .projectId(1)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusWeeks(2))
                .build();

        when(sprintService.createSprint(any(), eq(testUser)))
                .thenReturn(sprintResponse);

        // When & Then
        mockMvc.perform(post("/api/sprints")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Sprint created successfully"))
                .andExpect(jsonPath("$.data.name").value("Sprint 1"))
                .andExpect(jsonPath("$.data.status").value("PLANNING"));

        verify(sprintService).createSprint(any(), eq(testUser));
    }

    @Test
    @WithMockUser
    void createSprint_WithInvalidData_ShouldReturn400() throws Exception {
        // Given - Empty name
        CreateSprintRequest request = CreateSprintRequest.builder()
                .name("")
                .projectId(1)
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
                .andExpect(status().isBadRequest());

        verify(sprintService, never()).createSprint(any(), any());
    }

    // ========== GET SPRINT TESTS ==========

    @Test
    @WithMockUser
    void getSprintById_WithValidId_ShouldReturn200() throws Exception {
        // Given
        when(sprintService.getSprintById(1, testUser))
                .thenReturn(sprintResponse);

        // When & Then
        mockMvc.perform(get("/api/sprints/1")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Sprint 1"));

        verify(sprintService).getSprintById(1, testUser);
    }

    @Test
    @WithMockUser
    void getSprintDetails_WithValidId_ShouldReturn200() throws Exception {
        // Given
        SprintDetailResponse detailResponse = SprintDetailResponse.builder()
                .id(1)
                .name("Sprint 1")
                .status(SprintStatus.ACTIVE)
                .build();

        when(sprintService.getSprintDetails(1, testUser))
                .thenReturn(detailResponse);

        // When & Then
        mockMvc.perform(get("/api/sprints/1/details")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(sprintService).getSprintDetails(1, testUser);
    }

    // ========== UPDATE SPRINT TESTS ==========

    @Test
    @WithMockUser
    void updateSprint_WithValidData_ShouldReturn200() throws Exception {
        // Given
        UpdateSprintRequest request = UpdateSprintRequest.builder()
                .name("Updated Sprint")
                .goal("Updated goal")
                .build();

        SprintResponse updatedResponse = SprintResponse.builder()
                .id(1)
                .name("Updated Sprint")
                .goal("Updated goal")
                .build();

        when(sprintService.updateSprint(eq(1), any(), eq(testUser)))
                .thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/api/sprints/1")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Sprint"));

        verify(sprintService).updateSprint(eq(1), any(), eq(testUser));
    }

    // ========== START SPRINT TESTS ==========

    @Test
    @WithMockUser
    void startSprint_WithValidId_ShouldReturn200() throws Exception {
        // Given
        SprintResponse activeResponse = SprintResponse.builder()
                .id(1)
                .name("Sprint 1")
                .status(SprintStatus.ACTIVE)
                .build();

        when(sprintService.startSprint(1, testUser))
                .thenReturn(activeResponse);

        // When & Then
        mockMvc.perform(post("/api/sprints/1/start")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(sprintService).startSprint(1, testUser);
    }

    // ========== COMPLETE SPRINT TESTS ==========

    @Test
    @WithMockUser
    void completeSprint_WithValidId_ShouldReturn200() throws Exception {
        // Given
        SprintResponse completedResponse = SprintResponse.builder()
                .id(1)
                .name("Sprint 1")
                .status(SprintStatus.COMPLETED)
                .build();

        when(sprintService.completeSprint(1, testUser))
                .thenReturn(completedResponse);

        // When & Then
        mockMvc.perform(post("/api/sprints/1/complete")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        verify(sprintService).completeSprint(1, testUser);
    }

    // ========== CANCEL SPRINT TESTS ==========

    @Test
    @WithMockUser
    void cancelSprint_WithValidId_ShouldReturn200() throws Exception {
        // Given
        SprintResponse cancelledResponse = SprintResponse.builder()
                .id(1)
                .name("Sprint 1")
                .status(SprintStatus.CANCELLED)
                .build();

        when(sprintService.cancelSprint(1, testUser))
                .thenReturn(cancelledResponse);

        // When & Then
        mockMvc.perform(post("/api/sprints/1/cancel")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        verify(sprintService).cancelSprint(1, testUser);
    }

    // ========== DELETE SPRINT TESTS ==========

    @Test
    @WithMockUser
    void deleteSprint_WithValidId_ShouldReturn204() throws Exception {
        // Given
        doNothing().when(sprintService).deleteSprint(1, testUser);

        // When & Then
        mockMvc.perform(delete("/api/sprints/1")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(sprintService).deleteSprint(1, testUser);
    }

    // ========== GET ACTIVE SPRINT TESTS ==========

    @Test
    @WithMockUser
    void getActiveSprint_ForProject_ShouldReturn200() throws Exception {
        // Given
        SprintResponse activeResponse = SprintResponse.builder()
                .id(1)
                .name("Active Sprint")
                .status(SprintStatus.ACTIVE)
                .build();

        when(sprintService.getActiveSprint(1, testUser))
                .thenReturn(activeResponse);

        // When & Then
        mockMvc.perform(get("/api/sprints/projects/1/active")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(sprintService).getActiveSprint(1, testUser);
    }

    // ========== GET COMPLETED SPRINTS TESTS ==========

    @Test
    @WithMockUser
    void getCompletedSprints_ForProject_ShouldReturnList() throws Exception {
        // Given
        List<SprintResponse> completedSprints = Arrays.asList(
                SprintResponse.builder().id(1).name("Sprint 1").status(SprintStatus.COMPLETED).build(),
                SprintResponse.builder().id(2).name("Sprint 2").status(SprintStatus.COMPLETED).build()
        );

        when(sprintService.getCompletedSprints(1, testUser))
                .thenReturn(completedSprints);

        // When & Then
        mockMvc.perform(get("/api/sprints/projects/1/completed")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)));

        verify(sprintService).getCompletedSprints(1, testUser);
    }
}
