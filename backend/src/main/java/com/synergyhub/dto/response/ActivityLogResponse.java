package com.synergyhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class ActivityLogResponse {
    private Long id;
    
    // Actor information (can be null for system events)
    private Long actorId;
    private String actorName;
    private String actorEmail;
    
    // ✅ Event details (aligned with entity)
    private String eventType;    // Was "action"
    private String eventDetails;  // Was "details"
    
    // Context
    private String ipAddress;
    private String userAgent;
    private Long projectId;
    
    // Timestamp
    private LocalDateTime timestamp;
    
    // Computed field
    private boolean systemEvent;
}