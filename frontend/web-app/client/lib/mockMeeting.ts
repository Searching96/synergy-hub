/**
 * Mock Meeting Data and Utilities
 * Simulates video conferencing functionality until WebRTC backend is implemented
 */

import type { Meeting, MeetingParticipant, MeetingSettings, CreateMeetingRequest } from "@/types/meeting.types";

/**
 * Generate random meeting code (e.g., "veh-sjar-kyk")
 */
export function generateMeetingCode(): string {
  const characters = "abcdefghijklmnopqrstuvwxyz";
  const segments = 3;
  const segmentLength = 3;
  
  return Array.from({ length: segments }, () =>
    Array.from({ length: segmentLength }, () =>
      characters[Math.floor(Math.random() * characters.length)]
    ).join("")
  ).join("-");
}

/**
 * Default meeting settings
 */
const defaultMeetingSettings: MeetingSettings = {
  allowParticipantScreenShare: true,
  allowParticipantChat: true,
  muteParticipantsOnEntry: false,
  waitingRoomEnabled: false,
  recordingEnabled: false,
  maxDuration: 60, // 60 minutes
};

/**
 * Mock participants
 */
export const mockParticipants: MeetingParticipant[] = [
  {
    id: 1,
    userId: 1,
    userName: "John Doe",
    email: "john@example.com",
    isAudioEnabled: true,
    isVideoEnabled: true,
    isScreenSharing: false,
    isHandRaised: false,
    isSpeaking: false,
    joinedAt: new Date(Date.now() - 600000).toISOString(),
  },
  {
    id: 2,
    userId: 2,
    userName: "Jane Smith",
    email: "jane@example.com",
    isAudioEnabled: true,
    isVideoEnabled: false,
    isScreenSharing: false,
    isHandRaised: false,
    isSpeaking: false,
    joinedAt: new Date(Date.now() - 480000).toISOString(),
  },
  {
    id: 3,
    userId: 3,
    userName: "Bob Johnson",
    email: "bob@example.com",
    isAudioEnabled: false,
    isVideoEnabled: true,
    isScreenSharing: false,
    isHandRaised: true,
    isSpeaking: false,
    joinedAt: new Date(Date.now() - 300000).toISOString(),
  },
];

/**
 * In-memory storage for mock meetings
 */
const meetingStorage = new Map<string, Meeting>();

/**
 * Initialize with a mock meeting
 */
const mockMeeting: Meeting = {
  id: "meeting-1",
  projectId: 1,
  projectName: "SynergyHub",
  meetingCode: "veh-sjar-kyk",
  title: "Sprint Planning Meeting",
  description: "Discuss upcoming sprint goals and tasks",
  status: "IN_PROGRESS",
  hostUserId: 1,
  participants: mockParticipants,
  maxParticipants: 50,
  isRecording: false,
  settings: defaultMeetingSettings,
  startedAt: new Date(Date.now() - 600000).toISOString(),
};

meetingStorage.set(mockMeeting.id, mockMeeting);
meetingStorage.set(mockMeeting.meetingCode, mockMeeting);

/**
 * Create a new meeting (mock)
 */
export async function mockCreateMeeting(
  request: CreateMeetingRequest
): Promise<Meeting> {
  await new Promise((resolve) => setTimeout(resolve, 300));

  const meetingCode = generateMeetingCode();
  const meetingId = `meeting-${Date.now()}`;

  const newMeeting: Meeting = {
    id: meetingId,
    projectId: request.projectId,
    projectName: "Project",
    meetingCode,
    title: request.title,
    description: request.description,
    scheduledStartTime: request.scheduledStartTime,
    scheduledEndTime: request.scheduledEndTime,
    status: request.scheduledStartTime ? "SCHEDULED" : "IN_PROGRESS",
    hostUserId: 1, // TODO: Get from auth context
    participants: [],
    maxParticipants: 50,
    isRecording: false,
    settings: { ...defaultMeetingSettings, ...request.settings },
    startedAt: request.scheduledStartTime ? undefined : new Date().toISOString(),
  };

  meetingStorage.set(meetingId, newMeeting);
  meetingStorage.set(meetingCode, newMeeting);

  return newMeeting;
}

/**
 * Join a meeting (mock)
 */
export async function mockJoinMeeting(
  meetingId: string,
  userId: number,
  userName: string,
  audioEnabled: boolean = true,
  videoEnabled: boolean = true
): Promise<Meeting> {
  await new Promise((resolve) => setTimeout(resolve, 200));

  const meeting = meetingStorage.get(meetingId);
  if (!meeting) {
    throw new Error("Meeting not found");
  }

  // Check if user already in meeting
  if (meeting.participants.some((p) => p.userId === userId)) {
    return meeting;
  }

  // Add participant
  const newParticipant: MeetingParticipant = {
    id: Date.now(),
    userId,
    userName,
    email: `${userName.toLowerCase().replace(" ", ".")}@example.com`,
    isAudioEnabled: audioEnabled,
    isVideoEnabled: videoEnabled,
    isScreenSharing: false,
    isHandRaised: false,
    isSpeaking: false,
    joinedAt: new Date().toISOString(),
  };

  meeting.participants.push(newParticipant);

  // Update meeting status if needed
  if (meeting.status === "SCHEDULED" && !meeting.startedAt) {
    meeting.status = "IN_PROGRESS";
    meeting.startedAt = new Date().toISOString();
  }

  return meeting;
}

/**
 * Leave a meeting (mock)
 */
export async function mockLeaveMeeting(
  meetingId: string,
  userId: number
): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 200));

  const meeting = meetingStorage.get(meetingId);
  if (!meeting) {
    return;
  }

  // Remove participant
  meeting.participants = meeting.participants.filter((p) => p.userId !== userId);

  // End meeting if no participants left
  if (meeting.participants.length === 0) {
    meeting.status = "ENDED";
    meeting.endedAt = new Date().toISOString();
  }
}

/**
 * Get meeting by ID or code (mock)
 */
export async function mockGetMeeting(
  meetingIdOrCode: string
): Promise<Meeting | null> {
  await new Promise((resolve) => setTimeout(resolve, 100));

  return meetingStorage.get(meetingIdOrCode) || null;
}

/**
 * Get all meetings for a project (mock)
 */
export async function mockGetProjectMeetings(
  projectId: number
): Promise<Meeting[]> {
  await new Promise((resolve) => setTimeout(resolve, 200));

  const meetings = Array.from(meetingStorage.values()).filter(
    (meeting) => meeting.projectId === projectId && typeof meeting.id === "string"
  );

  return meetings;
}

/**
 * Update participant audio status (mock)
 */
export async function mockUpdateParticipantAudio(
  meetingId: string,
  userId: number,
  isEnabled: boolean
): Promise<void> {
  const meeting = meetingStorage.get(meetingId);
  if (!meeting) return;

  const participant = meeting.participants.find((p) => p.userId === userId);
  if (participant) {
    participant.isAudioEnabled = isEnabled;
  }
}

/**
 * Update participant video status (mock)
 */
export async function mockUpdateParticipantVideo(
  meetingId: string,
  userId: number,
  isEnabled: boolean
): Promise<void> {
  const meeting = meetingStorage.get(meetingId);
  if (!meeting) return;

  const participant = meeting.participants.find((p) => p.userId === userId);
  if (participant) {
    participant.isVideoEnabled = isEnabled;
  }
}

/**
 * Toggle screen sharing (mock)
 */
export async function mockToggleScreenShare(
  meetingId: string,
  userId: number,
  isSharing: boolean
): Promise<void> {
  const meeting = meetingStorage.get(meetingId);
  if (!meeting) return;

  // Stop all other screen shares
  meeting.participants.forEach((p) => {
    p.isScreenSharing = false;
  });

  const participant = meeting.participants.find((p) => p.userId === userId);
  if (participant) {
    participant.isScreenSharing = isSharing;
  }
}

/**
 * Toggle hand raise (mock)
 */
export async function mockToggleHandRaise(
  meetingId: string,
  userId: number,
  isRaised: boolean
): Promise<void> {
  const meeting = meetingStorage.get(meetingId);
  if (!meeting) return;

  const participant = meeting.participants.find((p) => p.userId === userId);
  if (participant) {
    participant.isHandRaised = isRaised;
  }
}

/**
 * Start recording (mock)
 */
export async function mockStartRecording(meetingId: string): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 500));

  const meeting = meetingStorage.get(meetingId);
  if (meeting) {
    meeting.isRecording = true;
  }
}

/**
 * Stop recording (mock)
 */
export async function mockStopRecording(meetingId: string): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 500));

  const meeting = meetingStorage.get(meetingId);
  if (meeting) {
    meeting.isRecording = false;
    meeting.recordingUrl = `https://recordings.synergyhub.com/${meetingId}.mp4`;
  }
}

/**
 * End meeting (mock)
 */
export async function mockEndMeeting(meetingId: string): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 300));

  const meeting = meetingStorage.get(meetingId);
  if (meeting) {
    meeting.status = "ENDED";
    meeting.endedAt = new Date().toISOString();
    meeting.participants = [];
  }
}

/**
 * Calculate meeting duration in seconds
 */
export function calculateMeetingDuration(meeting: Meeting): number {
  if (!meeting.startedAt) return 0;

  const startTime = new Date(meeting.startedAt).getTime();
  const endTime = meeting.endedAt
    ? new Date(meeting.endedAt).getTime()
    : Date.now();

  return Math.floor((endTime - startTime) / 1000);
}

/**
 * Format meeting duration for display
 */
export function formatMeetingDuration(seconds: number): string {
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;

  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
  }
  return `${minutes}:${String(secs).padStart(2, "0")}`;
}

/**
 * Check if user can start meeting
 */
export function canStartMeeting(meeting: Meeting, userId: number): boolean {
  return meeting.hostUserId === userId && meeting.status === "SCHEDULED";
}

/**
 * Check if user can end meeting
 */
export function canEndMeeting(meeting: Meeting, userId: number): boolean {
  return meeting.hostUserId === userId && meeting.status === "IN_PROGRESS";
}

/**
 * Get active meetings count for project
 */
export function getActiveMeetingsCount(projectId: number): number {
  return Array.from(meetingStorage.values()).filter(
    (meeting) =>
      meeting.projectId === projectId &&
      meeting.status === "IN_PROGRESS" &&
      typeof meeting.id === "string"
  ).length;
}
