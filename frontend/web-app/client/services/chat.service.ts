import api from './api';
import type { ChatMessage, SendMessageRequest } from '@/types/chat.types';

export const chatService = {
    getProjectMessages: async (projectId: number): Promise<ChatMessage[]> => {
        const response = await api.get(`/chat/project/${projectId}/messages`);
        return response.data.data.map(mapToChatMessage);
    },

    sendMessage: async (data: SendMessageRequest): Promise<ChatMessage> => {
        const response = await api.post('/chat/messages', {
            content: data.message,
            projectId: data.projectId,
            // replyToId: data.replyToId // Backend doesn't support replies yet
        });
        return mapToChatMessage(response.data.data);
    },
};

// Mapper to convert Backend DTO to Frontend Type
// Backend: { id, content, sentAt, userId, userName, userAvatar, channelId }
// Frontend: { id, projectId, userId, user: {id, name, avatar}, message, timestamp, ... }
function mapToChatMessage(dto: any): ChatMessage {
    return {
        id: dto.id,
        projectId: 0, // Not provided in message DTO, usually known by context
        userId: dto.userId,
        user: {
            id: dto.userId,
            name: dto.userName,
            avatar: dto.userAvatar
        },
        message: dto.content,
        timestamp: dto.sentAt,
        reactions: [], // Not implemented backend side yet
        attachments: [] // Not implemented backend side yet
    };
}
