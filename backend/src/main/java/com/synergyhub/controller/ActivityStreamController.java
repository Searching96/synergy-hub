package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.response.ActivityLogResponse;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.security.UserContext;
import com.synergyhub.service.activity.ActivityStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ActivityStreamController {

    private final ActivityStreamService activityStreamService;

    /**
     * Get project activity stream (recent events)
     * GET /api/projects/{projectId}/activity?page=0&size=20
     */
    @GetMapping("/{projectId}/activity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getProjectActivity(
            @PathVariable Integer projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            UserContext userContext) {

        User currentUser = new User();
        currentUser.setId(userContext.getId());

        List<ActivityLogResponse> activities = activityStreamService.getProjectActivity(projectId, currentUser, page, size);

        return ResponseEntity.ok(ApiResponse.success(activities));
    }
}