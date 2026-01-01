package com.synergyhub.service.security;

import com.synergyhub.dto.mapper.ActivityLogMapper;
import com.synergyhub.dto.response.ActivityLogResponse;
import com.synergyhub.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for querying audit log entries.
 * This is a READ-ONLY service following Command-Query Separation.
 * For creating audit logs, use AuditLogService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final ActivityLogMapper activityLogMapper;

    /**
     * Get activity logs for a specific user.
     */
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getUserActivityLogs(Integer userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        var logs = auditLogRepository.findByUserId(userId, pageable);
        return activityLogMapper.toResponseList(logs);
    }

    /**
     * Get activity logs for a specific project.
     */
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getProjectActivityLogs(Integer projectId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        var logs = auditLogRepository.findByProjectId(projectId, pageable);
        return activityLogMapper.toResponseList(logs);
    }

    /**
     * Get all activity logs (admin only).
     */
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getAllActivityLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        var logs = auditLogRepository.findAll(pageable).getContent();
        return activityLogMapper.toResponseList(logs);
    }

    /**
     * Get recent activity for a user (last 24 hours).
     */
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getRecentUserActivity(Integer userId) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        var logs = auditLogRepository.findByUserIdAndTimestampAfter(userId, since);
        return activityLogMapper.toResponseList(logs);
    }

    /**
     * Get system events (no user associated).
     */
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getSystemEvents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        var logs = auditLogRepository.findByUserIsNull(pageable);
        return activityLogMapper.toResponseList(logs);
    }

    /**
     * Get activity logs by event type.
     */
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getLogsByEventType(String eventType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        var logs = auditLogRepository.findByEventType(eventType, pageable);
        return activityLogMapper.toResponseList(logs);
    }

    /**
     * Security investigation: Get all activity from a specific IP address.
     */
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getActivityByIpAddress(String ipAddress, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        var logs = auditLogRepository.findByIpAddress(ipAddress, pageable);
        return activityLogMapper.toResponseList(logs);
    }

    /**
     * Get activity in a date range.
     */
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getActivityInRange(LocalDateTime start, LocalDateTime end, 
                                                         int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        var logs = auditLogRepository.findByTimestampBetween(start, end, pageable);
        return activityLogMapper.toResponseList(logs);
    }
}