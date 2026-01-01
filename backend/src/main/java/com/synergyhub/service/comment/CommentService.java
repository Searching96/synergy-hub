package com.synergyhub.service.comment;

import com.synergyhub.domain.entity.Comment;
import com.synergyhub.domain.entity.Task;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.mapper.CommentMapper;
import com.synergyhub.dto.response.CommentResponse;
import com.synergyhub.exception.TaskNotFoundException;
import com.synergyhub.repository.CommentRepository;
import com.synergyhub.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final CommentMapper commentMapper;

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional
    public CommentResponse addComment(Integer taskId, String content, User currentUser) {
        log.info("Adding comment to task: {} by user: {}", taskId, currentUser.getId());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        Comment comment = Comment.builder()
                .task(task)
                .user(currentUser)
                .content(content)
                .build();

        Comment savedComment = commentRepository.save(comment);
        return commentMapper.toResponse(savedComment);
    }

    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #currentUser)")
    @Transactional(readOnly = true)
    public List<CommentResponse> getTaskComments(Integer taskId, User currentUser, int page, int size) {
        log.info("Fetching comments for task: {} (page: {}, size: {})", taskId, page, size);
        
        // Quick check to ensure task exists (security check handles the access)
        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException(taskId);
        }

        Pageable pageable = PageRequest.of(page, size);
        List<Comment> comments = commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId, pageable);
        return commentMapper.toResponseList(comments);
    }
}