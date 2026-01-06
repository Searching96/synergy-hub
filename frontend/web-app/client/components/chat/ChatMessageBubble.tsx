/**
 * ChatMessageBubble Component
 * Displays individual chat message with user info and actions
 */

import { useState } from "react";
import { MoreVertical, Reply, Edit2, Trash2, Smile } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { cn } from "@/lib/utils";
import type { ChatMessage } from "@/types/chat.types";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

interface ChatMessageBubbleProps {
  message: ChatMessage;
  isCurrentUser: boolean;
  onReply?: (message: ChatMessage) => void;
  onEdit?: (message: ChatMessage) => void;
  onDelete?: (messageId: number) => void;
  onReact?: (messageId: number, emoji: string) => void;
}

const EMOJI_REACTIONS = ["👍", "❤️", "😊", "🎉", "🚀"];

export function ChatMessageBubble({
  message,
  isCurrentUser,
  onReply,
  onEdit,
  onDelete,
  onReact,
}: ChatMessageBubbleProps) {
  const [showActions, setShowActions] = useState(false);

  const formatTime = (timestamp: string) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return "Just now";
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    if (days < 7) return `${days}d ago`;
    return date.toLocaleDateString();
  };

  const getInitials = (name: string) => {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2);
  };

  return (
    <div
      className={cn(
        "flex gap-3 py-2 px-3 rounded-lg transition-colors",
        showActions && "bg-accent/50",
        isCurrentUser && "flex-row-reverse"
      )}
      onMouseEnter={() => setShowActions(true)}
      onMouseLeave={() => setShowActions(false)}
    >
      {/* Avatar */}
      <Avatar className="h-8 w-8 flex-shrink-0">
        <AvatarFallback className="text-xs bg-gradient-to-br from-blue-500 to-purple-500 text-white">
          {getInitials(message.user.name)}
        </AvatarFallback>
      </Avatar>

      {/* Message Content */}
      <div className={cn("flex-1 min-w-0", isCurrentUser && "flex flex-col items-end")}>
        {/* Header */}
        <div className={cn("flex items-center gap-2 mb-1", isCurrentUser && "flex-row-reverse")}>
          <span className="text-sm font-semibold">{message.user.name}</span>
          <span className="text-xs text-muted-foreground">{formatTime(message.timestamp)}</span>
          {message.edited && (
            <span className="text-xs text-muted-foreground italic">(edited)</span>
          )}
        </div>

        {/* Reply Context */}
        {message.replyTo && (
          <div className="mb-2 p-2 rounded bg-muted/50 border-l-2 border-blue-500">
            <div className="text-xs text-muted-foreground mb-0.5">
              Replying to {message.replyTo.userName}
            </div>
            <div className="text-sm text-muted-foreground truncate">
              {message.replyTo.message}
            </div>
          </div>
        )}

        {/* Message Text */}
        <div
          className={cn(
            "inline-block p-3 rounded-lg max-w-[70%]",
            isCurrentUser
              ? "bg-blue-600 text-white"
              : "bg-muted text-foreground"
          )}
        >
          <p className="text-sm whitespace-pre-wrap break-words">{message.message}</p>
        </div>

        {/* Reactions */}
        {message.reactions && message.reactions.length > 0 && (
          <div className="flex gap-1 mt-1">
            {message.reactions.map((reaction, idx) => (
              <button
                key={idx}
                onClick={() => onReact?.(message.id, reaction.emoji)}
                className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-accent hover:bg-accent/80 transition-colors text-xs"
              >
                <span>{reaction.emoji}</span>
                <span className="text-muted-foreground">{reaction.count}</span>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Actions Menu */}
      {showActions && (
        <div className="flex items-start gap-1">
          {/* Quick Reactions */}
          <div className="flex gap-0.5 bg-background border rounded-lg shadow-sm p-1">
            {EMOJI_REACTIONS.slice(0, 3).map((emoji) => (
              <Button
                key={emoji}
                size="sm"
                variant="ghost"
                className="h-6 w-6 p-0 hover:bg-accent"
                onClick={() => onReact?.(message.id, emoji)}
              >
                {emoji}
              </Button>
            ))}
          </div>

          {/* More Actions */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button size="sm" variant="ghost" className="h-6 w-6 p-0">
                <MoreVertical className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => onReply?.(message)}>
                <Reply className="h-4 w-4 mr-2" />
                Reply
              </DropdownMenuItem>
              {isCurrentUser && (
                <>
                  <DropdownMenuItem onClick={() => onEdit?.(message)}>
                    <Edit2 className="h-4 w-4 mr-2" />
                    Edit
                  </DropdownMenuItem>
                  <DropdownMenuItem
                    onClick={() => onDelete?.(message.id)}
                    className="text-red-600"
                  >
                    <Trash2 className="h-4 w-4 mr-2" />
                    Delete
                  </DropdownMenuItem>
                </>
              )}
              <DropdownMenuItem onClick={() => {}}>
                <Smile className="h-4 w-4 mr-2" />
                Add Reaction
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      )}
    </div>
  );
}
