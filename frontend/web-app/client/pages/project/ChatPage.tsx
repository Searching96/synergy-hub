import { useState, useEffect, useCallback } from "react";
import { useParams } from "react-router-dom";
import { useProject } from "@/context/ProjectContext";
import { ProjectChatRoom } from "@/components/chat/ProjectChatRoom";
import { AlertCircle } from "lucide-react";
import { chatService } from "@/services/chat.service";
import authService from "@/services/auth.service";
import type { ChatMessage } from "@/types/chat.types";
import { ProjectBreadcrumb } from "@/components/project/ProjectBreadcrumb";
import { useToast } from "@/hooks/use-toast";
import { useProjectChatWebSocket } from "@/hooks/useWebSocket";

export default function ChatPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { project } = useProject();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const { toast } = useToast();

  const currentUser = authService.getCurrentUser();
  const currentUserId = currentUser?.id || 0;

  // Handle incoming WebSocket messages
  const handleWebSocketMessage = useCallback((message: ChatMessage) => {
    setMessages((prev) => {
      // 1. Avoid duplicates if the real ID is already present
      if (prev.some((m) => m.id === message.id)) {
        return prev;
      }

      // 2. Look for an optimistic message from this user with the same content
      // and a temporary ID (Date.now() based IDs are much larger than backend IDs)
      // This is a heuristic until we have client-side UUIDs
      if (message.userId === currentUserId) {
        // Use slice().reverse() to find the last occurrence without findLastIndex
        const reversedIdx = [...prev].reverse().findIndex(
          (m) => m.userId === currentUserId && m.id > 1000000000000 && m.message === message.message
        );

        if (reversedIdx !== -1) {
          const optimisticIdx = prev.length - 1 - reversedIdx;
          const newMessages = [...prev];
          newMessages[optimisticIdx] = message;
          return newMessages;
        }
      }

      return [...prev, message];
    });
  }, [currentUserId]);

  // WebSocket connection with automatic reconnect
  const { isConnected } = useProjectChatWebSocket({
    projectId: projectId ? parseInt(projectId) : 0,
    onMessage: handleWebSocketMessage,
    enabled: !!projectId,
  });

  useEffect(() => {
    if (projectId) {
      loadMessages();

      // Polling fallback: Only poll if WebSocket is not connected
      const intervalId = setInterval(() => {
        if (!isConnected) {
          loadMessages(true); // silent load
        }
      }, 5000); // Slower polling when WS is available

      return () => clearInterval(intervalId);
    }
  }, [projectId, isConnected]);

  const loadMessages = async (silent = false) => {
    if (!silent) setIsLoading(true);
    try {
      const projectMessages = await chatService.getProjectMessages(parseInt(projectId!));
      setMessages(projectMessages);
    } catch (err) {
      console.error(err);
      if (!silent) {
        toast({
          title: "Error",
          description: "Failed to load messages",
          variant: "destructive",
        });
      }
    } finally {
      if (!silent) setIsLoading(false);
    }
  };

  const handleSendMessage = async (message: string, replyToId?: number) => {
    if (!project || !currentUser) return;

    // 1. Optimistic Update
    const tempId = Date.now(); // Large temporary ID
    const optimisticMessage: ChatMessage = {
      id: tempId,
      projectId: parseInt(projectId!),
      userId: currentUserId,
      user: {
        id: currentUserId,
        name: currentUser.name || "Me",
        avatar: undefined,
      },
      message: message,
      timestamp: new Date().toISOString(),
      reactions: [],
      attachments: []
    };

    setMessages((prev) => [...prev, optimisticMessage]);

    try {
      // 2. Network Request
      const newMessage = await chatService.sendMessage({
        projectId: parseInt(projectId!),
        message: message,
        replyToId
      });

      // 3. Reconcile
      setMessages((prev) => {
        // Check if the WebSocket already handled this message
        if (prev.some((msg) => msg.id === newMessage.id)) {
          // If real message exists, just remove our temp one
          return prev.filter((msg) => msg.id !== tempId);
        }
        // Otherwise, replace the temp one with the real one
        return prev.map((msg) => (msg.id === tempId ? newMessage : msg));
      });
    } catch (error) {
      console.error("Failed to send message:", error);
      // Revert optimistic update
      setMessages((prev) => prev.filter((msg) => msg.id !== tempId));
      toast({
        title: "Error",
        description: "Failed to send message. Please try again.",
        variant: "destructive",
      });
    }
  };

  const handleEditMessage = async (messageId: number, newText: string) => {
    toast({
      title: "Not Implemented",
      description: "Message editing coming soon!",
    });
  };

  const handleDeleteMessage = async (messageId: number) => {
    toast({
      title: "Not Implemented",
      description: "Message deletion coming soon!",
    });
  };

  const handleReactToMessage = async (messageId: number, emoji: string) => {
    toast({
      title: "Not Implemented",
      description: "Reactions coming soon!",
    });
  };

  if (!projectId) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-center">
          <AlertCircle className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
          <p className="text-muted-foreground">Project not found</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 h-full">
      <div className="mb-4">
        <ProjectBreadcrumb current="Chat" />
      </div>
      <div className="h-[calc(100%-3rem)]">
        <ProjectChatRoom
          projectId={parseInt(projectId)}
          projectName={project?.name || "Project"}
          currentUserId={currentUserId}
          messages={messages}
          onSendMessage={handleSendMessage}
          onEditMessage={handleEditMessage}
          onDeleteMessage={handleDeleteMessage}
          onReactToMessage={handleReactToMessage}
          isLoading={isLoading}
        />
      </div>
    </div>
  );
}
