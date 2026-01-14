/**
 * Mock Chat Data and Utilities
 * Simulates real-time chat functionality until WebSocket backend is implemented
 */

import type { ChatMessage, ChatUser, ChatRoom } from "@/types/chat.types";

/**
 * Mock users for chat
 */
export const mockChatUsers: ChatUser[] = [
  { id: 1, name: "John Doe", email: "john@example.com" },
  { id: 2, name: "Jane Smith", email: "jane@example.com" },
  { id: 3, name: "Bob Johnson", email: "bob@example.com" },
  { id: 4, name: "Alice Williams", email: "alice@example.com" },
];

/**
 * Mock chat messages
 */
export const mockChatMessages: ChatMessage[] = [
  {
    id: 1,
    projectId: 1,
    userId: 1,
    user: mockChatUsers[0],
    message: "Hey team! Just kicked off the user authentication feature. Let me know if you have any questions.",
    timestamp: new Date(Date.now() - 3600000).toISOString(),
  },
  {
    id: 2,
    projectId: 1,
    userId: 2,
    user: mockChatUsers[1],
    message: "Sounds good! I'll start working on the dashboard layout today.",
    timestamp: new Date(Date.now() - 3000000).toISOString(),
  },
  {
    id: 3,
    projectId: 1,
    userId: 3,
    user: mockChatUsers[2],
    message: "Quick question - are we using JWT for authentication?",
    timestamp: new Date(Date.now() - 2500000).toISOString(),
    replyTo: {
      id: 1,
      userId: 1,
      userName: "John Doe",
      message: "Hey team! Just kicked off the user authentication feature.",
    },
  },
  {
    id: 4,
    projectId: 1,
    userId: 1,
    user: mockChatUsers[0],
    message: "Yes, JWT tokens with refresh token rotation. I'll update the docs shortly.",
    timestamp: new Date(Date.now() - 2400000).toISOString(),
    replyTo: {
      id: 3,
      userId: 3,
      userName: "Bob Johnson",
      message: "Quick question - are we using JWT for authentication?",
    },
  },
  {
    id: 5,
    projectId: 1,
    userId: 4,
    user: mockChatUsers[3],
    message: "I found a bug in the responsive layout. Creating a ticket now 🐛",
    timestamp: new Date(Date.now() - 1800000).toISOString(),
    reactions: [
      { emoji: "👍", users: [1, 2], count: 2 },
      { emoji: "👀", users: [3], count: 1 },
    ],
  },
  {
    id: 6,
    projectId: 1,
    userId: 2,
    user: mockChatUsers[1],
    message: "Thanks Alice! I'll take a look at it this afternoon.",
    timestamp: new Date(Date.now() - 1700000).toISOString(),
  },
  {
    id: 7,
    projectId: 1,
    userId: 1,
    user: mockChatUsers[0],
    message: "Great progress everyone! Let's sync up in the daily standup tomorrow.",
    timestamp: new Date(Date.now() - 300000).toISOString(),
    reactions: [
      { emoji: "🎉", users: [2, 3, 4], count: 3 },
    ],
  },
];

/**
 * In-memory storage for mock messages
 */
const messageStorage = new Map<number, ChatMessage[]>();
let messageIdCounter = 1000;

// Initialize with mock data
mockChatMessages.forEach((msg) => {
  const projectMessages = messageStorage.get(msg.projectId) || [];
  projectMessages.push(msg);
  messageStorage.set(msg.projectId, projectMessages);
  messageIdCounter = Math.max(messageIdCounter, msg.id);
});

/**
 * Get messages for a project
 */
export function getProjectMessages(projectId: number): ChatMessage[] {
  return messageStorage.get(projectId) || [];
}

/**
 * Send a message (mock)
 */
export async function mockSendMessage(
  projectId: number,
  userId: number,
  message: string,
  replyToId?: number
): Promise<ChatMessage> {
  // Simulate network delay
  await new Promise((resolve) => setTimeout(resolve, 300));

  const user = mockChatUsers.find((u) => u.id === userId) || mockChatUsers[0];
  
  let replyTo = undefined;
  if (replyToId) {
    const messages = getProjectMessages(projectId);
    const replyMessage = messages.find((m) => m.id === replyToId);
    if (replyMessage) {
      replyTo = {
        id: replyMessage.id,
        userId: replyMessage.userId,
        userName: replyMessage.user.name,
        message: replyMessage.message,
      };
    }
  }

  const newMessage: ChatMessage = {
    id: ++messageIdCounter,
    projectId,
    userId,
    user,
    message,
    timestamp: new Date().toISOString(),
    replyTo,
  };

  const projectMessages = messageStorage.get(projectId) || [];
  projectMessages.push(newMessage);
  messageStorage.set(projectId, projectMessages);

  return newMessage;
}

/**
 * Edit a message (mock)
 */
export async function mockEditMessage(
  messageId: number,
  newText: string
): Promise<ChatMessage> {
  await new Promise((resolve) => setTimeout(resolve, 200));

  for (const [projectId, messages] of messageStorage.entries()) {
    const message = messages.find((m) => m.id === messageId);
    if (message) {
      message.message = newText;
      message.edited = true;
      message.editedAt = new Date().toISOString();
      return message;
    }
  }

  throw new Error("Message not found");
}

/**
 * Delete a message (mock)
 */
export async function mockDeleteMessage(messageId: number): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 200));

  for (const [projectId, messages] of messageStorage.entries()) {
    const index = messages.findIndex((m) => m.id === messageId);
    if (index !== -1) {
      messages.splice(index, 1);
      messageStorage.set(projectId, messages);
      return;
    }
  }

  throw new Error("Message not found");
}

/**
 * Add reaction to message (mock)
 */
export async function mockAddReaction(
  messageId: number,
  userId: number,
  emoji: string
): Promise<ChatMessage> {
  await new Promise((resolve) => setTimeout(resolve, 100));

  for (const [projectId, messages] of messageStorage.entries()) {
    const message = messages.find((m) => m.id === messageId);
    if (message) {
      if (!message.reactions) {
        message.reactions = [];
      }

      const existingReaction = message.reactions.find((r) => r.emoji === emoji);
      if (existingReaction) {
        // Toggle reaction
        if (existingReaction.users.includes(userId)) {
          existingReaction.users = existingReaction.users.filter((id) => id !== userId);
          existingReaction.count--;
          if (existingReaction.count === 0) {
            message.reactions = message.reactions.filter((r) => r.emoji !== emoji);
          }
        } else {
          existingReaction.users.push(userId);
          existingReaction.count++;
        }
      } else {
        message.reactions.push({
          emoji,
          users: [userId],
          count: 1,
        });
      }

      return message;
    }
  }

  throw new Error("Message not found");
}

/**
 * Get chatrooms for a user (mock)
 */
export function getMockChatRooms(userId: number): ChatRoom[] {
  return [
    {
      projectId: 1,
      projectName: "SynergyHub",
      memberCount: 4,
      lastMessage: mockChatMessages[mockChatMessages.length - 1],
      unreadCount: 3,
    },
    {
      projectId: 2,
      projectName: "E-Commerce Platform",
      memberCount: 6,
      unreadCount: 0,
    },
  ];
}

/**
 * Subscribe to new messages (mock implementation)
 * In real implementation, this would use WebSocket
 */
export function subscribeToMessages(
  projectId: number,
  callback: (message: ChatMessage) => void
): () => void {
  // Mock: simulate receiving messages every 30 seconds
  const interval = setInterval(() => {
    // Random chance of receiving a message
    if (Math.random() > 0.7) {
      const randomUser = mockChatUsers[Math.floor(Math.random() * mockChatUsers.length)];
      const messages = [
        "Anyone online?",
        "Just pushed the latest changes",
        "Need help with this task",
        "Great work team! 🎉",
        "Taking a break, back in 15",
      ];
      
      const newMessage: ChatMessage = {
        id: ++messageIdCounter,
        projectId,
        userId: randomUser.id,
        user: randomUser,
        message: messages[Math.floor(Math.random() * messages.length)],
        timestamp: new Date().toISOString(),
      };

      callback(newMessage);
    }
  }, 30000);

  return () => clearInterval(interval);
}

/**
 * Format timestamp for display
 */
export function formatChatTimestamp(timestamp: string): string {
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return "Just now";
  if (minutes < 60) return `${minutes}m ago`;
  if (hours < 24) return `${hours}h ago`;
  if (days === 0) return date.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
  if (days < 7) return `${days}d ago`;
  return date.toLocaleDateString();
}

/**
 * Check if user is typing (mock)
 */
export function mockUserTyping(projectId: number, userId: number, isTyping: boolean): void {
  // In real implementation, this would broadcast via WebSocket
  console.log(`User ${userId} is ${isTyping ? "typing" : "stopped typing"} in project ${projectId}`);
}
