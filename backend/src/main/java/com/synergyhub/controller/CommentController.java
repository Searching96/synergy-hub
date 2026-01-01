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

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CommentController {

    private final CommentService commentService;

    /**
     * Add a comment to a task
     * POST /api/tasks/{taskId}/comments
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable @Positive(message = "Task ID must be positive") Integer taskId,
            @Valid @RequestBody CreateCommentRequest request,
            UserContext userContext) {

        User currentUser = new User();
        currentUser.setId(userContext.getId());
        // currentUser.setName(userContext.getName()); // If available in context, useful for mapper

        CommentResponse response = commentService.addComment(taskId, request.getContent(), currentUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added", response));
    }

    /**
     * Get all comments for a task
     * GET /api/tasks/{taskId}/comments?page=0&size=50
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable @Positive(message = "Task ID must be positive") Integer taskId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size,
            UserContext userContext) {

        User currentUser = new User();
        currentUser.setId(userContext.getId());

        List<CommentResponse> comments = commentService.getTaskComments(taskId, currentUser, page, size);

        return ResponseEntity.ok(ApiResponse.success(comments));
    }
}