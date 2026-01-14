package com.synergyhub.repository;

import com.synergyhub.domain.entity.ChatChannel;
import com.synergyhub.domain.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatChannelRepository extends JpaRepository<ChatChannel, Long> {
    Optional<ChatChannel> findByProjectAndName(Project project, String name);
    List<ChatChannel> findByProjectId(Long projectId);
}
