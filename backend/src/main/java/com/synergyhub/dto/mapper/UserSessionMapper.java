package com.synergyhub.dto.mapper;

import com.synergyhub.domain.entity.UserSession;
import com.synergyhub.dto.response.UserSessionResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UserSessionMapper {
    
    public UserSessionResponse toDTO(UserSession session, String currentTokenId) {
        return UserSessionResponse.builder()
                .sessionId(maskTokenId(session.getTokenId()))
                .deviceInfo(parseUserAgent(session.getUserAgent()))
                .location(getApproximateLocation(session.getIpAddress()))
                .createdAt(session.getCreatedAt())
                .lastAccessedAt(session.getLastAccessedAt())
                .expiresAt(session.getExpiresAt())
                .isCurrentSession(session.getTokenId().equals(currentTokenId))
                .isActive(!session.getRevoked() && session.getExpiresAt().isAfter(LocalDateTime.now()))
                .build();
    }
    
    private String maskTokenId(String tokenId) {
        // Show only last 8 characters
        if (tokenId == null || tokenId.length() < 12) return "****";
        return "..." + tokenId.substring(tokenId.length() - 8);
    }
    
    private String parseUserAgent(String userAgent) {
        // Simple parsing - use a library like UAParser for production
        if (userAgent == null) return "Unknown Device";
        
        String browser = "Unknown";
        String os = "Unknown";
        
        if (userAgent.contains("Chrome")) browser = "Chrome";
        else if (userAgent.contains("Firefox")) browser = "Firefox";
        else if (userAgent.contains("Safari")) browser = "Safari";
        
        if (userAgent.contains("Windows")) os = "Windows";
        else if (userAgent.contains("Mac")) os = "macOS";
        else if (userAgent.contains("Linux")) os = "Linux";
        else if (userAgent.contains("Android")) os = "Android";
        else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) os = "iOS";
        
        return browser + " on " + os;
    }
    
    private String getApproximateLocation(String ipAddress) {
        // For production, use MaxMind GeoIP2 or similar
        // For now, return generic info
        return "Vietnam"; // Or use IP geolocation API
    }
}