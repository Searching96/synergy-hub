/**
 * Meeting Room Types
 * Types for video conferencing functionality
 */

export interface MeetingParticipant {
  id: number;
  userId: number;
  userName: string;
  email: string;
  avatarUrl?: string;
  isAudioEnabled: boolean;
  isVideoEnabled: boolean;
  isScreenSharing: boolean;
  isHandRaised: boolean;
  isSpeaking: boolean;
  joinedAt: string;
  videoStream?: MediaStream;
  audioStream?: MediaStream;
}

export interface Meeting {
  id: string;
  projectId: number;
  projectName: string;
  meetingCode: string; // e.g., "veh-sjar-kyk"
  title: string;
  description?: string;
  scheduledStartTime?: string;
  scheduledEndTime?: string;
  startedAt?: string;
  endedAt?: string;
  status: MeetingStatus;
  hostUserId: number;
  participants: MeetingParticipant[];
  maxParticipants: number;
  isRecording: boolean;
  recordingUrl?: string;
  settings: MeetingSettings;
}

export type MeetingStatus = "SCHEDULED" | "IN_PROGRESS" | "ENDED" | "CANCELLED";

export interface MeetingSettings {
  allowParticipantScreenShare: boolean;
  allowParticipantChat: boolean;
  muteParticipantsOnEntry: boolean;
  waitingRoomEnabled: boolean;
  recordingEnabled: boolean;
  maxDuration?: number; // in minutes
}

export interface MeetingChatMessage {
  id: number;
  meetingId: string;
  userId: number;
  userName: string;
  message: string;
  timestamp: string;
  isPrivate: boolean;
  recipientUserId?: number;
}

export interface CreateMeetingRequest {
  projectId: number;
  title: string;
  description?: string;
  scheduledStartTime?: string;
  scheduledEndTime?: string;
  settings?: Partial<MeetingSettings>;
}

export interface JoinMeetingRequest {
  meetingId: string;
  userId: number;
  audioEnabled: boolean;
  videoEnabled: boolean;
}

export interface MeetingControls {
  isAudioEnabled: boolean;
  isVideoEnabled: boolean;
  isScreenSharing: boolean;
  isHandRaised: boolean;
  isChatOpen: boolean;
  isParticipantsOpen: boolean;
  isSettingsOpen: boolean;
}

export interface ScreenShareStream {
  userId: number;
  userName: string;
  stream: MediaStream;
  startedAt: string;
}

export interface MeetingReaction {
  emoji: string;
  userId: number;
  userName: string;
  timestamp: string;
}

export interface MeetingStats {
  duration: number; // in seconds
  participantCount: number;
  peakParticipantCount: number;
  messagesCount: number;
  reactionsCount: number;
}
