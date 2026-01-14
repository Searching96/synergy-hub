package com.synergyhub.service.board;

import com.synergyhub.domain.entity.Sprint;
import com.synergyhub.domain.entity.Task;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.enums.SprintStatus;
import com.synergyhub.dto.mapper.SprintMapper;
import com.synergyhub.dto.mapper.TaskMapper;
import com.synergyhub.dto.response.BoardViewResponse;
import com.synergyhub.dto.response.SprintDetailResponse;
import com.synergyhub.dto.response.TaskResponse;
import com.synergyhub.exception.ProjectNotFoundException;
import com.synergyhub.repository.ProjectRepository;
import com.synergyhub.repository.SprintRepository;
import com.synergyhub.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardService {

    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;
    private final SprintMapper sprintMapper;
    private final TaskMapper taskMapper;

    /**
     * Aggregates Active Sprints and Backlog for a single Board View.
     */
    @PreAuthorize("@projectSecurity.hasProjectAccess(#projectId, #currentUser)")
    @Transactional(readOnly = true)
    public BoardViewResponse getProjectBoard(Long projectId, User currentUser) {
        log.info("Fetching board view for project: {}", projectId);

        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        // 1. Fetch Active Sprints (with Tasks via fetch join in mapper or repo)
        // Note: Using a List to support potential future parallel sprints
        List<Sprint> activeSprints = sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE);
        
        List<SprintDetailResponse> sprintResponses = activeSprints.stream()
                .map(sprint -> {
                    // We need to ensure tasks are loaded. 
                    // If Lazy Loading is an issue, we should use a specific 'findByIdWithTasks' equivalent for Lists
                    // For now, assuming standard JPA access or Transactional context handles the lazy load of tasks
                    return sprintMapper.toSprintDetailResponse(sprint);
                })
                .collect(Collectors.toList());

        // 2. Fetch Backlog Tasks (Tasks with no sprint)
        List<Task> backlogTasks = taskRepository.findByProjectIdAndSprintIsNullOrderByPriorityDescCreatedAtAsc(projectId);
        List<TaskResponse> backlogResponses = taskMapper.toTaskResponseList(backlogTasks);

        return BoardViewResponse.builder()
                .activeSprints(sprintResponses)
                .backlogTasks(backlogResponses)
                .build();
    }
}