package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.CreateChatMessageRequest;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.ChatMessageResponse;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.ChatService;
import com.synergyhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @RequestBody CreateChatMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        ChatMessageResponse message = chatService.sendMessage(request, user);
        
        // Broadcast to WebSocket subscribers
        String destination = "/topic/project/" + request.getProjectId() + "/chat";
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Broadcasted chat message to {}", destination);
        
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @GetMapping("/project/{projectId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getProjectMessages(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(
                chatService.getProjectMessages(projectId)
        ));
    }
}

