package com.synergyhub.repository;

import com.synergyhub.domain.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChannelIdOrderBySentAtAsc(Long channelId);
    Page<ChatMessage> findByChannelIdOrderBySentAtDesc(Long channelId, Pageable pageable);
}
