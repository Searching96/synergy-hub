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
import com.synergyhub.repository.ProjectRepository;
import com.synergyhub.repository.SprintRepository;
import com.synergyhub.service.security.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final SprintMapper sprintMapper;
    private final AuditLogService auditLogService;

    @PreAuthorize("@projectSecurity.hasProjectAccess(#request.projectId, #currentUser)")
    @Transactional
    public SprintResponse createSprint(CreateSprintRequest request, User currentUser) {
        log.info("Creating sprint: {} in project: {} by user: {}",
                request.getName(), request.getProjectId(), currentUser.getId());

        // Verify project exists
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ProjectNotFoundException(request.getProjectId()));

        // Validate sprint dates
        try {
            SprintValidator.validateSprintDates(request.getStartDate(), request.getEndDate());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }

        // Check for active sprint
        Optional<Sprint> activeSprint = sprintRepository.findActiveSprintByProjectId(project.getId());
        if (activeSprint.isPresent()) {
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

    @PreAuthorize("@projectSecurity.hasSprintAccess(#sprintId, #currentUser)")
    @Transactional(readOnly = true)
    public SprintResponse getSprintById(Integer sprintId, User currentUser) {
        log.info("Getting sprint: {} for user: {}", sprintId, currentUser.getId());

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));

        return sprintMapper.toSprintResponse(sprint);
    }

    @PreAuthorize("@projectSecurity.hasSprintAccess(#sprintId, #currentUser)")
    @Transactional(readOnly = true)
    public SprintDetailResponse getSprintDetails(Integer sprintId, User currentUser) {
        log.info("Getting sprint details: {} for user: {}", sprintId, currentUser.getId());

        Sprint sprint = sprintRepository.findByIdWithTasks(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));

        return sprintMapper.toSprintDetailResponse(sprint);
    }

    @PreAuthorize("@projectSecurity.hasProjectAccess(#projectId, #currentUser)")
    @Transactional(readOnly = true)
    public List<SprintResponse> getSprintsByProject(Integer projectId, User currentUser) {
        log.info("Getting sprints for project: {} by user: {}", projectId, currentUser.getId());

        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        List<Sprint> sprints = sprintRepository.findByProjectIdOrderByStartDateDesc(projectId);
        return sprintMapper.toSprintResponseList(sprints);
    }

    @PreAuthorize("@projectSecurity.hasProjectAccess(#projectId, #currentUser)")
    @Transactional(readOnly = true)
    public SprintResponse getActiveSprint(Integer projectId, User currentUser) {
        log.info("Getting active sprint for project: {}", projectId);

        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        Sprint activeSprint = sprintRepository.findActiveSprintByProjectId(projectId)
                .orElseThrow(() -> new SprintNotFoundException("No active sprint found for project"));

        return sprintMapper.toSprintResponse(activeSprint);
    }

    @PreAuthorize("@projectSecurity.hasProjectAccess(#projectId, #currentUser)")
    @Transactional(readOnly = true)
    public List<SprintResponse> getCompletedSprints(Integer projectId, User currentUser) {
        log.info("Getting completed sprints for project: {}", projectId);

        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        List<Sprint> completedSprints = sprintRepository.findCompletedSprintsByProjectId(projectId);
        return sprintMapper.toSprintResponseList(completedSprints);
    }

    @PreAuthorize("@projectSecurity.hasSprintAccess(#sprintId, #currentUser)")
    @Transactional
    public SprintResponse updateSprint(Integer sprintId, UpdateSprintRequest request, User currentUser) {
        log.info("Updating sprint: {} by user: {}", sprintId, currentUser.getId());

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));

        // Don't allow updating completed or cancelled sprints
        if (sprint.getStatus() == SprintStatus.COMPLETED) {
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

        // Track changes
        StringBuilder changes = new StringBuilder();

        // Update fields
        if (request.getName() != null && !request.getName().equals(sprint.getName())) {
            changes.append(String.format("Name: '%s' → '%s'; ", sprint.getName(), request.getName()));
            sprint.setName(request.getName());
        }
        if (request.getGoal() != null && !request.getGoal().equals(sprint.getGoal())) {
            changes.append("Goal updated; ");
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
            try {
                SprintValidator.validateSprintDates(sprint.getStartDate(), sprint.getEndDate());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException(ex.getMessage());
            }
        }

        Sprint updatedSprint = sprintRepository.save(sprint);
        log.info("Sprint updated successfully: {}", sprintId);

        if (!changes.isEmpty()) {
            auditLogService.createAuditLog(
                currentUser,
                "SPRINT_UPDATED",
                String.format("Sprint '%s' (ID: %d) updated: %s",
                        sprint.getName(), sprintId, changes),
                null,
                null
            );
        }

        return sprintMapper.toSprintResponse(updatedSprint);
    }

    @PreAuthorize("@projectSecurity.hasSprintAccess(#sprintId, #currentUser)")
    @Transactional
    public SprintResponse startSprint(Integer sprintId, User currentUser) {
        log.info("Starting sprint: {} by user: {}", sprintId, currentUser.getId());

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));

        // Check if sprint can be started
        if (!sprint.canBeStarted()) {
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

    @PreAuthorize("@projectSecurity.hasSprintAccess(#sprintId, #currentUser)")
    @Transactional
    public SprintResponse completeSprint(Integer sprintId, User currentUser) {
        log.info("Completing sprint: {} by user: {}", sprintId, currentUser.getId());

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));

        // Check if sprint can be completed
        if (!sprint.canBeCompleted()) {
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

    @PreAuthorize("@projectSecurity.hasSprintAccess(#sprintId, #currentUser)")
    @Transactional
    public SprintResponse cancelSprint(Integer sprintId, User currentUser) {
        log.info("Cancelling sprint: {} by user: {}", sprintId, currentUser.getId());

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));

        if (sprint.getStatus() == SprintStatus.COMPLETED) {
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

    @PreAuthorize("@projectSecurity.hasSprintAccess(#sprintId, #currentUser)")
    @Transactional
    public void deleteSprint(Integer sprintId, User currentUser) {
        log.info("Deleting sprint: {} by user: {}", sprintId, currentUser.getId());

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));

        if (!sprint.canBeDeleted()) {
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

        auditLogService.createAuditLog(
            currentUser,
            "SPRINT_DELETED",
            String.format("Sprint '%s' (ID: %d) deleted from project '%s'",
                    sprintName, sprintId, projectName),
            null,
            null
        );
    }
}