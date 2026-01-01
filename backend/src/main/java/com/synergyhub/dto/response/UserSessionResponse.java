package com.synergyhub.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserSessionResponse {
    private String sessionId;          // Masked or partial tokenId
    private String deviceInfo;         // Parsed user agent (e.g., "Chrome on Windows")
    private String location;           // Approximate location (e.g., "Ho Chi Minh City, VN")
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime expiresAt;
    private boolean isCurrentSession;  // Highlight the current session
    private boolean isActive;          // !revoked && !expired
}