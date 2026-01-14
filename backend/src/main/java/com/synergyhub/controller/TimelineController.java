package com.synergyhub.controller;

import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.TimelineViewResponse;
import com.synergyhub.security.UserContext;
import com.synergyhub.domain.entity.User;
import com.synergyhub.service.timeline.TimelineService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TimelineController {
    
    private final TimelineService timelineService;
    
    @GetMapping("/{projectId}/timeline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TimelineViewResponse>> getProjectTimeline(
            @PathVariable @Positive(message = "Project ID must be positive") Long projectId,
            @RequestParam(name = "months", defaultValue = "6") Integer monthsAhead,
            UserContext userContext,
            HttpServletRequest httpRequest) {
        
        log.info("Getting timeline for project: {} by user: {}", projectId, userContext.getId());
        
        // Convert UserContext to User entity (as expected by Service security check)
        User currentUser = new User();
        currentUser.setId(userContext.getId());
        
        TimelineViewResponse timeline = timelineService.getProjectTimeline(projectId, currentUser, monthsAhead);
        
        return ResponseEntity.ok(ApiResponse.<TimelineViewResponse>builder()
                .success(true)
                .message("Timeline retrieved successfully")
                .data(timeline)
                .build());
    }
}
