# WebRTC Video Meeting Room Implementation Plan

## Overview
This document outlines the complete implementation plan for adding Google Meet-style video conferencing functionality to the SynergyHub project. The implementation uses WebRTC for peer-to-peer video/audio communication, with a signaling server for connection coordination and optional media server for improved quality and features.

---

## Table of Contents
1. [Technology Stack](#technology-stack)
2. [Architecture Overview](#architecture-overview)
3. [Database Schema](#database-schema)
4. [Entity Design](#entity-design)
5. [WebRTC Signaling Server](#webrtc-signaling-server)
6. [Media Server Setup (Optional)](#media-server-setup-optional)
7. [Repository Layer](#repository-layer)
8. [Service Layer](#service-layer)
9. [REST API Endpoints](#rest-api-endpoints)
10. [WebSocket Signaling](#websocket-signaling)
11. [Security & Authentication](#security--authentication)
12. [Recording & Storage](#recording--storage)
13. [Performance & Scalability](#performance--scalability)
14. [Testing Strategy](#testing-strategy)
15. [Deployment Guide](#deployment-guide)

---

## 1. Technology Stack

### Core Technologies
- **WebRTC**: Real-time peer-to-peer audio/video communication
- **Spring Boot WebSocket**: Signaling server for WebRTC connection setup
- **STOMP Protocol**: Message protocol for signaling
- **Janus Gateway** (Optional): Selective Forwarding Unit (SFU) for improved scalability
- **Kurento Media Server** (Alternative): Full-featured media server with recording
- **PostgreSQL**: Meeting metadata and participant records
- **Redis**: Session management and presence
- **MinIO/S3**: Recording storage

### Dependencies (pom.xml)
```xml
<dependencies>
    <!-- WebSocket Support -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    
    <!-- Redis for session management -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- WebRTC client (if using Kurento) -->
    <dependency>
        <groupId>org.kurento</groupId>
        <artifactId>kurento-client</artifactId>
        <version>7.0.0</version>
    </dependency>
    
    <!-- AWS SDK for S3/MinIO -->
    <dependency>
        <groupId>com.amazonaws</groupId>
        <artifactId>aws-java-sdk-s3</artifactId>
    </dependency>
</dependencies>
```

---

## 2. Architecture Overview

### Architecture Options

#### Option 1: Mesh Architecture (P2P)
```
Participant 1 ←→ Participant 2
     ↕              ↕
Participant 3 ←→ Participant 4
```
- **Pros**: No media server required, lower latency, simpler setup
- **Cons**: High bandwidth usage (N-1 connections per participant), max ~4-6 participants
- **Best for**: Small meetings (2-6 people)

#### Option 2: SFU Architecture (Recommended)
```
Participant 1 ←→ SFU Server ←→ Participant 2
Participant 3 ←→     ↑      ←→ Participant 4
```
- **Pros**: Scalable to 50+ participants, lower client bandwidth, server controls quality
- **Cons**: Requires media server (Janus/Mediasoup)
- **Best for**: Medium to large meetings

#### Option 3: MCU Architecture
```
Participants → MCU (mixes all streams) → Single mixed stream to all
```
- **Pros**: Lowest client bandwidth (1 incoming stream)
- **Cons**: High server CPU usage, higher latency, expensive
- **Best for**: Large webinars (100+ participants)

**Recommendation**: Start with **Mesh** for MVP, migrate to **SFU** for production.

### Component Diagram
```
┌─────────────────┐         ┌──────────────────┐
│  React Client   │◄─────►  │ Signaling Server │
│  (WebRTC)       │         │  (Spring Boot)   │
└─────────────────┘         └──────────────────┘
        ↕                            ↕
┌─────────────────┐         ┌──────────────────┐
│  TURN Server    │         │   PostgreSQL     │
│  (coturn)       │         │  (Metadata DB)   │
└─────────────────┘         └──────────────────┘
        ↕                            ↕
┌─────────────────┐         ┌──────────────────┐
│  Media Server   │         │   MinIO/S3       │
│  (Janus/SFU)    │         │  (Recordings)    │
└─────────────────┘         └──────────────────┘
```

---

## 3. Database Schema

### Meetings Table
```sql
CREATE TABLE meetings (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id BIGINT NOT NULL,
    meeting_code VARCHAR(20) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    host_user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL, -- SCHEDULED, IN_PROGRESS, ENDED, CANCELLED
    max_participants INT DEFAULT 50,
    is_recording BOOLEAN DEFAULT FALSE,
    recording_url VARCHAR(500),
    scheduled_start_time TIMESTAMP,
    scheduled_end_time TIMESTAMP,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_meeting_project FOREIGN KEY (project_id) 
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_meeting_host FOREIGN KEY (host_user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_meetings_project_id ON meetings(project_id);
CREATE INDEX idx_meetings_status ON meetings(status);
CREATE INDEX idx_meetings_code ON meetings(meeting_code);
CREATE INDEX idx_meetings_scheduled_start ON meetings(scheduled_start_time);
```

### Meeting Participants Table
```sql
CREATE TABLE meeting_participants (
    id BIGSERIAL PRIMARY KEY,
    meeting_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    is_audio_enabled BOOLEAN DEFAULT TRUE,
    is_video_enabled BOOLEAN DEFAULT TRUE,
    is_screen_sharing BOOLEAN DEFAULT FALSE,
    is_hand_raised BOOLEAN DEFAULT FALSE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    duration_seconds INT, -- calculated when leaving
    
    CONSTRAINT fk_participant_meeting FOREIGN KEY (meeting_id) 
        REFERENCES meetings(id) ON DELETE CASCADE,
    CONSTRAINT fk_participant_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Prevent duplicate active participants
    CONSTRAINT uk_active_participant UNIQUE (meeting_id, user_id, left_at)
);

-- Indexes
CREATE INDEX idx_participants_meeting_id ON meeting_participants(meeting_id);
CREATE INDEX idx_participants_user_id ON meeting_participants(user_id);
CREATE INDEX idx_participants_active ON meeting_participants(meeting_id, left_at) 
    WHERE left_at IS NULL;
```

### Meeting Settings Table
```sql
CREATE TABLE meeting_settings (
    meeting_id VARCHAR(36) PRIMARY KEY,
    allow_participant_screen_share BOOLEAN DEFAULT TRUE,
    allow_participant_chat BOOLEAN DEFAULT TRUE,
    mute_participants_on_entry BOOLEAN DEFAULT FALSE,
    waiting_room_enabled BOOLEAN DEFAULT FALSE,
    recording_enabled BOOLEAN DEFAULT TRUE,
    max_duration_minutes INT DEFAULT 60,
    
    CONSTRAINT fk_settings_meeting FOREIGN KEY (meeting_id) 
        REFERENCES meetings(id) ON DELETE CASCADE
);
```

### Meeting Chat Messages Table
```sql
CREATE TABLE meeting_chat_messages (
    id BIGSERIAL PRIMARY KEY,
    meeting_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    is_private BOOLEAN DEFAULT FALSE,
    recipient_user_id BIGINT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_chat_meeting FOREIGN KEY (meeting_id) 
        REFERENCES meetings(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_recipient FOREIGN KEY (recipient_user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_chat_meeting_id ON meeting_chat_messages(meeting_id);
CREATE INDEX idx_chat_timestamp ON meeting_chat_messages(timestamp);
```

### Meeting Events Log Table (for analytics)
```sql
CREATE TABLE meeting_events (
    id BIGSERIAL PRIMARY KEY,
    meeting_id VARCHAR(36) NOT NULL,
    user_id BIGINT,
    event_type VARCHAR(50) NOT NULL, -- JOINED, LEFT, MUTED, UNMUTED, VIDEO_ON, VIDEO_OFF, SCREEN_SHARE_STARTED, SCREEN_SHARE_STOPPED, HAND_RAISED, HAND_LOWERED
    event_data JSONB,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_event_meeting FOREIGN KEY (meeting_id) 
        REFERENCES meetings(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_events_meeting_id ON meeting_events(meeting_id);
CREATE INDEX idx_events_type ON meeting_events(event_type);
CREATE INDEX idx_events_timestamp ON meeting_events(timestamp);
```

---

## 4. Entity Design

### Meeting.java
```java
package com.synergyhub.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "meetings")
@Data
public class Meeting {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(length = 36)
    private String id;
    
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    
    @Column(name = "meeting_code", unique = true, nullable = false, length = 20)
    private String meetingCode;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "host_user_id", nullable = false)
    private Long hostUserId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingStatus status = MeetingStatus.SCHEDULED;
    
    @Column(name = "max_participants")
    private Integer maxParticipants = 50;
    
    @Column(name = "is_recording")
    private Boolean isRecording = false;
    
    @Column(name = "recording_url", length = 500)
    private String recordingUrl;
    
    @Column(name = "scheduled_start_time")
    private LocalDateTime scheduledStartTime;
    
    @Column(name = "scheduled_end_time")
    private LocalDateTime scheduledEndTime;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_user_id", insertable = false, updatable = false)
    private User host;
    
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MeetingParticipant> participants;
    
    @OneToOne(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private MeetingSettings settings;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (meetingCode == null) {
            meetingCode = generateMeetingCode();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    private String generateMeetingCode() {
        // Generate format: xxx-xxx-xxx
        String chars = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            if (i > 0) code.append("-");
            for (int j = 0; j < 3; j++) {
                code.append(chars.charAt((int) (Math.random() * chars.length())));
            }
        }
        return code.toString();
    }
    
    public enum MeetingStatus {
        SCHEDULED, IN_PROGRESS, ENDED, CANCELLED
    }
}
```

### MeetingParticipant.java
```java
package com.synergyhub.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_participants")
@Data
public class MeetingParticipant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "meeting_id", nullable = false, length = 36)
    private String meetingId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "is_audio_enabled")
    private Boolean isAudioEnabled = true;
    
    @Column(name = "is_video_enabled")
    private Boolean isVideoEnabled = true;
    
    @Column(name = "is_screen_sharing")
    private Boolean isScreenSharing = false;
    
    @Column(name = "is_hand_raised")
    private Boolean isHandRaised = false;
    
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();
    
    @Column(name = "left_at")
    private LocalDateTime leftAt;
    
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", insertable = false, updatable = false)
    private Meeting meeting;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }
    
    public void leave() {
        this.leftAt = LocalDateTime.now();
        if (joinedAt != null) {
            long duration = java.time.Duration.between(joinedAt, leftAt).getSeconds();
            this.durationSeconds = (int) duration;
        }
    }
}
```

### MeetingSettings.java
```java
package com.synergyhub.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "meeting_settings")
@Data
public class MeetingSettings {
    
    @Id
    @Column(name = "meeting_id", length = 36)
    private String meetingId;
    
    @Column(name = "allow_participant_screen_share")
    private Boolean allowParticipantScreenShare = true;
    
    @Column(name = "allow_participant_chat")
    private Boolean allowParticipantChat = true;
    
    @Column(name = "mute_participants_on_entry")
    private Boolean muteParticipantsOnEntry = false;
    
    @Column(name = "waiting_room_enabled")
    private Boolean waitingRoomEnabled = false;
    
    @Column(name = "recording_enabled")
    private Boolean recordingEnabled = true;
    
    @Column(name = "max_duration_minutes")
    private Integer maxDurationMinutes = 60;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;
}
```

---

## 5. WebRTC Signaling Server

### WebSocket Configuration
```java
package com.synergyhub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebRTCSignalingConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/meeting")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

### Signaling Controller
```java
package com.synergyhub.controller;

import com.synergyhub.dto.webrtc.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class WebRTCSignalingController {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Participant joins meeting room
     */
    @MessageMapping("/meeting.join")
    public void joinMeeting(
            @Payload JoinMeetingMessage message,
            @DestinationVariable String meetingId,
            Principal principal) {
        
        // Notify all other participants that someone joined
        messagingTemplate.convertAndSend(
            "/topic/meeting/" + meetingId + "/participants",
            new ParticipantJoinedMessage(
                message.getUserId(),
                message.getUserName(),
                message.getSocketId()
            )
        );
    }
    
    /**
     * Send WebRTC offer
     */
    @MessageMapping("/meeting.offer")
    public void handleOffer(
            @Payload RTCOfferMessage offer,
            @DestinationVariable String meetingId) {
        
        // Forward offer to specific participant
        messagingTemplate.convertAndSendToUser(
            offer.getTargetUserId().toString(),
            "/queue/meeting/" + meetingId + "/offer",
            offer
        );
    }
    
    /**
     * Send WebRTC answer
     */
    @MessageMapping("/meeting.answer")
    public void handleAnswer(
            @Payload RTCAnswerMessage answer,
            @DestinationVariable String meetingId) {
        
        // Forward answer to specific participant
        messagingTemplate.convertAndSendToUser(
            answer.getTargetUserId().toString(),
            "/queue/meeting/" + meetingId + "/answer",
            answer
        );
    }
    
    /**
     * Send ICE candidate
     */
    @MessageMapping("/meeting.iceCandidate")
    public void handleIceCandidate(
            @Payload RTCIceCandidateMessage candidate,
            @DestinationVariable String meetingId) {
        
        // Forward ICE candidate to specific participant
        messagingTemplate.convertAndSendToUser(
            candidate.getTargetUserId().toString(),
            "/queue/meeting/" + meetingId + "/iceCandidate",
            candidate
        );
    }
    
    /**
     * Participant leaves meeting
     */
    @MessageMapping("/meeting.leave")
    public void leaveMeeting(
            @Payload LeaveMeetingMessage message,
            @DestinationVariable String meetingId) {
        
        // Notify all participants
        messagingTemplate.convertAndSend(
            "/topic/meeting/" + meetingId + "/participants/left",
            new ParticipantLeftMessage(message.getUserId())
        );
    }
    
    /**
     * Toggle audio/video/screen share
     */
    @MessageMapping("/meeting.mediaState")
    public void updateMediaState(
            @Payload MediaStateMessage message,
            @DestinationVariable String meetingId) {
        
        // Broadcast to all participants
        messagingTemplate.convertAndSend(
            "/topic/meeting/" + meetingId + "/mediaState",
            message
        );
    }
}
```

### Signaling DTOs
```java
package com.synergyhub.dto.webrtc;

import lombok.Data;

@Data
public class JoinMeetingMessage {
    private Long userId;
    private String userName;
    private String socketId;
}

@Data
public class RTCOfferMessage {
    private Long userId;
    private Long targetUserId;
    private String offer; // SDP
    private String socketId;
}

@Data
public class RTCAnswerMessage {
    private Long userId;
    private Long targetUserId;
    private String answer; // SDP
}

@Data
public class RTCIceCandidateMessage {
    private Long userId;
    private Long targetUserId;
    private String candidate;
}

@Data
public class MediaStateMessage {
    private Long userId;
    private Boolean isAudioEnabled;
    private Boolean isVideoEnabled;
    private Boolean isScreenSharing;
}
```

---

## 6. Media Server Setup (Optional)

### Janus Gateway (Recommended SFU)

#### Docker Compose Setup
```yaml
version: '3.8'

services:
  janus:
    image: canyan/janus-gateway:latest
    ports:
      - "8088:8088"    # HTTP
      - "8089:8089"    # HTTPS
      - "8188:8188"    # WebSocket
      - "20000-20100:20000-20100/udp"  # RTP/RTCP
    volumes:
      - ./janus.jcfg:/usr/local/etc/janus/janus.jcfg
    environment:
      - DOCKER_IP=<your-server-ip>
```

#### Janus Configuration (janus.jcfg)
```ini
general: {
    admin_secret = "your-admin-secret"
    api_secret = "your-api-secret"
}

nat: {
    stun_server = "stun.l.google.com"
    stun_port = 19302
    turn_server = "turn:your-turn-server.com:3478"
    turn_user = "username"
    turn_pwd = "password"
}

media: {
    ipv6 = false
    rtp_port_range = "20000-20100"
}
```

### TURN Server (coturn)

#### Installation
```bash
# Ubuntu/Debian
sudo apt-get install coturn

# Enable coturn
sudo systemctl enable coturn
sudo systemctl start coturn
```

#### Configuration (/etc/turnserver.conf)
```ini
listening-port=3478
tls-listening-port=5349

external-ip=<your-server-public-ip>

realm=synergyhub.com
server-name=synergyhub.com

user=username:password

fingerprint
lt-cred-mech

# SSL certificates
cert=/etc/ssl/turn_server_cert.pem
pkey=/etc/ssl/turn_server_pkey.pem

# Logging
log-file=/var/log/turnserver.log
verbose
```

---

## 7. Repository Layer

### MeetingRepository.java
```java
package com.synergyhub.repository;

import com.synergyhub.entity.Meeting;
import com.synergyhub.entity.Meeting.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, String> {
    
    Optional<Meeting> findByMeetingCode(String meetingCode);
    
    List<Meeting> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    
    List<Meeting> findByProjectIdAndStatus(Long projectId, MeetingStatus status);
    
    @Query("SELECT m FROM Meeting m WHERE m.projectId = :projectId " +
           "AND m.scheduledStartTime BETWEEN :start AND :end")
    List<Meeting> findScheduledMeetings(Long projectId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT m FROM Meeting m WHERE m.hostUserId = :userId " +
           "AND m.status = 'IN_PROGRESS'")
    List<Meeting> findActiveHostedMeetings(Long userId);
    
    @Query("SELECT COUNT(mp) FROM MeetingParticipant mp " +
           "WHERE mp.meetingId = :meetingId AND mp.leftAt IS NULL")
    int countActiveParticipants(String meetingId);
}
```

### MeetingParticipantRepository.java
```java
package com.synergyhub.repository;

import com.synergyhub.entity.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {
    
    List<MeetingParticipant> findByMeetingIdAndLeftAtIsNull(String meetingId);
    
    Optional<MeetingParticipant> findByMeetingIdAndUserIdAndLeftAtIsNull(
        String meetingId, Long userId);
    
    List<MeetingParticipant> findByMeetingIdOrderByJoinedAtAsc(String meetingId);
    
    @Query("SELECT AVG(mp.durationSeconds) FROM MeetingParticipant mp " +
           "WHERE mp.meetingId = :meetingId AND mp.durationSeconds IS NOT NULL")
    Double getAverageDuration(String meetingId);
}
```

---

## 8. Service Layer

### MeetingService.java
```java
package com.synergyhub.service;

import com.synergyhub.dto.*;
import com.synergyhub.entity.*;
import com.synergyhub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingService {
    
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final MeetingSettingsRepository settingsRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Transactional
    public MeetingDTO createMeeting(CreateMeetingRequest request) {
        // Create meeting entity
        Meeting meeting = new Meeting();
        meeting.setProjectId(request.getProjectId());
        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());
        meeting.setHostUserId(request.getHostUserId());
        meeting.setScheduledStartTime(request.getScheduledStartTime());
        meeting.setScheduledEndTime(request.getScheduledEndTime());
        
        // Set status
        if (request.getScheduledStartTime() != null) {
            meeting.setStatus(Meeting.MeetingStatus.SCHEDULED);
        } else {
            meeting.setStatus(Meeting.MeetingStatus.IN_PROGRESS);
            meeting.setStartedAt(LocalDateTime.now());
        }
        
        meeting = meetingRepository.save(meeting);
        
        // Create default settings
        MeetingSettings settings = new MeetingSettings();
        settings.setMeetingId(meeting.getId());
        if (request.getSettings() != null) {
            // Apply custom settings
            settings.setAllowParticipantScreenShare(
                request.getSettings().getAllowParticipantScreenShare());
            settings.setMuteParticipantsOnEntry(
                request.getSettings().getMuteParticipantsOnEntry());
            // ... other settings
        }
        settingsRepository.save(settings);
        
        return toDTO(meeting);
    }
    
    @Transactional
    public MeetingDTO joinMeeting(String meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        
        // Check if meeting is active
        if (meeting.getStatus() == Meeting.MeetingStatus.ENDED) {
            throw new IllegalStateException("Meeting has ended");
        }
        
        // Check max participants
        int activeCount = meetingRepository.countActiveParticipants(meetingId);
        if (activeCount >= meeting.getMaxParticipants()) {
            throw new IllegalStateException("Meeting is full");
        }
        
        // Check if user already in meeting
        Optional<MeetingParticipant> existing = participantRepository
            .findByMeetingIdAndUserIdAndLeftAtIsNull(meetingId, userId);
        
        if (existing.isPresent()) {
            return toDTO(meeting);
        }
        
        // Add participant
        MeetingParticipant participant = new MeetingParticipant();
        participant.setMeetingId(meetingId);
        participant.setUserId(userId);
        
        // Check if should be muted on entry
        MeetingSettings settings = meeting.getSettings();
        if (settings != null && settings.getMuteParticipantsOnEntry()) {
            participant.setIsAudioEnabled(false);
        }
        
        participantRepository.save(participant);
        
        // Start meeting if scheduled
        if (meeting.getStatus() == Meeting.MeetingStatus.SCHEDULED) {
            meeting.setStatus(Meeting.MeetingStatus.IN_PROGRESS);
            meeting.setStartedAt(LocalDateTime.now());
            meetingRepository.save(meeting);
        }
        
        // Broadcast participant joined
        messagingTemplate.convertAndSend(
            "/topic/meeting/" + meetingId + "/participants",
            new ParticipantJoinedEvent(userId)
        );
        
        return toDTO(meeting);
    }
    
    @Transactional
    public void leaveMeeting(String meetingId, Long userId) {
        MeetingParticipant participant = participantRepository
            .findByMeetingIdAndUserIdAndLeftAtIsNull(meetingId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        
        participant.leave();
        participantRepository.save(participant);
        
        // Check if meeting should end (no participants left)
        int activeCount = meetingRepository.countActiveParticipants(meetingId);
        if (activeCount == 0) {
            Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
            if (meeting != null) {
                meeting.setStatus(Meeting.MeetingStatus.ENDED);
                meeting.setEndedAt(LocalDateTime.now());
                meetingRepository.save(meeting);
            }
        }
        
        // Broadcast participant left
        messagingTemplate.convertAndSend(
            "/topic/meeting/" + meetingId + "/participants/left",
            new ParticipantLeftEvent(userId)
        );
    }
    
    public MeetingDTO getMeeting(String meetingIdOrCode) {
        Meeting meeting = meetingRepository.findById(meetingIdOrCode)
            .or(() -> meetingRepository.findByMeetingCode(meetingIdOrCode))
            .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        
        return toDTO(meeting);
    }
    
    public List<MeetingDTO> getProjectMeetings(Long projectId) {
        List<Meeting> meetings = meetingRepository
            .findByProjectIdOrderByCreatedAtDesc(projectId);
        
        return meetings.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public void updateParticipantMedia(
            String meetingId, 
            Long userId, 
            MediaStateUpdate update) {
        
        MeetingParticipant participant = participantRepository
            .findByMeetingIdAndUserIdAndLeftAtIsNull(meetingId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        
        if (update.getIsAudioEnabled() != null) {
            participant.setIsAudioEnabled(update.getIsAudioEnabled());
        }
        if (update.getIsVideoEnabled() != null) {
            participant.setIsVideoEnabled(update.getIsVideoEnabled());
        }
        if (update.getIsScreenSharing() != null) {
            participant.setIsScreenSharing(update.getIsScreenSharing());
        }
        if (update.getIsHandRaised() != null) {
            participant.setIsHandRaised(update.getIsHandRaised());
        }
        
        participantRepository.save(participant);
    }
    
    private MeetingDTO toDTO(Meeting meeting) {
        // Convert entity to DTO
        MeetingDTO dto = new MeetingDTO();
        dto.setId(meeting.getId());
        dto.setProjectId(meeting.getProjectId());
        dto.setMeetingCode(meeting.getMeetingCode());
        dto.setTitle(meeting.getTitle());
        dto.setDescription(meeting.getDescription());
        dto.setStatus(meeting.getStatus().name());
        dto.setHostUserId(meeting.getHostUserId());
        // ... other fields
        
        // Load active participants
        List<MeetingParticipant> activeParticipants = participantRepository
            .findByMeetingIdAndLeftAtIsNull(meeting.getId());
        dto.setParticipants(activeParticipants.stream()
            .map(this::toParticipantDTO)
            .collect(Collectors.toList()));
        
        return dto;
    }
    
    private ParticipantDTO toParticipantDTO(MeetingParticipant participant) {
        // Convert participant entity to DTO
        ParticipantDTO dto = new ParticipantDTO();
        dto.setUserId(participant.getUserId());
        dto.setIsAudioEnabled(participant.getIsAudioEnabled());
        dto.setIsVideoEnabled(participant.getIsVideoEnabled());
        dto.setIsScreenSharing(participant.getIsScreenSharing());
        dto.setIsHandRaised(participant.getIsHandRaised());
        dto.setJoinedAt(participant.getJoinedAt());
        return dto;
    }
}
```

---

## 9. REST API Endpoints

### MeetingRestController.java
```java
package com.synergyhub.controller;

import com.synergyhub.dto.*;
import com.synergyhub.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingRestController {
    
    private final MeetingService meetingService;
    
    /**
     * POST /api/meetings
     * Create a new meeting
     */
    @PostMapping
    public ResponseEntity<MeetingDTO> createMeeting(
            @RequestBody CreateMeetingRequest request) {
        MeetingDTO meeting = meetingService.createMeeting(request);
        return ResponseEntity.ok(meeting);
    }
    
    /**
     * GET /api/meetings/{meetingId}
     * Get meeting details
     */
    @GetMapping("/{meetingIdOrCode}")
    public ResponseEntity<MeetingDTO> getMeeting(
            @PathVariable String meetingIdOrCode) {
        MeetingDTO meeting = meetingService.getMeeting(meetingIdOrCode);
        return ResponseEntity.ok(meeting);
    }
    
    /**
     * GET /api/meetings/project/{projectId}
     * Get all meetings for a project
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<MeetingDTO>> getProjectMeetings(
            @PathVariable Long projectId) {
        List<MeetingDTO> meetings = meetingService.getProjectMeetings(projectId);
        return ResponseEntity.ok(meetings);
    }
    
    /**
     * POST /api/meetings/{meetingId}/join
     * Join a meeting
     */
    @PostMapping("/{meetingId}/join")
    public ResponseEntity<MeetingDTO> joinMeeting(
            @PathVariable String meetingId,
            @RequestParam Long userId) {
        MeetingDTO meeting = meetingService.joinMeeting(meetingId, userId);
        return ResponseEntity.ok(meeting);
    }
    
    /**
     * POST /api/meetings/{meetingId}/leave
     * Leave a meeting
     */
    @PostMapping("/{meetingId}/leave")
    public ResponseEntity<Void> leaveMeeting(
            @PathVariable String meetingId,
            @RequestParam Long userId) {
        meetingService.leaveMeeting(meetingId, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * PUT /api/meetings/{meetingId}/media
     * Update participant media state
     */
    @PutMapping("/{meetingId}/media")
    public ResponseEntity<Void> updateMediaState(
            @PathVariable String meetingId,
            @RequestParam Long userId,
            @RequestBody MediaStateUpdate update) {
        meetingService.updateParticipantMedia(meetingId, userId, update);
        return ResponseEntity.ok().build();
    }
    
    /**
     * POST /api/meetings/{meetingId}/end
     * End a meeting (host only)
     */
    @PostMapping("/{meetingId}/end")
    public ResponseEntity<Void> endMeeting(
            @PathVariable String meetingId,
            @RequestParam Long userId) {
        meetingService.endMeeting(meetingId, userId);
        return ResponseEntity.ok().build();
    }
}
```

---

## 10. WebSocket Signaling

### Frontend Integration (TypeScript)
```typescript
// WebRTC Service
class WebRTCService {
  private peerConnections = new Map<number, RTCPeerConnection>();
  private stompClient: Client;
  
  constructor(private meetingId: string, private userId: number) {
    this.initializeSignaling();
  }
  
  private initializeSignaling() {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws/meeting'),
      onConnect: () => this.onSignalingConnected(),
    });
    this.stompClient.activate();
  }
  
  private onSignalingConnected() {
    // Subscribe to participant events
    this.stompClient.subscribe(
      `/topic/meeting/${this.meetingId}/participants`,
      (message) => this.handleParticipantJoined(JSON.parse(message.body))
    );
    
    // Subscribe to offers
    this.stompClient.subscribe(
      `/user/queue/meeting/${this.meetingId}/offer`,
      (message) => this.handleOffer(JSON.parse(message.body))
    );
    
    // Subscribe to answers
    this.stompClient.subscribe(
      `/user/queue/meeting/${this.meetingId}/answer`,
      (message) => this.handleAnswer(JSON.parse(message.body))
    );
    
    // Subscribe to ICE candidates
    this.stompClient.subscribe(
      `/user/queue/meeting/${this.meetingId}/iceCandidate`,
      (message) => this.handleIceCandidate(JSON.parse(message.body))
    );
    
    // Announce joining
    this.stompClient.publish({
      destination: `/app/meeting.join`,
      body: JSON.stringify({
        userId: this.userId,
        meetingId: this.meetingId,
      }),
    });
  }
  
  async createPeerConnection(targetUserId: number): Promise<RTCPeerConnection> {
    const config: RTCConfiguration = {
      iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        {
          urls: 'turn:your-turn-server.com:3478',
          username: 'username',
          credential: 'password',
        },
      ],
    };
    
    const pc = new RTCPeerConnection(config);
    
    // Handle ICE candidates
    pc.onicecandidate = (event) => {
      if (event.candidate) {
        this.stompClient.publish({
          destination: '/app/meeting.iceCandidate',
          body: JSON.stringify({
            userId: this.userId,
            targetUserId,
            candidate: JSON.stringify(event.candidate),
          }),
        });
      }
    };
    
    // Handle remote stream
    pc.ontrack = (event) => {
      this.onRemoteStream(targetUserId, event.streams[0]);
    };
    
    this.peerConnections.set(targetUserId, pc);
    return pc;
  }
  
  async handleParticipantJoined(participant: any) {
    if (participant.userId === this.userId) return;
    
    // Create peer connection and send offer
    const pc = await this.createPeerConnection(participant.userId);
    
    // Add local stream
    const localStream = await this.getLocalStream();
    localStream.getTracks().forEach((track) => {
      pc.addTrack(track, localStream);
    });
    
    // Create and send offer
    const offer = await pc.createOffer();
    await pc.setLocalDescription(offer);
    
    this.stompClient.publish({
      destination: '/app/meeting.offer',
      body: JSON.stringify({
        userId: this.userId,
        targetUserId: participant.userId,
        offer: JSON.stringify(offer),
      }),
    });
  }
  
  async handleOffer(data: any) {
    const pc = await this.createPeerConnection(data.userId);
    
    // Add local stream
    const localStream = await this.getLocalStream();
    localStream.getTracks().forEach((track) => {
      pc.addTrack(track, localStream);
    });
    
    // Set remote description
    await pc.setRemoteDescription(JSON.parse(data.offer));
    
    // Create and send answer
    const answer = await pc.createAnswer();
    await pc.setLocalDescription(answer);
    
    this.stompClient.publish({
      destination: '/app/meeting.answer',
      body: JSON.stringify({
        userId: this.userId,
        targetUserId: data.userId,
        answer: JSON.stringify(answer),
      }),
    });
  }
  
  async handleAnswer(data: any) {
    const pc = this.peerConnections.get(data.userId);
    if (pc) {
      await pc.setRemoteDescription(JSON.parse(data.answer));
    }
  }
  
  async handleIceCandidate(data: any) {
    const pc = this.peerConnections.get(data.userId);
    if (pc) {
      await pc.addIceCandidate(JSON.parse(data.candidate));
    }
  }
}
```

---

## 11. Security & Authentication

### WebSocket Security
```java
@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {
    
    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
            .simpDestMatchers("/app/meeting.*").authenticated()
            .simpSubscribeDestMatchers("/topic/meeting/**").authenticated()
            .simpSubscribeDestMatchers("/user/queue/**").authenticated()
            .anyMessage().denyAll();
    }
    
    @Override
    protected boolean sameOriginDisabled() {
        return true; // Disable CSRF for WebSocket
    }
}
```

---

## 12. Recording & Storage

### Recording Service
```java
@Service
public class RecordingService {
    
    @Autowired
    private S3Client s3Client;
    
    public void startRecording(String meetingId) {
        // Implement recording logic
        // Option 1: Use Janus recording plugin
        // Option 2: Use Kurento recorder
        // Option 3: Use server-side screen capture
    }
    
    public String stopRecording(String meetingId) {
        // Stop recording and upload to S3
        String recordingPath = "/tmp/recording-" + meetingId + ".mp4";
        String s3Key = "recordings/" + meetingId + ".mp4";
        
        s3Client.putObject(PutObjectRequest.builder()
            .bucket("synergyhub-recordings")
            .key(s3Key)
            .build(),
            RequestBody.fromFile(new File(recordingPath)));
        
        return "https://recordings.synergyhub.com/" + s3Key;
    }
}
```

---

## 13. Performance & Scalability

### Scaling Strategy
1. **Horizontal Scaling**: Deploy multiple signaling servers behind load balancer
2. **Redis Session Sharing**: Share WebSocket sessions across servers
3. **SFU Scaling**: Deploy multiple Janus instances with load distribution
4. **Database Optimization**: Index frequently queried columns, use read replicas

### Redis Configuration for Session Sharing
```java
@Configuration
public class RedisWebSocketConfig {
    
    @Bean
    public MessageChannel brokerChannel() {
        return new ExecutorSubscribableChannel();
    }
    
    @Bean
    public MessageChannel clientInboundChannel() {
        return new ExecutorSubscribableChannel();
    }
    
    @Bean
    public MessageChannel clientOutboundChannel() {
        return new ExecutorSubscribableChannel();
    }
}
```

---

## 14. Testing Strategy

### Unit Tests
```java
@SpringBootTest
class MeetingServiceTest {
    
    @Autowired
    private MeetingService meetingService;
    
    @MockBean
    private MeetingRepository meetingRepository;
    
    @Test
    void testCreateMeeting() {
        CreateMeetingRequest request = new CreateMeetingRequest();
        request.setProjectId(1L);
        request.setTitle("Test Meeting");
        request.setHostUserId(1L);
        
        MeetingDTO result = meetingService.createMeeting(request);
        
        assertNotNull(result);
        assertEquals("Test Meeting", result.getTitle());
        assertNotNull(result.getMeetingCode());
    }
}
```

### Integration Tests
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class WebRTCSignalingIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    private StompSession stompSession;
    
    @Test
    void testSignalingFlow() throws Exception {
        // Connect to WebSocket
        WebSocketStompClient stompClient = new WebSocketStompClient(
            new SockJsClient(createTransportClient())
        );
        
        stompSession = stompClient.connect(
            "ws://localhost:" + port + "/ws/meeting",
            new StompSessionHandlerAdapter() {}
        ).get(1, TimeUnit.SECONDS);
        
        // Test offer/answer exchange
        // ...
    }
}
```

---

## 15. Deployment Guide

### Production Checklist
- [ ] Deploy TURN server with SSL certificates
- [ ] Configure firewall rules for UDP ports (20000-20100)
- [ ] Set up Janus Gateway with clustering
- [ ] Configure Redis for session management
- [ ] Set up MinIO/S3 for recording storage
- [ ] Configure monitoring (Prometheus + Grafana)
- [ ] Set up logging (ELK stack)
- [ ] Configure CDN for static assets
- [ ] Load test with 50+ concurrent participants
- [ ] Set up auto-scaling for signaling servers

### Nginx Configuration
```nginx
upstream signaling_backend {
    server signaling1:8080;
    server signaling2:8080;
}

server {
    listen 443 ssl http2;
    server_name meet.synergyhub.com;
    
    ssl_certificate /etc/ssl/certs/synergyhub.crt;
    ssl_certificate_key /etc/ssl/private/synergyhub.key;
    
    location /ws/meeting {
        proxy_pass http://signaling_backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
    
    location /api/meetings {
        proxy_pass http://signaling_backend;
    }
}
```

---

## Implementation Timeline

### Phase 1: Basic P2P (Week 1-2)
- [ ] Database schema and entities
- [ ] REST API for meetings
- [ ] WebSocket signaling server
- [ ] Basic WebRTC implementation (2 participants)

### Phase 2: Full Features (Week 3-4)
- [ ] Multi-party support (mesh, up to 6 participants)
- [ ] Screen sharing
- [ ] Chat in meeting
- [ ] Hand raise

### Phase 3: Production Ready (Week 5-6)
- [ ] SFU integration (Janus)
- [ ] TURN server setup
- [ ] Recording functionality
- [ ] Security hardening
- [ ] Load testing

### Phase 4: Polish (Week 7-8)
- [ ] UI/UX improvements
- [ ] Analytics and monitoring
- [ ] Documentation
- [ ] User testing and feedback

---

## Conclusion

This implementation provides enterprise-grade video conferencing functionality similar to Google Meet. The architecture supports scaling from small meetings (2-6 participants with P2P) to large meetings (50+ participants with SFU).

**Key Features:**
- WebRTC for real-time communication
- Mesh architecture for simplicity, SFU for scalability
- TURN server for NAT traversal
- Meeting recording with S3 storage
- Comprehensive security with JWT authentication
- Production-ready with monitoring and logging

**Next Steps:**
1. Review and approve architecture
2. Set up development environment
3. Implement Phase 1 (Basic P2P)
4. Test with real users
5. Deploy Phase 2 with SFU
6. Production launch
