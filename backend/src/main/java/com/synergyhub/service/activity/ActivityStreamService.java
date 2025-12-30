package com.synergyhub.service.activity;

import com.synergyhub.domain.entity.AuditLog;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.mapper.ActivityLogMapper;
import com.synergyhub.dto.response.ActivityLogResponse;
import com.synergyhub.exception.ProjectNotFoundException;
import com.synergyhub.repository.AuditLogRepository;
import com.synergyhub.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityStreamService {

    private final AuditLogRepository auditLogRepository;
    private final ProjectRepository projectRepository;
    private final ActivityLogMapper activityLogMapper;

    @PreAuthorize("@projectSecurity.hasProjectAccess(#projectId, #currentUser)")
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getProjectActivity(Integer projectId, User currentUser, int page, int size) {
        log.info("Fetching activity stream for project: {}", projectId);

        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        Pageable pageable = PageRequest.of(page, size);
        
        // Fetch logs (using Option B from repository: Activity of all project members)
        Page<AuditLog> logs = auditLogRepository.findProjectActivity(projectId, pageable);

        return activityLogMapper.toResponseList(logs.getContent());
    }
}