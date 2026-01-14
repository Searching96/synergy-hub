package com.synergyhub.dto.response;

import com.synergyhub.domain.enums.MeetingStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class MeetingResponse {
    private Long id;
    private String title;
    private String description;
    private MeetingStatus status;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String meetingCode;
    private Long projectId;
    private String projectName;
    private Long organizerId;
    private String organizerName;
    private Set<UserResponse> participants;
}
