# WebSocket Chatroom Backend Implementation Plan

## Overview
This document outlines the complete implementation plan for adding real-time chatroom functionality to the SynergyHub project using WebSocket technology. The implementation will provide real-time messaging, typing indicators, message reactions, and online status for project team members.

---

## Table of Contents
1. [Technology Stack](#technology-stack)
2. [Database Schema](#database-schema)
3. [WebSocket Configuration](#websocket-configuration)
4. [Entity Design](#entity-design)
5. [Repository Layer](#repository-layer)
6. [Service Layer](#service-layer)
7. [WebSocket Controllers](#websocket-controllers)
8. [REST API Endpoints](#rest-api-endpoints)
9. [Security & Authentication](#security--authentication)
10. [Real-Time Features](#real-time-features)
11. [Performance Optimization](#performance-optimization)
12. [Testing Strategy](#testing-strategy)
13. [Deployment Considerations](#deployment-considerations)

---

## 1. Technology Stack

### Core Technologies
- **Spring Boot WebSocket**: For WebSocket connection management
- **STOMP Protocol**: Simple Text Oriented Messaging Protocol over WebSocket
- **SockJS**: WebSocket fallback for browsers that don't support WebSocket
- **Redis**: For distributed message broadcasting and session management
- **PostgreSQL**: Primary database for message persistence
- **Spring Data JPA**: ORM for database operations

### Dependencies (pom.xml)
```xml
<dependencies>
    <!-- WebSocket Support -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    
    <!-- Redis for distributed messaging -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- Spring Messaging -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
    
    <!-- Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- For JSON processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

---

## 2. Database Schema

### Chat Message Table
```sql
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    reply_to_message_id BIGINT,
    is_edited BOOLEAN DEFAULT FALSE,
    edited_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_chat_project FOREIGN KEY (project_id) 
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_reply FOREIGN KEY (reply_to_message_id) 
        REFERENCES chat_messages(id) ON DELETE SET NULL
);

-- Indexes for performance
CREATE INDEX idx_chat_messages_project_id ON chat_messages(project_id);
CREATE INDEX idx_chat_messages_user_id ON chat_messages(user_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at DESC);
CREATE INDEX idx_chat_messages_project_created ON chat_messages(project_id, created_at DESC);
```

### Message Reactions Table
```sql
CREATE TABLE chat_message_reactions (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    emoji VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_reaction_message FOREIGN KEY (message_id) 
        REFERENCES chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_reaction_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Prevent duplicate reactions (same user + emoji on same message)
    CONSTRAINT uk_reaction_unique UNIQUE (message_id, user_id, emoji)
);

-- Indexes
CREATE INDEX idx_reactions_message_id ON chat_message_reactions(message_id);
CREATE INDEX idx_reactions_user_id ON chat_message_reactions(user_id);
```

### User Presence Table (Optional - for online status)
```sql
CREATE TABLE user_presence (
    user_id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL, -- ONLINE, AWAY, OFFLINE
    last_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_presence_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_presence_project FOREIGN KEY (project_id) 
        REFERENCES projects(id) ON DELETE CASCADE
);

-- Index for quick lookups
CREATE INDEX idx_presence_project_id ON user_presence(project_id);
```

---

## 3. WebSocket Configuration

### WebSocketConfig.java
```java
package com.synergyhub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker
        // Prefix for messages FROM server TO client
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefix for messages FROM client TO server
        config.setApplicationDestinationPrefixes("/app");
        
        // Enable user-specific messaging
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add interceptor for authentication
        registration.interceptors(new UserInterceptor());
    }
}
```

### UserInterceptor.java (Authentication)
```java
package com.synergyhub.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

public class UserInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = 
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract JWT token from headers
            String authToken = accessor.getFirstNativeHeader("Authorization");
            
            if (authToken != null && authToken.startsWith("Bearer ")) {
                String token = authToken.substring(7);
                
                // Validate token and extract user
                Authentication auth = validateToken(token);
                accessor.setUser(auth);
            }
        }
        
        return message;
    }
    
    private Authentication validateToken(String token) {
        // Implement JWT validation logic
        // Return UsernamePasswordAuthenticationToken with user details
        return null; // TODO: Implement
    }
}
```

### Redis Configuration (for distributed systems)
```java
package com.synergyhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        return template;
    }
    
    @Bean
    public ChannelTopic chatTopic() {
        return new ChannelTopic("chat-messages");
    }
    
    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
```

---

## 4. Entity Design

### ChatMessage.java
```java
package com.synergyhub.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;
    
    @Column(name = "is_edited")
    private Boolean isEdited = false;
    
    @Column(name = "edited_at")
    private LocalDateTime editedAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id", insertable = false, updatable = false)
    private ChatMessage replyToMessage;
    
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessageReaction> reactions = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

### ChatMessageReaction.java
```java
package com.synergyhub.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message_reactions")
@Data
public class ChatMessageReaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "message_id", nullable = false)
    private Long messageId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(nullable = false, length = 10)
    private String emoji;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", insertable = false, updatable = false)
    private ChatMessage message;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

### UserPresence.java
```java
package com.synergyhub.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_presence")
@Data
public class UserPresence {
    
    @Id
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PresenceStatus status = PresenceStatus.OFFLINE;
    
    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen = LocalDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;
    
    public enum PresenceStatus {
        ONLINE, AWAY, OFFLINE
    }
}
```

---

## 5. Repository Layer

### ChatMessageRepository.java
```java
package com.synergyhub.repository;

import com.synergyhub.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    // Get paginated messages for a project
    Page<ChatMessage> findByProjectIdOrderByCreatedAtDesc(
        Long projectId, Pageable pageable);
    
    // Get messages after a specific timestamp (for real-time sync)
    List<ChatMessage> findByProjectIdAndCreatedAtAfterOrderByCreatedAtAsc(
        Long projectId, LocalDateTime after);
    
    // Search messages
    @Query("SELECT m FROM ChatMessage m WHERE m.projectId = :projectId " +
           "AND LOWER(m.message) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<ChatMessage> searchMessages(Long projectId, String query);
    
    // Count unread messages (messages after last seen timestamp)
    long countByProjectIdAndCreatedAtAfter(Long projectId, LocalDateTime lastSeen);
    
    // Delete messages by project (for cleanup)
    void deleteByProjectId(Long projectId);
}
```

### ChatMessageReactionRepository.java
```java
package com.synergyhub.repository;

import com.synergyhub.entity.ChatMessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageReactionRepository 
        extends JpaRepository<ChatMessageReaction, Long> {
    
    // Get all reactions for a message
    List<ChatMessageReaction> findByMessageId(Long messageId);
    
    // Find specific reaction by user and emoji
    Optional<ChatMessageReaction> findByMessageIdAndUserIdAndEmoji(
        Long messageId, Long userId, String emoji);
    
    // Delete reaction
    void deleteByMessageIdAndUserIdAndEmoji(
        Long messageId, Long userId, String emoji);
    
    // Check if user already reacted with emoji
    boolean existsByMessageIdAndUserIdAndEmoji(
        Long messageId, Long userId, String emoji);
}
```

### UserPresenceRepository.java
```java
package com.synergyhub.repository;

import com.synergyhub.entity.UserPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPresenceRepository extends JpaRepository<UserPresence, Long> {
    
    Optional<UserPresence> findByUserIdAndProjectId(Long userId, Long projectId);
    
    List<UserPresence> findByProjectId(Long projectId);
    
    void deleteByUserId(Long userId);
}
```

---

## 6. Service Layer

### ChatService.java
```java
package com.synergyhub.service;

import com.synergyhub.dto.*;
import com.synergyhub.entity.ChatMessage;
import com.synergyhub.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final ChatMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    
    /**
     * Send a new message
     */
    @Transactional
    public ChatMessageDTO sendMessage(SendMessageRequest request) {
        // Validate project access
        validateProjectAccess(request.getProjectId(), request.getUserId());
        
        // Create message entity
        ChatMessage message = new ChatMessage();
        message.setProjectId(request.getProjectId());
        message.setUserId(request.getUserId());
        message.setMessage(request.getMessage());
        message.setReplyToMessageId(request.getReplyToMessageId());
        
        // Save to database
        message = messageRepository.save(message);
        
        // Convert to DTO
        ChatMessageDTO dto = toDTO(message);
        
        // Broadcast to all connected clients in the project
        messagingTemplate.convertAndSend(
            "/topic/project/" + request.getProjectId() + "/messages",
            dto
        );
        
        return dto;
    }
    
    /**
     * Edit existing message
     */
    @Transactional
    public ChatMessageDTO editMessage(Long messageId, String newMessage, Long userId) {
        ChatMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        
        // Verify ownership
        if (!message.getUserId().equals(userId)) {
            throw new UnauthorizedException("Cannot edit other user's messages");
        }
        
        message.setMessage(newMessage);
        message.setIsEdited(true);
        message.setEditedAt(LocalDateTime.now());
        
        message = messageRepository.save(message);
        ChatMessageDTO dto = toDTO(message);
        
        // Broadcast update
        messagingTemplate.convertAndSend(
            "/topic/project/" + message.getProjectId() + "/messages/updated",
            dto
        );
        
        return dto;
    }
    
    /**
     * Delete message
     */
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        ChatMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        
        // Verify ownership
        if (!message.getUserId().equals(userId)) {
            throw new UnauthorizedException("Cannot delete other user's messages");
        }
        
        Long projectId = message.getProjectId();
        messageRepository.delete(message);
        
        // Broadcast deletion
        messagingTemplate.convertAndSend(
            "/topic/project/" + projectId + "/messages/deleted",
            messageId
        );
    }
    
    /**
     * Get messages for a project (paginated)
     */
    public Page<ChatMessageDTO> getProjectMessages(Long projectId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> messages = messageRepository
            .findByProjectIdOrderByCreatedAtDesc(projectId, pageable);
        
        return messages.map(this::toDTO);
    }
    
    /**
     * Get messages after timestamp (for sync)
     */
    public List<ChatMessageDTO> getMessagesSince(Long projectId, LocalDateTime since) {
        List<ChatMessage> messages = messageRepository
            .findByProjectIdAndCreatedAtAfterOrderByCreatedAtAsc(projectId, since);
        
        return messages.stream()
            .map(this::toDTO)
            .toList();
    }
    
    /**
     * Search messages
     */
    public List<ChatMessageDTO> searchMessages(Long projectId, String query) {
        List<ChatMessage> messages = messageRepository.searchMessages(projectId, query);
        return messages.stream()
            .map(this::toDTO)
            .toList();
    }
    
    private ChatMessageDTO toDTO(ChatMessage message) {
        // Convert entity to DTO with user information
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setProjectId(message.getProjectId());
        dto.setUserId(message.getUserId());
        dto.setMessage(message.getMessage());
        dto.setTimestamp(message.getCreatedAt());
        dto.setEdited(message.getIsEdited());
        dto.setEditedAt(message.getEditedAt());
        
        // Add user info
        dto.setUser(userService.getUserById(message.getUserId()));
        
        // Add reply-to info if exists
        if (message.getReplyToMessageId() != null) {
            ChatMessage replyToMsg = messageRepository
                .findById(message.getReplyToMessageId())
                .orElse(null);
            if (replyToMsg != null) {
                dto.setReplyTo(toSimpleDTO(replyToMsg));
            }
        }
        
        return dto;
    }
    
    private void validateProjectAccess(Long projectId, Long userId) {
        // TODO: Implement project access validation
    }
}
```

### ChatReactionService.java
```java
package com.synergyhub.service;

import com.synergyhub.dto.ReactionDTO;
import com.synergyhub.entity.ChatMessageReaction;
import com.synergyhub.repository.ChatMessageReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatReactionService {
    
    private final ChatMessageReactionRepository reactionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Transactional
    public void toggleReaction(Long messageId, Long userId, String emoji, Long projectId) {
        // Check if reaction exists
        Optional<ChatMessageReaction> existing = reactionRepository
            .findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);
        
        if (existing.isPresent()) {
            // Remove reaction
            reactionRepository.delete(existing.get());
        } else {
            // Add reaction
            ChatMessageReaction reaction = new ChatMessageReaction();
            reaction.setMessageId(messageId);
            reaction.setUserId(userId);
            reaction.setEmoji(emoji);
            reactionRepository.save(reaction);
        }
        
        // Broadcast reaction update
        ReactionDTO dto = new ReactionDTO(messageId, userId, emoji, existing.isEmpty());
        messagingTemplate.convertAndSend(
            "/topic/project/" + projectId + "/reactions",
            dto
        );
    }
}
```

---

## 7. WebSocket Controllers

### ChatWebSocketController.java
```java
package com.synergyhub.controller;

import com.synergyhub.dto.*;
import com.synergyhub.service.ChatService;
import com.synergyhub.service.ChatReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {
    
    private final ChatService chatService;
    private final ChatReactionService reactionService;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Handle new message from client
     * Client sends to: /app/chat.sendMessage
     * Response broadcasts to: /topic/project/{projectId}/messages
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(
            @Payload SendMessageRequest request,
            Principal principal) {
        
        // Extract user from principal
        Long userId = extractUserId(principal);
        request.setUserId(userId);
        
        // Process message
        chatService.sendMessage(request);
    }
    
    /**
     * Handle message edit
     */
    @MessageMapping("/chat.editMessage")
    public void editMessage(
            @Payload EditMessageRequest request,
            Principal principal) {
        
        Long userId = extractUserId(principal);
        chatService.editMessage(request.getMessageId(), request.getMessage(), userId);
    }
    
    /**
     * Handle message deletion
     */
    @MessageMapping("/chat.deleteMessage")
    public void deleteMessage(
            @Payload DeleteMessageRequest request,
            Principal principal) {
        
        Long userId = extractUserId(principal);
        chatService.deleteMessage(request.getMessageId(), userId);
    }
    
    /**
     * Handle reaction toggle
     */
    @MessageMapping("/chat.toggleReaction")
    public void toggleReaction(
            @Payload ReactionRequest request,
            Principal principal) {
        
        Long userId = extractUserId(principal);
        reactionService.toggleReaction(
            request.getMessageId(),
            userId,
            request.getEmoji(),
            request.getProjectId()
        );
    }
    
    /**
     * Handle typing indicator
     * Client sends to: /app/chat.typing
     * Broadcasts to: /topic/project/{projectId}/typing
     */
    @MessageMapping("/chat.typing")
    public void handleTyping(
            @Payload TypingIndicatorDTO typing,
            Principal principal) {
        
        Long userId = extractUserId(principal);
        typing.setUserId(userId);
        
        // Broadcast typing status to all users in project (except sender)
        messagingTemplate.convertAndSend(
            "/topic/project/" + typing.getProjectId() + "/typing",
            typing
        );
    }
    
    /**
     * Handle user joining chatroom
     */
    @MessageMapping("/chat.join")
    @SendTo("/topic/project/{projectId}/users")
    public UserJoinedDTO userJoined(
            @DestinationVariable Long projectId,
            Principal principal) {
        
        Long userId = extractUserId(principal);
        return new UserJoinedDTO(userId, projectId);
    }
    
    /**
     * Handle user leaving chatroom
     */
    @MessageMapping("/chat.leave")
    @SendTo("/topic/project/{projectId}/users")
    public UserLeftDTO userLeft(
            @DestinationVariable Long projectId,
            Principal principal) {
        
        Long userId = extractUserId(principal);
        return new UserLeftDTO(userId, projectId);
    }
    
    private Long extractUserId(Principal principal) {
        // Extract user ID from authenticated principal
        // TODO: Implement based on your authentication setup
        return 1L;
    }
}
```

---

## 8. REST API Endpoints

### ChatRestController.java
```java
package com.synergyhub.controller;

import com.synergyhub.dto.ChatMessageDTO;
import com.synergyhub.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {
    
    private final ChatService chatService;
    
    /**
     * GET /api/chat/projects/{projectId}/messages
     * Get paginated messages for a project
     */
    @GetMapping("/projects/{projectId}/messages")
    public ResponseEntity<Page<ChatMessageDTO>> getMessages(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Page<ChatMessageDTO> messages = chatService.getProjectMessages(projectId, page, size);
        return ResponseEntity.ok(messages);
    }
    
    /**
     * GET /api/chat/projects/{projectId}/messages/since
     * Get messages after a specific timestamp (for sync)
     */
    @GetMapping("/projects/{projectId}/messages/since")
    public ResponseEntity<List<ChatMessageDTO>> getMessagesSince(
            @PathVariable Long projectId,
            @RequestParam String timestamp) {
        
        LocalDateTime since = LocalDateTime.parse(timestamp);
        List<ChatMessageDTO> messages = chatService.getMessagesSince(projectId, since);
        return ResponseEntity.ok(messages);
    }
    
    /**
     * GET /api/chat/projects/{projectId}/search
     * Search messages
     */
    @GetMapping("/projects/{projectId}/search")
    public ResponseEntity<List<ChatMessageDTO>> searchMessages(
            @PathVariable Long projectId,
            @RequestParam String query) {
        
        List<ChatMessageDTO> messages = chatService.searchMessages(projectId, query);
        return ResponseEntity.ok(messages);
    }
}
```

---

## 9. Security & Authentication

### WebSocket Security Configuration
```java
package com.synergyhub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketSecurityConfig 
        extends AbstractSecurityWebSocketMessageBrokerConfigurer {
    
    @Override
    protected void configureInbound(
            MessageSecurityMetadataSourceRegistry messages) {
        messages
            // Require authentication for all chat operations
            .simpDestMatchers("/app/chat.*").authenticated()
            // Allow subscription to project topics (check in service layer)
            .simpSubscribeDestMatchers("/topic/project/**").authenticated()
            // Allow subscription to user-specific queues
            .simpSubscribeDestMatchers("/user/queue/**").authenticated()
            // Deny all other messages
            .anyMessage().denyAll();
    }
    
    @Override
    protected boolean sameOriginDisabled() {
        // Disable CSRF for WebSocket (handle via JWT)
        return true;
    }
}
```

### Project Access Validation
```java
@Service
public class ChatAuthorizationService {
    
    public boolean canAccessProjectChat(Long userId, Long projectId) {
        // Check if user is a member of the project
        // TODO: Implement based on your project member logic
        return true;
    }
    
    public boolean canModifyMessage(Long userId, Long messageId) {
        // Check if user owns the message
        ChatMessage message = messageRepository.findById(messageId)
            .orElse(null);
        return message != null && message.getUserId().equals(userId);
    }
}
```

---

## 10. Real-Time Features

### Typing Indicator Implementation
```java
// DTO
public class TypingIndicatorDTO {
    private Long userId;
    private String userName;
    private Long projectId;
    private boolean isTyping;
}

// Service method
public void broadcastTypingIndicator(Long projectId, Long userId, boolean isTyping) {
    TypingIndicatorDTO indicator = new TypingIndicatorDTO();
    indicator.setUserId(userId);
    indicator.setProjectId(projectId);
    indicator.setIsTyping(isTyping);
    
    messagingTemplate.convertAndSend(
        "/topic/project/" + projectId + "/typing",
        indicator
    );
}
```

### Online Status Management
```java
@Service
public class PresenceService {
    
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        // User connected - set status to ONLINE
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(headerAccessor);
        Long projectId = extractProjectId(headerAccessor);
        
        updateUserPresence(userId, projectId, PresenceStatus.ONLINE);
    }
    
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        // User disconnected - set status to OFFLINE
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(headerAccessor);
        Long projectId = extractProjectId(headerAccessor);
        
        updateUserPresence(userId, projectId, PresenceStatus.OFFLINE);
    }
    
    private void updateUserPresence(Long userId, Long projectId, PresenceStatus status) {
        // Update presence in database
        // Broadcast to all connected clients
        messagingTemplate.convertAndSend(
            "/topic/project/" + projectId + "/presence",
            new PresenceUpdateDTO(userId, status)
        );
    }
}
```

---

## 11. Performance Optimization

### Message Pagination
- Load messages in batches of 50
- Implement infinite scroll on frontend
- Cache recent messages in Redis

### Redis Caching Strategy
```java
@Service
public class ChatCacheService {
    
    private static final String RECENT_MESSAGES_KEY = "project:%d:recent_messages";
    private static final int CACHE_SIZE = 100;
    
    @Autowired
    private RedisTemplate<String, ChatMessageDTO> redisTemplate;
    
    public void cacheMessage(ChatMessageDTO message) {
        String key = String.format(RECENT_MESSAGES_KEY, message.getProjectId());
        redisTemplate.opsForList().leftPush(key, message);
        redisTemplate.opsForList().trim(key, 0, CACHE_SIZE - 1);
    }
    
    public List<ChatMessageDTO> getRecentMessages(Long projectId) {
        String key = String.format(RECENT_MESSAGES_KEY, projectId);
        return redisTemplate.opsForList().range(key, 0, -1);
    }
}
```

### Database Optimization
- Index on (project_id, created_at)
- Partition messages by date for large datasets
- Archive old messages (>6 months) to separate table

---

## 12. Testing Strategy

### Unit Tests
```java
@SpringBootTest
class ChatServiceTest {
    
    @Autowired
    private ChatService chatService;
    
    @MockBean
    private ChatMessageRepository messageRepository;
    
    @Test
    void testSendMessage() {
        SendMessageRequest request = new SendMessageRequest();
        request.setProjectId(1L);
        request.setUserId(1L);
        request.setMessage("Hello");
        
        ChatMessageDTO result = chatService.sendMessage(request);
        
        assertNotNull(result);
        assertEquals("Hello", result.getMessage());
    }
}
```

### Integration Tests
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ChatWebSocketIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    private StompSession stompSession;
    
    @Test
    void testWebSocketConnection() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(
            new SockJsClient(createTransportClient())
        );
        
        stompSession = stompClient.connect(
            "ws://localhost:" + port + "/ws",
            new StompSessionHandlerAdapter() {}
        ).get(1, TimeUnit.SECONDS);
        
        assertTrue(stompSession.isConnected());
    }
}
```

---

## 13. Deployment Considerations

### Load Balancing with Multiple Instances
- Use Redis Pub/Sub for message broadcasting across instances
- Configure Spring Session with Redis for WebSocket session management
- Use sticky sessions in load balancer

### Scalability
- Horizontal scaling with Redis as message broker
- Database read replicas for message history
- CDN for static assets

### Monitoring
- Track active WebSocket connections
- Monitor message throughput
- Alert on connection failures
- Log message delivery failures

### Configuration (application.yml)
```yaml
spring:
  websocket:
    message-size-limit: 128KB
    send-buffer-size: 512KB
    send-time-limit: 20000
  
  redis:
    host: localhost
    port: 6379
    
chat:
  message:
    max-length: 5000
    retention-days: 90
  typing-indicator:
    timeout-seconds: 3
```

---

## Implementation Checklist

### Phase 1: Core Chat
- [ ] Database schema creation
- [ ] Entity classes
- [ ] Repository layer
- [ ] Basic chat service
- [ ] WebSocket configuration
- [ ] Send/receive messages

### Phase 2: Advanced Features
- [ ] Message editing/deletion
- [ ] Reactions
- [ ] Typing indicators
- [ ] Message search
- [ ] Pagination

### Phase 3: Optimization
- [ ] Redis caching
- [ ] Performance tuning
- [ ] Load testing
- [ ] Multi-instance support

### Phase 4: Polish
- [ ] Online status
- [ ] Unread message counts
- [ ] Notification integration
- [ ] Message attachments (optional)

---

## Frontend Integration Guide

### WebSocket Connection (TypeScript/JavaScript)
```typescript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class ChatWebSocketService {
  private client: Client;
  
  connect(projectId: number, token: string) {
    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      onConnect: () => {
        // Subscribe to project messages
        this.client.subscribe(
          `/topic/project/${projectId}/messages`,
          (message) => {
            const chatMessage = JSON.parse(message.body);
            this.handleNewMessage(chatMessage);
          }
        );
        
        // Subscribe to typing indicators
        this.client.subscribe(
          `/topic/project/${projectId}/typing`,
          (message) => {
            const typing = JSON.parse(message.body);
            this.handleTyping(typing);
          }
        );
      },
    });
    
    this.client.activate();
  }
  
  sendMessage(projectId: number, message: string, replyToId?: number) {
    this.client.publish({
      destination: '/app/chat.sendMessage',
      body: JSON.stringify({
        projectId,
        message,
        replyToMessageId: replyToId,
      }),
    });
  }
  
  sendTypingIndicator(projectId: number, isTyping: boolean) {
    this.client.publish({
      destination: '/app/chat.typing',
      body: JSON.stringify({ projectId, isTyping }),
    });
  }
}
```

---

## Conclusion

This implementation plan provides a comprehensive guide for adding real-time chatroom functionality to the SynergyHub project. The architecture is scalable, secure, and follows Spring Boot best practices. The WebSocket implementation with STOMP protocol ensures real-time bidirectional communication, while Redis enables horizontal scaling across multiple server instances.

**Key Benefits:**
- Real-time messaging without polling
- Scalable architecture with Redis
- Secure with JWT authentication
- Efficient with pagination and caching
- Production-ready with comprehensive error handling

**Next Steps:**
1. Review and approve this plan
2. Set up development environment
3. Implement Phase 1 (Core Chat)
4. Test thoroughly
5. Deploy to staging
6. Collect feedback and iterate
