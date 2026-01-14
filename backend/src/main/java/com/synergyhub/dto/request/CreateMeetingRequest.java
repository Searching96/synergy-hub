package com.synergyhub.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateMeetingRequest {
    private String title;
    private String description;
    private LocalDateTime scheduledAt;
    private Long projectId;
    private Boolean isInstant; // true = start now
}
