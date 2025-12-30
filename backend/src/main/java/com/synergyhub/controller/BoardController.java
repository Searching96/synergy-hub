package com.synergyhub.controller;

import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.BoardViewResponse;
import com.synergyhub.security.UserContext;
import com.synergyhub.domain.entity.User;
import com.synergyhub.service.board.BoardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class BoardController {

    private final BoardService boardService;

    /**
     * Get the "Board View" (Active Sprints + Backlog)
     * GET /api/projects/{projectId}/board
     */
    @GetMapping("/{projectId}/board")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BoardViewResponse>> getProjectBoard(
            @PathVariable Integer projectId,
            UserContext userContext,
            HttpServletRequest httpRequest) {

        log.info("Getting board view for project: {} by user: {}", projectId, userContext.getId());
        
        // Convert UserContext to User entity (as expected by Service security check)
        User currentUser = new User();
        currentUser.setId(userContext.getId());
        currentUser.setEmail(userContext.getEmail());

        BoardViewResponse boardView = boardService.getProjectBoard(projectId, currentUser);

        return ResponseEntity.ok(
                ApiResponse.success("Board data retrieved successfully", boardView)
        );
    }
}