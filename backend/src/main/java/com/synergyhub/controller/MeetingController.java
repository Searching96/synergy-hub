package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.CreateMeetingRequest;
import com.synergyhub.dto.response.ApiResponse;
import com.synergyhub.dto.response.MeetingResponse;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.MeetingService;
import com.synergyhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;
    private final UserRepository userRepository;
    private final com.synergyhub.service.meeting.LiveKitService liveKitService;

    @PostMapping
    public ResponseEntity<ApiResponse<MeetingResponse>> createMeeting(
            @RequestBody CreateMeetingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(ApiResponse.success(
                meetingService.createMeeting(request, user)
        ));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getProjectMeetings(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(
                meetingService.getProjectMeetings(projectId)
        ));
    }

    @GetMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<MeetingResponse>> getMeeting(
            @PathVariable Long meetingId) {
        return ResponseEntity.ok(ApiResponse.success(
                meetingService.getMeeting(meetingId)
        ));
    }
    
    @PostMapping("/{meetingId}/join")
    public ResponseEntity<ApiResponse<MeetingResponse>> joinMeeting(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(ApiResponse.success(
                meetingService.joinMeeting(meetingId, user)
        ));
    }

    @PostMapping("/{meetingId}/leave")
    public ResponseEntity<ApiResponse<MeetingResponse>> leaveMeeting(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(ApiResponse.success(
                meetingService.leaveMeeting(meetingId, user)
        ));
    }

    @GetMapping("/{meetingId}/token")
    public ResponseEntity<ApiResponse<String>> getJoinToken(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        MeetingResponse meeting = meetingService.getMeeting(meetingId);
        // In a real app, verify user has access to this meeting/project here

        String token = liveKitService.generateToken(
                meeting.getMeetingCode(),
                principal.getId().toString(),
                principal.getName()
        );

        return ResponseEntity.ok(ApiResponse.success(token));
    }
}
