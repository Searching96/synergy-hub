package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.response.ActivityLogResponse;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.security.UserContext;
import com.synergyhub.service.activity.ActivityStreamService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ActivityStreamController {

    private final ActivityStreamService activityStreamService;

    /**
     * Get project activity stream (recent events)
     * GET /api/projects/{projectId}/activity?page=0&size=20
     */
    @GetMapping("/{projectId}/activity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getProjectActivity(
            @PathVariable @Positive(message = "Project ID must be positive") Long projectId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            UserContext userContext) {

        User currentUser = new User();
        currentUser.setId(userContext.getId());

        List<ActivityLogResponse> activities = activityStreamService.getProjectActivity(projectId, currentUser, page, size);

        return ResponseEntity.ok(ApiResponse.success(activities));
    }
}