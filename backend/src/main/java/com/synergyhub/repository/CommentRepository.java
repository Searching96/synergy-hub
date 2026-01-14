package com.synergyhub.repository;

import com.synergyhub.domain.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    // Fetch comments for a task, ordered by oldest first (chat style) - paginated
    List<Comment> findByTaskIdOrderByCreatedAtAsc(Long taskId, Pageable pageable);
}