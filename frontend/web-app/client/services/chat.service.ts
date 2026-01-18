import api from './api';
import type { ChatMessage, SendMessageRequest, EditMessageRequest, DeleteMessageRequest } from '@/types/chat.types';

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

    editMessage: async (data: EditMessageRequest): Promise<ChatMessage> => {
        const response = await api.put(`/chat/messages/${data.messageId}`, {
            content: data.message
        });
        return mapToChatMessage(response.data.data);
    },

    deleteMessage: async (data: DeleteMessageRequest): Promise<void> => {
        await api.delete(`/chat/messages/${data.messageId}`);
    },
};

// Mapper to convert Backend DTO to Frontend Type
// Backend: { id, content, sentAt, updatedAt, userId, userName, userAvatar, channelId, edited }
// Frontend: { id, projectId, userId, user: {id, name, avatar}, message, timestamp, edited, editedAt ... }
function mapToChatMessage(dto: any): ChatMessage {
    return {
        id: dto.id,
        projectId: dto.projectId || 0,
        userId: dto.userId,
        user: {
            id: dto.userId,
            name: dto.userName,
            avatar: dto.userAvatar
        },
        message: dto.content,
        timestamp: dto.sentAt,
        edited: dto.edited,
        editedAt: dto.updatedAt,
    } as ChatMessage;
}
