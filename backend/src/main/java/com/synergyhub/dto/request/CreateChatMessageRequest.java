package com.synergyhub.dto.request;

import lombok.Data;

@Data
public class CreateChatMessageRequest {
    private String content;
    private Long channelId;
    private Long projectId; // optional if channelId not known yet (auto-create general channel)
}
