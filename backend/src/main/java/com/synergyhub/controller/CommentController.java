package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.CreateCommentRequest;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.CommentResponse;
import com.synergyhub.security.UserContext;
import com.synergyhub.service.comment.CommentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // ✅ Added

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CommentController {

    private final CommentService commentService;
    private final com.synergyhub.repository.UserRepository userRepository; // ✅ Inject for fetching user entity

    /**
     * Add a comment to a task
     * POST /api/tasks/{taskId}/comments
     */
    @PostMapping
    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #principal)")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable @Positive(message = "Task ID must be positive") Long taskId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal com.synergyhub.security.UserPrincipal principal) {

        // Need the real User entity for the service
        // Service expects User, not just ID
        User currentUser = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        CommentResponse response = commentService.addComment(taskId, request.getContent(), currentUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added", response));
    }

    /**
     * Get all comments for a task
     * GET /api/tasks/{taskId}/comments?page=0&size=50
     */
    @GetMapping
    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #principal)")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable @Positive(message = "Task ID must be positive") Long taskId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size,
            @AuthenticationPrincipal com.synergyhub.security.UserPrincipal principal) {

        User currentUser = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<CommentResponse> comments = commentService.getTaskComments(taskId, currentUser, page, size);

        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    /**
     * Update a comment
     * PUT /api/tasks/{taskId}/comments/{commentId}
     */
    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable @Positive(message = "Task ID must be positive") Long taskId,
            @PathVariable @Positive(message = "Comment ID must be positive") Long commentId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal com.synergyhub.security.UserPrincipal principal) {

        User currentUser = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        CommentResponse response = commentService.updateComment(commentId, request.getContent(), currentUser);

        return ResponseEntity.ok(ApiResponse.success("Comment updated", response));
    }

    /**
     * Delete a comment
     * DELETE /api/tasks/{taskId}/comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable @Positive(message = "Task ID must be positive") Long taskId,
            @PathVariable @Positive(message = "Comment ID must be positive") Long commentId,
            @AuthenticationPrincipal com.synergyhub.security.UserPrincipal principal) {

        User currentUser = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        commentService.deleteComment(commentId, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Comment deleted", null));
    }
}