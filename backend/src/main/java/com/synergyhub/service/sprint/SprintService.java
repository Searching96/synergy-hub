package com.synergyhub.service.sprint;

import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.Sprint;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.enums.SprintStatus;
import com.synergyhub.dto.mapper.SprintMapper;
import com.synergyhub.dto.request.CreateSprintRequest;
import com.synergyhub.dto.request.UpdateSprintRequest;
import com.synergyhub.dto.response.SprintDetailResponse;
import com.synergyhub.dto.response.SprintResponse;
import com.synergyhub.exception.*;
import com.synergyhub.repository.ProjectMemberRepository;
import com.synergyhub.repository.ProjectRepository;
import com.synergyhub.repository.SprintRepository;
import com.synergyhub.service.security.AuditLogService;  // ✅ Added
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final SprintMapper sprintMapper;
    private final AuditLogService auditLogService;  // ✅ Added

    @Transactional
    public SprintResponse createSprint(CreateSprintRequest request, User currentUser) {
        log.info("Creating sprint: {} in project: {} by user: {}",
                request.getName(), request.getProjectId(), currentUser.getId());

        // Verify project exists
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ProjectNotFoundException(request.getProjectId()));

        // Verify user has access to project
        verifyProjectAccess(project, currentUser);

        // Validate sprint dates
        validateSprintDates(request.getStartDate(), request.getEndDate());

        // Check for active sprint
        Optional<Sprint> activeSprint = sprintRepository.findActiveSprintByProjectId(project.getId());
        if (activeSprint.isPresent()) {
            // ✅ Audit log for failed creation
            auditLogService.createAuditLog(
                    currentUser,
                    "SPRINT_CREATION_FAILED",
                    String.format("Failed to create sprint '%s': Project '%s' already has active sprint '%s'",
                            request.getName(), project.getName(), activeSprint.get().getName()),
                    null,
                    null
            );
            throw new SprintAlreadyActiveException(project.getName());
        }

        // Check for overlapping sprints
        List<Sprint> overlappingSprints = sprintRepository.findOverlappingSprints(
                project.getId(), request.getStartDate(), request.getEndDate());
        if (!overlappingSprints.isEmpty()) {
            // ✅ Audit log for overlapping dates
            auditLogService.createAuditLog(
                    currentUser,
                    "SPRINT_CREATION_FAILED",
                    String.format("Failed to create sprint '%s': Dates overlap with sprint '%s'",
                            request.getName(), overlappingSprints.get(0).getName()),
                    null,
                    null
            );
            throw new BadRequestException("Sprint dates overlap with existing sprint: " +
                    overlappingSprints.get(0).getName());
        }

        // Create sprint
        Sprint sprint = Sprint.builder()
                .name(request.getName())
                .goal(request.getGoal())
                .project(project)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(SprintStatus.PLANNING)
                .build();

        Sprint savedSprint = sprintRepository.save(sprint);
        log.info("Sprint created successfully: {}", savedSprint.getId());

        // ✅ Audit log for sprint creation
        auditLogService.createAuditLog(
                currentUser,
                "SPRINT_CREATED",
                String.format("Sprint '%s' (ID: %d) created in project '%s' (%s to %s)",
                        savedSprint.getName(), savedSprint.getId(), project.getName(),
                        savedSprint.getStartDate(), savedSprint.getEndDate()),
                null,
                null
        );

        return sprintMapper.toSprintResponse(savedSprint);
    }

    @Transactional(readOnly = true)
    public SprintResponse getSprintById(Integer sprintId, User currentUser) {
        log.info("Getting sprint: {} for user: {}", sprintId, currentUser.getId());

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));

        verifyProjectAccess(sprint.getProject(), currentUser);

        return sprintMapper.toSprintResponse(sprint);
    }

    @Transactional(readOnly = true)
    public SprintDetailResponse getSprintDetails(Integer sprintId, User currentUser) {
        log.info("Getting sprint details: {} for user: {}", sprintId, currentUser.getId());

        Sprint sprint = sprintRepository.findByIdWithTasks(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));

        verifyProjectAccess(sprint.getProject(), currentUser);

        return sprintMapper.toSprintDetailResponse(sprint);
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> getSprintsByProject(Integer projectId, User currentUser) {
        log.info("Getting sprints for project: {} by user: {}", projectId, currentUser.getId());

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        verifyProjectAccess(project, currentUser);

        List<Sprint> sprints = sprintRepository.findByProjectIdOrderByStartDateDesc(projectId);
        return sprintMapper.toSprintResponseList(sprints);
    }

    @Transactional(readOnly = true)
    public SprintResponse getActiveSprint(Integer projectId, User currentUser) {
        log.info("Getting active sprint for project: {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        verifyProjectAccess(project, currentUser);

        Sprint activeSprint = sprintRepository.findActiveSprintByProjectId(projectId)
                .orElseThrow(() -> new SprintNotFoundException("No active sprint found for project"));

        return sprintMapper.toSprintResponse(activeSprint);
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> getCompletedSprints(Integer projectId, User currentUser) {
        log.info("Getting completed sprints for project: {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        verifyProjectAccess(project, currentUser);

        List<Sprint> completedSprints = sprintRepository.findCompletedSprintsByProjectId(projectId);
        return sprintMapper.toSprintResponseList(completedSprints);
    }

    @Transactional
    public SprintResponse updateSprint(Integer sprintId, UpdateSprintRequest request, User currentUser) {
        log.info("Updating sprint: {} by user: {}", sprintId, currentUser.getId());

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));

        verifyProjectAccess(sprint.getProject(), currentUser);

        // Don't allow updating completed or cancelled sprints
        if (sprint.getStatus() == SprintStatus.COMPLETED) {
            // ✅ Audit log for failed update
            auditLogService.createAuditLog(
                    currentUser,
                    "SPRINT_UPDATE_FAILED",
                    String.format("Failed to update sprint '%s' (ID: %d): Sprint is completed",
                            sprint.getName(), sprintId),
                    null,
                    null
            );
            throw new InvalidSprintStateException("Cannot update completed sprint");
        }
        if (sprint.getStatus() == SprintStatus.CANCELLED) {
            // ✅ Audit log for failed update
            auditLogService.createAuditLog(
                    currentUser,
                    "SPRINT_UPDATE_FAILED",
                    String.format("Failed to update sprint '%s' (ID: %d): Sprint is cancelled",
                            sprint.getName(), sprintId),
                    null,
                    null
            );
            throw new InvalidSprintStateException("Cannot update cancelled sprint");
        }

        // ✅ Track changes
        StringBuilder changes = new StringBuilder();

        // Update fields
        if (request.getName() != null && !request.getName().equals(sprint.getName())) {
            changes.append(String.format("Name: '%s' → '%s'; ", sprint.getName(), request.getName()));
            sprint.setName(request.getName());
        }
        if (request.getGoal() != null && !request.getGoal().equals(sprint.getGoal())) {
            changes.append(String.format("Goal updated; "));
            sprint.setGoal(request.getGoal());
        }
        if (request.getStartDate() != null && !request.getStartDate().equals(sprint.getStartDate())) {
            changes.append(String.format("Start: %s → %s; ", sprint.getStartDate(), request.getStartDate()));
            sprint.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null && !request.getEndDate().equals(sprint.getEndDate())) {
            changes.append(String.format("End: %s → %s; ", sprint.getEndDate(), request.getEndDate()));
            sprint.setEndDate(request.getEndDate());
        }

        // Validate dates if they were updated
        if (request.getStartDate() != null || request.getEndDate() != null) {
            validateSprintDates(sprint.getStartDate(), sprint.getEndDate());
        }

        Sprint updatedSprint = sprintRepository.save(sprint);
        log.info("Sprint updated successfully: {}", sprintId);

        // ✅ Audit log for sprint update
        if (changes.length() > 0) {
            auditLogService.createAuditLog(
                    currentUser,
                    "SPRINT_UPDATED",
                    String.format("Sprint '%s' (ID: %d) updated: %s",
                            sprint.getName(), sprintId, changes.toString()),
                    null,
                    null
            );
        }

        return sprintMapper.toSprintResponse(updatedSprint);
    }

    @Transactional
    public SprintResponse startSprint(Integer sprintId, User currentUser) {
        log.info("Starting sprint: {} by user: {}", sprintId, currentUser.getId());

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));

        verifyProjectAccess(sprint.getProject(), currentUser);

        // Check if sprint can be started
        if (!sprint.canBeStarted()) {
            // ✅ Audit log for failed start
            auditLogService.createAuditLog(
                    currentUser,
                    "SPRINT_START_FAILED",
                    String.format("Failed to start sprint '%s' (ID: %d): Already %s",
                            sprint.getName(), sprintId, sprint.getStatus()),
                    null,
                    null
            );
            throw new InvalidSprintStateException("Sprint is already " + sprint.getStatus());
        }

        // Check if another sprint is active
        Optional<Sprint> activeSprint = sprintRepository.findActiveSprintByProjectId(sprint.getProject().getId());
        if (activeSprint.isPresent() && !activeSprint.get().getId().equals(sprintId)) {
            // ✅ Audit log for conflicting active sprint
            auditLogService.createAuditLog(
                    currentUser,
                    "SPRINT_START_FAILED",
                    String.format("Failed to start sprint '%s' (ID: %d): Sprint '%s' (ID: %d) is already active",
                            sprint.getName(), sprintId, activeSprint.get().getName(), activeSprint.get().getId()),
                    null,
                    null
            );
            throw new SprintAlreadyActiveException(
                    sprint.getProject().getId(),
                    activeSprint.get().getId()
            );
        }

        sprint.setStatus(SprintStatus.ACTIVE);
        Sprint updatedSprint = sprintRepository.save(sprint);

        log.info("Sprint started successfully: {}", sprintId);

        // ✅ Audit log for sprint start
        auditLogService.createAuditLog(
                currentUser,
                "SPRINT_STARTED",
                String.format("Sprint '%s' (ID: %d) started in project '%s'",
                        sprint.getName(), sprintId, sprint.getProject().getName()),
                null,
                null
        );

        return sprintMapper.toSprintResponse(updatedSprint);
    }

    @Transactional
    public SprintResponse completeSprint(Integer sprintId, User currentUser) {
        log.info("Completing sprint: {} by user: {}", sprintId, currentUser.getId());

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));

        verifyProjectAccess(sprint.getProject(), currentUser);

        // Check if sprint can be completed
        if (!sprint.canBeCompleted()) {
            // ✅ Audit log for failed completion
            auditLogService.createAuditLog(
                    currentUser,
                    "SPRINT_COMPLETE_FAILED",
                    String.format("Failed to complete sprint '%s' (ID: %d): Only active sprints can be completed (current: %s)",
                            sprint.getName(), sprintId, sprint.getStatus()),
                    null,
                    null
            );
            throw new InvalidSprintStateException("Only active sprints can be completed");
        }

        sprint.setStatus(SprintStatus.COMPLETED);
        Sprint updatedSprint = sprintRepository.save(sprint);

        log.info("Sprint completed successfully: {}", sprintId);

        // ✅ Audit log for sprint completion
        auditLogService.createAuditLog(
                currentUser,
                "SPRINT_COMPLETED",
                String.format("Sprint '%s' (ID: %d) completed in project '%s'",
                        sprint.getName(), sprintId, sprint.getProject().getName()),
                null,
                null
        );

        return sprintMapper.toSprintResponse(updatedSprint);
    }

    @Transactional
    public SprintResponse cancelSprint(Integer sprintId, User currentUser) {
        log.info("Cancelling sprint: {} by user: {}", sprintId, currentUser.getId());

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));

        verifyProjectAccess(sprint.getProject(), currentUser);

        // Don't allow cancelling completed sprints
        if (sprint.getStatus() == SprintStatus.COMPLETED) {
            // ✅ Audit log for failed cancellation
            auditLogService.createAuditLog(
                    currentUser,
                    "SPRINT_CANCEL_FAILED",
                    String.format("Failed to cancel sprint '%s' (ID: %d): Sprint is completed",
                            sprint.getName(), sprintId),
                    null,
                    null
            );
            throw new InvalidSprintStateException("Cannot cancel completed sprint");
        }
        if (sprint.getStatus() == SprintStatus.CANCELLED) {
            // ✅ Audit log for already cancelled
            auditLogService.createAuditLog(
                    currentUser,
                    "SPRINT_CANCEL_FAILED",
                    String.format("Failed to cancel sprint '%s' (ID: %d): Already cancelled",
                            sprint.getName(), sprintId),
                    null,
                    null
            );
            throw new InvalidSprintStateException("Sprint is already cancelled");
        }

        sprint.setStatus(SprintStatus.CANCELLED);
        Sprint updatedSprint = sprintRepository.save(sprint);

        log.info("Sprint cancelled successfully: {}", sprintId);

        // ✅ Audit log for sprint cancellation
        auditLogService.createAuditLog(
                currentUser,
                "SPRINT_CANCELLED",
                String.format("Sprint '%s' (ID: %d) cancelled in project '%s'",
                        sprint.getName(), sprintId, sprint.getProject().getName()),
                null,
                null
        );

        return sprintMapper.toSprintResponse(updatedSprint);
    }

    @Transactional
    public void deleteSprint(Integer sprintId, User currentUser) {
        log.info("Deleting sprint: {} by user: {}", sprintId, currentUser.getId());

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));

        verifyProjectAccess(sprint.getProject(), currentUser);

        // Only allow deleting planning or cancelled sprints
        if (!sprint.canBeDeleted()) {
            // ✅ Audit log for failed deletion
            auditLogService.createAuditLog(
                    currentUser,
                    "SPRINT_DELETE_FAILED",
                    String.format("Failed to delete sprint '%s' (ID: %d): Cannot delete %s sprint",
                            sprint.getName(), sprintId, sprint.getStatus()),
                    null,
                    null
            );
            throw new InvalidSprintStateException("Cannot delete active or completed sprint");
        }

        String sprintName = sprint.getName();
        String projectName = sprint.getProject().getName();

        sprintRepository.delete(sprint);
        log.info("Sprint deleted successfully: {}", sprintId);

        // ✅ Audit log for sprint deletion
        auditLogService.createAuditLog(
                currentUser,
                "SPRINT_DELETED",
                String.format("Sprint '%s' (ID: %d) deleted from project '%s'",
                        sprintName, sprintId, projectName),
                null,
                null
        );
    }

    // ========== HELPER METHODS ==========

    private void verifyProjectAccess(Project project, User user) {
        if (!projectMemberRepository.hasAccessToTaskProject(project.getId(), user.getId())) {
            // ✅ Audit log for access denied
            auditLogService.createAuditLog(
                    user,
                    "SPRINT_ACCESS_DENIED",
                    String.format("Access denied to project '%s' (ID: %d)",
                            project.getName(), project.getId()),
                    null,
                    null
            );
            throw new AccessDeniedException("You don't have access to this project");
        }
    }

    private void validateSprintDates(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();

        // Start date validation
        if (startDate.isBefore(today)) {
            throw new BadRequestException("Start date cannot be in the past");
        }

        // End date validation
        if (endDate.isBefore(today)) {
            throw new BadRequestException("End date cannot be in the past");
        }

        // Date range validation
        if (endDate.isBefore(startDate) || endDate.isEqual(startDate)) {
            throw new BadRequestException("End date must be after start date");
        }

        // Optional: Sprint duration validation
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween < 7) {
            throw new BadRequestException("Sprint must be at least 7 days long");
        }
        if (daysBetween > 30) {
            throw new BadRequestException("Sprint cannot exceed 30 days");
        }
    }
}
