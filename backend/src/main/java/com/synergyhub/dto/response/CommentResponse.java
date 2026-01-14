package com.synergyhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private Long taskId;
    private String content;
    private LocalDateTime createdAt;
    
    // User info for display
    private Long userId;
    private String userName;
    // Optional: Add avatarUrl if you have it
}