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

      // 2. Look for optimistic message
      if (message.userId === currentUserId) {
        // Find optimistic message within 5 seconds based on content match
        // Or check for a specific temporary ID flag if we had one
        const optimisticIdx = prev.findIndex(
          (m) => (m as any)._isOptimistic &&
            m.message === message.message &&
            Math.abs(new Date(m.timestamp).getTime() - new Date(message.timestamp).getTime()) < 10000
        );

        if (optimisticIdx !== -1) {
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

  const loadMessages = useCallback(async (silent = false) => {
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
  }, [projectId, toast]);

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
  }, [projectId, isConnected, loadMessages]);

  const handleSendMessage = async (message: string, replyToId?: number) => {
    if (!project || !currentUser) return;

    // 1. Optimistic Update
    // Use a string-based temp ID cast to any to satisfy the number type constraint temporarily if needed, 
    // or just use a very large number that won't collide. 
    // UUIDs are better, but types say number. Let's stick to large number + flag for now as types might be strict.
    // Actually, user suggested UUID. I'll cast it to any to bypass strict number check or stick to user advice.
    // The user's advice: "const tempId = `temp-${crypto.randomUUID()}`;"
    const tempId = `temp-${Date.now()}-${Math.random()}` as any;

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
      attachments: [],
      ...{ _isOptimistic: true }
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
          return prev.filter((msg) => msg.id !== tempId);
        }
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
    if (!projectId) return;

    // 1. Optimistic Update
    setMessages((prev) =>
      prev.map((msg) =>
        msg.id === messageId ? { ...msg, message: newText, edited: true } : msg
      )
    );

    try {
      // 2. Network Request
      await chatService.editMessage({ messageId, message: newText });
    } catch (error) {
      console.error("Failed to edit message:", error);
      // Revert or show error - for simplicity, just show error toast
      // A better way would be to reload messages
      loadMessages(true);
      toast({
        title: "Error",
        description: "Failed to edit message. Please try again.",
        variant: "destructive",
      });
    }
  };

  const handleDeleteMessage = async (messageId: number) => {
    if (!projectId) return;

    // 1. Optimistic Update
    setMessages((prev) => prev.filter((msg) => msg.id !== messageId));

    try {
      // 2. Network Request
      await chatService.deleteMessage({ messageId });
    } catch (error) {
      console.error("Failed to delete message:", error);
      // Revert
      loadMessages(true);
      toast({
        title: "Error",
        description: "Failed to delete message. Please try again.",
        variant: "destructive",
      });
    }
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
