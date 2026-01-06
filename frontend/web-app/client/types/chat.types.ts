/**
 * Chat Message Type Definitions
 * For project chatroom functionality
 */

export interface ChatUser {
  id: number;
  name: string;
  email?: string;
  avatar?: string;
}

export interface ChatMessage {
  id: number;
  projectId: number;
  userId: number;
  user: ChatUser;
  message: string;
  timestamp: string;
  edited?: boolean;
  editedAt?: string;
  replyTo?: {
    id: number;
    userId: number;
    userName: string;
    message: string;
  };
  attachments?: ChatAttachment[];
  reactions?: ChatReaction[];
}

export interface ChatAttachment {
  id: number;
  fileName: string;
  fileUrl: string;
  fileType: string;
  fileSize: number;
}

export interface ChatReaction {
  emoji: string;
  users: number[]; // user IDs
  count: number;
}

export interface SendMessageRequest {
  projectId: number;
  message: string;
  replyToId?: number;
}

export interface EditMessageRequest {
  messageId: number;
  message: string;
}

export interface DeleteMessageRequest {
  messageId: number;
}

export interface AddReactionRequest {
  messageId: number;
  emoji: string;
}

export interface ChatRoom {
  projectId: number;
  projectName: string;
  memberCount: number;
  lastMessage?: ChatMessage;
  unreadCount: number;
}

export interface TypingIndicator {
  userId: number;
  userName: string;
  timestamp: string;
}
