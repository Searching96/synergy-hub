import { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import { useProject } from "@/context/ProjectContext";
import { ProjectChatRoom } from "@/components/chat/ProjectChatRoom";
import { AlertCircle } from "lucide-react";
import { 
  getProjectMessages, 
  mockSendMessage, 
  mockEditMessage, 
  mockDeleteMessage, 
  mockAddReaction 
} from "@/lib/mockChat";
import type { ChatMessage } from "@/types/chat.types";

export default function ChatPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { project } = useProject();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // TODO: Get current user ID from auth context
  const currentUserId = 1;

  useEffect(() => {
    if (projectId) {
      loadMessages();
    }
  }, [projectId]);

  const loadMessages = () => {
    setIsLoading(true);
    const projectMessages = getProjectMessages(parseInt(projectId!));
    setMessages(projectMessages);
    setIsLoading(false);
  };

  const handleSendMessage = async (message: string, replyToId?: number) => {
    try {
      const newMessage = await mockSendMessage(
        parseInt(projectId!),
        currentUserId,
        message,
        replyToId
      );
      setMessages([...messages, newMessage]);
    } catch (error) {
      console.error("Failed to send message:", error);
    }
  };

  const handleEditMessage = async (messageId: number, newText: string) => {
    try {
      const updatedMessage = await mockEditMessage(messageId, newText);
      setMessages(messages.map(m => m.id === messageId ? updatedMessage : m));
    } catch (error) {
      console.error("Failed to edit message:", error);
    }
  };

  const handleDeleteMessage = async (messageId: number) => {
    try {
      await mockDeleteMessage(messageId);
      setMessages(messages.filter(m => m.id !== messageId));
    } catch (error) {
      console.error("Failed to delete message:", error);
    }
  };

  const handleReactToMessage = async (messageId: number, emoji: string) => {
    try {
      const updatedMessage = await mockAddReaction(messageId, currentUserId, emoji);
      setMessages(messages.map(m => m.id === messageId ? updatedMessage : m));
    } catch (error) {
      console.error("Failed to add reaction:", error);
    }
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
    <div className="h-[calc(100vh-200px)]">
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
  );
}
