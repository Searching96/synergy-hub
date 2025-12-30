package com.synergyhub.service.security;

import com.synergyhub.domain.entity.AuditLog;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.enums.AuditEventType;
import com.synergyhub.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Main entry point for logging.
     * Listeners are responsible for formatting the 'details' string.
     */
    @Transactional
    public void createAuditLog(User user, String eventType, String details, 
                               String ipAddress, String userAgent, Integer projectId) {
        
        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .eventType(eventType)
                .eventDetails(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .projectId(projectId)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit: [{}] {} (Project: {})", eventType, details, projectId);
    }

    // --- Convenience Overloads (Optional wrappers to save null passing) ---

    @Transactional
    public void createAuditLog(User user, AuditEventType eventType, String details, String ipAddress) {
        createAuditLog(user, eventType.name(), details, ipAddress, null, null);
    }

    @Transactional
    public void createAuditLog(User user, String eventType, String details, String ipAddress) {
        createAuditLog(user, eventType, details, ipAddress, null, null);
    }
    
    @Transactional
    public void createAuditLog(User user, String eventType, String details, String ipAddress, Integer projectId) {
        createAuditLog(user, eventType, details, ipAddress, null, projectId);
    }
}