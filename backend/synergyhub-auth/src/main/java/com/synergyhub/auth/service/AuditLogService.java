package com.synergyhub.auth.service;

import com.synergyhub.auth.entity.AuditLog;
import com.synergyhub.auth.entity.User;
import com.synergyhub.auth.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional
    public void log(User user, String eventType, String eventDetails, String ipAddress, String userAgent) {
        AuditLog log = AuditLog.builder()
            .user(user)
            .eventType(eventType)
            .eventDetails(eventDetails)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();
        
        auditLogRepository.save(log);
    }
}