package com.synergyhub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {
    private Integer id;
    private Integer taskId;
    private String content;
    private LocalDateTime createdAt;
    
    // User info for display
    private Integer userId;
    private String userName;
    // Optional: Add avatarUrl if you have it
}