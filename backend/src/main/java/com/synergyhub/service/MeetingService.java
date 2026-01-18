package com.synergyhub.service;

import com.synergyhub.domain.entity.Meeting;
import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.enums.MeetingStatus;
import com.synergyhub.dto.request.CreateMeetingRequest;
import com.synergyhub.dto.response.MeetingResponse;
import com.synergyhub.dto.response.UserResponse;
import com.synergyhub.repository.MeetingRepository;
import com.synergyhub.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public MeetingResponse createMeeting(CreateMeetingRequest request, User organizer) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Meeting meeting = Meeting.builder()
                .project(project)
                .organizer(organizer)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(Boolean.TRUE.equals(request.getIsInstant()) ? MeetingStatus.IN_PROGRESS : MeetingStatus.SCHEDULED)
                .scheduledAt(Boolean.TRUE.equals(request.getIsInstant()) ? LocalDateTime.now() : request.getScheduledAt())
                .startedAt(Boolean.TRUE.equals(request.getIsInstant()) ? LocalDateTime.now() : null)
                .meetingCode(UUID.randomUUID().toString().substring(0, 8)) // Simple code
                .build();
        
        meeting.getParticipants().add(organizer); // Add organizer as participant
        
        meeting = meetingRepository.save(meeting);
        return mapToResponse(meeting);
    }

    @Transactional(readOnly = true)
    public java.util.List<MeetingResponse> getProjectMeetings(Long projectId) {
        return meetingRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MeetingResponse joinMeeting(Long meetingId, User user) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        if (meeting.getStatus() == MeetingStatus.ENDED || meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new RuntimeException("Meeting is not active");
        }

        meeting.getParticipants().add(user);
        
        // If first participant besides organizer (or just starting), ensure status is IN_PROGRESS
        if (meeting.getStatus() == MeetingStatus.SCHEDULED) {
            meeting.setStatus(MeetingStatus.IN_PROGRESS);
            if (meeting.getStartedAt() == null) {
                meeting.setStartedAt(LocalDateTime.now());
            }
        }
        
        meeting = meetingRepository.save(meeting);
        return mapToResponse(meeting);
    }

    @Transactional
    public MeetingResponse leaveMeeting(Long meetingId, User user) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        meeting.getParticipants().removeIf(p -> p.getId().equals(user.getId()));
        
        // If no more participants (including organizer), and it was in progress, maybe end it?
        // Actually, let's keep it simple: just remove the participant. 
        // If the organizer leaves, we could optionally end it, but let's stick to simple leave logic first.
        
        meeting = meetingRepository.save(meeting);
        return mapToResponse(meeting);
    }
    
    @Transactional(readOnly = true)
    public MeetingResponse getMeeting(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                 .orElseThrow(() -> new RuntimeException("Meeting not found"));
        return mapToResponse(meeting);
    }

    private MeetingResponse mapToResponse(Meeting meeting) {
        return MeetingResponse.builder()
                .id(meeting.getId())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .status(meeting.getStatus())
                .scheduledAt(meeting.getScheduledAt())
                .startedAt(meeting.getStartedAt())
                .endedAt(meeting.getEndedAt())
                .meetingCode(meeting.getMeetingCode())
                .projectId(meeting.getProject().getId())
                .projectName(meeting.getProject().getName())
                .organizerId(meeting.getOrganizer().getId())
                .organizerName(meeting.getOrganizer().getName())
                .participants(meeting.getParticipants().stream()
                        .map(u -> UserResponse.builder()
                                .id(u.getId())
                                .name(u.getName())
                                .email(u.getEmail())
                                .build())
                        .collect(Collectors.toSet()))
                .build();
    }
}
