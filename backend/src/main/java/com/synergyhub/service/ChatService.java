package com.synergyhub.service;

import com.synergyhub.domain.entity.ChatChannel;
import com.synergyhub.domain.entity.ChatMessage;
import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.enums.ChannelType;
import com.synergyhub.dto.request.CreateChatMessageRequest;
import com.synergyhub.dto.response.ChatMessageResponse;
import com.synergyhub.repository.ChatChannelRepository;
import com.synergyhub.repository.ChatMessageRepository;
import com.synergyhub.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatChannelRepository chatChannelRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public ChatMessageResponse sendMessage(CreateChatMessageRequest request, User sender) {
        ChatChannel channel;
        if (request.getChannelId() != null) {
            channel = chatChannelRepository.findById(request.getChannelId())
                    .orElseThrow(() -> new RuntimeException("Channel not found"));
        } else if (request.getProjectId() != null) {
            // Get or create general channel for project
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            
            channel = chatChannelRepository.findByProjectAndName(project, "General")
                    .orElseGet(() -> {
                        ChatChannel newChannel = ChatChannel.builder()
                                .project(project)
                                .organization(project.getOrganization())
                                .name("General")
                                .channelType(ChannelType.PROJECT)
                                .build();
                        return chatChannelRepository.save(newChannel);
                    });
        } else {
            throw new RuntimeException("Channel ID or Project ID required");
        }

        ChatMessage message = ChatMessage.builder()
                .channel(channel)
                .user(sender)
                .content(request.getContent())
                .sentAt(LocalDateTime.now())
                .build();

        message = chatMessageRepository.save(message);

        return mapToResponse(message);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChannelMessages(Long channelId) {
        return chatMessageRepository.findByChannelIdOrderBySentAtAsc(channelId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getProjectMessages(Long projectId) {
        // Simple implementation: Get "General" channel messages
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        ChatChannel channel = chatChannelRepository.findByProjectAndName(project, "General")
                .orElse(null);
        
        if (channel == null) {
            return java.util.Collections.emptyList();
        }
        
        return getChannelMessages(channel.getId());
    }

    @Transactional
    public ChatMessageResponse editMessage(Long messageId, String content, User user) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You can only edit your own messages");
        }

        message.setContent(content);
        message.setEdited(true);
        message.setUpdatedAt(LocalDateTime.now());
        
        message = chatMessageRepository.save(message);
        return mapToResponse(message);
    }

    @Transactional
    public void deleteMessage(Long messageId, User user) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You can only delete your own messages");
        }

        chatMessageRepository.delete(message);
    }

    private ChatMessageResponse mapToResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .updatedAt(message.getUpdatedAt())
                .userId(message.getUser().getId())
                .userName(message.getUser().getName())
                .channelId(message.getChannel().getId())
                .edited(message.isEdited())
                .build();
    }
}
