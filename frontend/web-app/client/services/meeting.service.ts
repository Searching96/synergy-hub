import api from './api';
import type { Meeting, CreateMeetingRequest, MeetingStatus } from '@/types/meeting.types';

export const meetingService = {
    createMeeting: async (data: CreateMeetingRequest & { isInstant?: boolean }): Promise<Meeting> => {
        const payload = {
            projectId: data.projectId,
            title: data.title,
            description: data.description,
            scheduledAt: data.scheduledStartTime,
            isInstant: data.isInstant
        };
        const response = await api.post('/meetings', payload);
        return mapToMeeting(response.data.data);
    },

    getProjectMeetings: async (projectId: number): Promise<Meeting[]> => {
        const response = await api.get(`/meetings/project/${projectId}`);
        return response.data.data.map(mapToMeeting);
    },

    getMeeting: async (meetingId: string): Promise<Meeting> => {
        const response = await api.get(`/meetings/${meetingId}`);
        return mapToMeeting(response.data.data);
    },

    joinMeeting: async (meetingId: string): Promise<Meeting> => {
        const response = await api.post(`/meetings/${meetingId}/join`);
        return mapToMeeting(response.data.data);
    },

    getJoinToken: async (meetingId: string): Promise<string> => {
        const response = await api.get(`/meetings/${meetingId}/token`);
        return response.data.data;
    }
};

function mapToMeeting(dto: any): Meeting {
    return {
        id: dto.id.toString(),
        projectId: dto.projectId,
        projectName: dto.projectName,
        meetingCode: dto.meetingCode,
        title: dto.title,
        description: dto.description,
        scheduledStartTime: dto.scheduledAt,
        startedAt: dto.startedAt,
        endedAt: dto.endedAt,
        status: dto.status as MeetingStatus,
        hostUserId: dto.organizerId,
        participants: (dto.participants || []).map((p: any) => ({
            id: p.id,
            userId: p.id,
            userName: p.name,
            email: p.email,
            isAudioEnabled: false,
            isVideoEnabled: false,
            isScreenSharing: false,
            isHandRaised: false,
            isSpeaking: false,
        })),
        maxParticipants: dto.maxParticipants || 50,
        isRecording: dto.isRecording || false,
    } as Meeting;
}
