package com.synergyhub.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageResponse {
    private Long id;
    private String content;
    private LocalDateTime sentAt;
    private Long userId;
    private String userName;
    private String userAvatar; // Optional
    private Long channelId;
}
