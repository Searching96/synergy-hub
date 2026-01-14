package com.synergyhub.service.security;

import com.synergyhub.domain.entity.AuditLog;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.enums.AuditEventType;
import com.synergyhub.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for creating audit log entries.
 * This is a WRITE-ONLY service following Command-Query Separation.
 * For reading audit logs, use AuditLogQueryService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Main entry point for logging.
     * Event listeners are responsible for formatting the 'eventDetails' string.
     * 
     * @param user The user who performed the action (null for system events)
     * @param eventType The type of event (e.g., "LOGIN_SUCCESS", "PROJECT_CREATED")
     * @param eventDetails Human-readable description of what happened
     * @param ipAddress IP address of the request origin
     * @param userAgent Browser/client user agent string
     * @param projectId Optional project context
     */
    @Transactional
    public void createAuditLog(User user, String eventType, String eventDetails, 
                               String ipAddress, String userAgent, Long projectId) {
        
        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .eventType(eventType)
                .eventDetails(eventDetails)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .projectId(projectId)
                .build();

        auditLogRepository.save(auditLog);
        
        // ✅ Structured logging for monitoring/debugging
        log.debug("Audit: [{}] {} | User: {} | Project: {} | IP: {}", 
                 eventType, 
                 eventDetails, 
                 user != null ? user.getEmail() : "SYSTEM",
                 projectId,
                 ipAddress);
    }

    // ========== CONVENIENCE OVERLOADS ==========
    
    /**
     * Log with AuditEventType enum (type-safe).
     */
    @Transactional
    public void createAuditLog(User user, AuditEventType eventType, String eventDetails, 
                               String ipAddress, String userAgent, Long projectId) {
        createAuditLog(user, eventType.name(), eventDetails, ipAddress, userAgent, projectId);
    }

    /**
     * Log without userAgent and projectId (common for auth events).
     */
    @Transactional
    public void createAuditLog(User user, String eventType, String eventDetails, String ipAddress) {
        createAuditLog(user, eventType, eventDetails, ipAddress, null, null);
    }
    
    /**
     * Log with projectId but no userAgent (common for project events).
     */
    @Transactional
    public void createAuditLog(User user, String eventType, String eventDetails, 
                               String ipAddress, Long projectId) {
        createAuditLog(user, eventType, eventDetails, ipAddress, null, projectId);
    }

    /**
     * Log with AuditEventType enum and no optional fields.
     */
    @Transactional
    public void createAuditLog(User user, AuditEventType eventType, String eventDetails, String ipAddress) {
        createAuditLog(user, eventType.name(), eventDetails, ipAddress, null, null);
    }
}