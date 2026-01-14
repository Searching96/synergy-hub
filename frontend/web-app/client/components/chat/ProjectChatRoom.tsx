/**
 * ProjectChatRoom Component
 * Main chatroom component for project communication
 */

import { useState, useRef, useEffect } from "react";
import { MessageSquare, Users } from "lucide-react"; // Removed Loader2
import { ScrollArea } from "@/components/ui/scroll-area";
import { Skeleton } from "@/components/ui/skeleton"; // Added Skeleton
import { ChatMessageBubble } from "@/components/chat/ChatMessageBubble";
import { ChatInput } from "@/components/chat/ChatInput";
import { Separator } from "@/components/ui/separator";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { ChatMessage } from "@/types/chat.types";
import { EmptyState } from "@/components/EmptyState";

interface ProjectChatRoomProps {
  projectId: number;
  projectName: string;
  currentUserId: number;
  messages?: ChatMessage[];
  onSendMessage?: (message: string, replyToId?: number) => void;
  onEditMessage?: (messageId: number, newText: string) => void;
  onDeleteMessage?: (messageId: number) => void;
  onReactToMessage?: (messageId: number, emoji: string) => void;
  isLoading?: boolean;
  className?: string;
}

export function ProjectChatRoom({
  projectId,
  projectName,
  currentUserId,
  messages = [],
  onSendMessage,
  onEditMessage,
  onDeleteMessage,
  onReactToMessage,
  isLoading = false,
  className,
}: ProjectChatRoomProps) {
  const [replyTo, setReplyTo] = useState<ChatMessage | null>(null);
  const scrollAreaRef = useRef<HTMLDivElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages.length]);

  const handleReply = (message: ChatMessage) => {
    setReplyTo(message);
  };

  const handleCancelReply = () => {
    setReplyTo(null);
  };

  const handleSend = (message: string, replyToId?: number) => {
    onSendMessage?.(message, replyToId);
    setReplyTo(null);
  };

  const handleEdit = (message: ChatMessage) => {
    // In a real implementation, show edit modal or inline editor
    const newText = prompt("Edit message:", message.message);
    if (newText && newText.trim() && onEditMessage) {
      onEditMessage(message.id, newText.trim());
    }
  };

  const handleDelete = (messageId: number) => {
    if (confirm("Are you sure you want to delete this message?")) {
      onDeleteMessage?.(messageId);
    }
  };

  const getUniqueUsers = () => {
    const userIds = new Set(messages.map((m) => m.userId));
    return userIds.size;
  };

  // Group messages by date
  const groupedMessages = messages.reduce((groups, message) => {
    const date = new Date(message.timestamp).toDateString();
    if (!groups[date]) {
      groups[date] = [];
    }
    groups[date].push(message);
    return groups;
  }, {} as Record<string, ChatMessage[]>);

  const formatDateHeader = (dateString: string) => {
    const date = new Date(dateString);
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    if (date.toDateString() === today.toDateString()) {
      return "Today";
    } else if (date.toDateString() === yesterday.toDateString()) {
      return "Yesterday";
    } else {
      return date.toLocaleDateString(undefined, {
        weekday: "long",
        month: "long",
        day: "numeric",
      });
    }
  };

  return (
    <div className={cn("flex flex-col h-full bg-background rounded-lg border", className)}>
      {/* Chat Header */}
      <div className="flex items-center justify-between p-4 border-b bg-muted/30">
        <div className="flex items-center gap-3">
          <div className="h-10 w-10 rounded-lg bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center">
            <MessageSquare className="h-5 w-5 text-white" />
          </div>
          <div>
            <h3 className="font-semibold">{projectName}</h3>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Users className="h-3 w-3" />
              <span>{getUniqueUsers()} participants</span>
            </div>
          </div>
        </div>
        <Badge variant="secondary">{messages.length} messages</Badge>
      </div>

      {/* Messages Area */}
      <ScrollArea className="flex-1 p-4" ref={scrollAreaRef}>
        {isLoading ? (
          <div className="space-y-4 p-4">
            <div className="flex items-start gap-3">
              <Skeleton className="h-8 w-8 rounded-full" />
              <div className="space-y-2">
                <Skeleton className="h-4 w-[200px]" />
                <Skeleton className="h-10 w-[300px]" />
              </div>
            </div>
            <div className="flex items-start gap-3 flex-row-reverse">
              <Skeleton className="h-8 w-8 rounded-full" />
              <div className="space-y-2">
                <Skeleton className="h-4 w-[150px] ml-auto" />
                <Skeleton className="h-12 w-[250px]" />
              </div>
            </div>
            <div className="flex items-start gap-3">
              <Skeleton className="h-8 w-8 rounded-full" />
              <div className="space-y-2">
                <Skeleton className="h-4 w-[180px]" />
                <Skeleton className="h-8 w-[280px]" />
              </div>
            </div>
          </div>
        ) : messages.length === 0 ? (
          <EmptyState
            icon={MessageSquare}
            title="No messages yet"
            description="Start the conversation by sending a message below."
          />
        ) : (
          <div className="space-y-6">
            {Object.entries(groupedMessages).map(([date, msgs]) => (
              <div key={date}>
                {/* Date Separator */}
                <div className="flex items-center gap-3 mb-4">
                  <Separator className="flex-1" />
                  <span className="text-xs text-muted-foreground font-medium">
                    {formatDateHeader(date)}
                  </span>
                  <Separator className="flex-1" />
                </div>

                {/* Messages for this date */}
                <div className="space-y-1">
                  {msgs.map((message) => (
                    <ChatMessageBubble
                      key={message.id}
                      message={message}
                      isCurrentUser={message.userId === currentUserId}
                      onReply={handleReply}
                      onEdit={handleEdit}
                      onDelete={handleDelete}
                      onReact={onReactToMessage}
                    />
                  ))}
                </div>
              </div>
            ))}
            <div ref={messagesEndRef} />
          </div>
        )}
      </ScrollArea>

      {/* Input Area */}
      <ChatInput
        onSend={handleSend}
        replyTo={replyTo}
        onCancelReply={handleCancelReply}
        placeholder={`Message ${projectName}...`}
      />
    </div>
  );
}

export default ProjectChatRoom;