/**
 * ChatInput Component
 * Input field for sending chat messages with reply context
 */

import { useState, useRef, useEffect } from "react";
import { Send, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";
import type { ChatMessage } from "@/types/chat.types";

interface ChatInputProps {
  onSend: (message: string, replyToId?: number) => void;
  replyTo?: ChatMessage | null;
  onCancelReply?: () => void;
  disabled?: boolean;
  placeholder?: string;
}

export function ChatInput({
  onSend,
  replyTo,
  onCancelReply,
  disabled = false,
  placeholder = "Type a message...",
}: ChatInputProps) {
  const [message, setMessage] = useState("");
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    // Auto-focus when reply is set
    if (replyTo && textareaRef.current) {
      textareaRef.current.focus();
    }
  }, [replyTo]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const trimmedMessage = message.trim();
    if (!trimmedMessage || disabled) return;

    onSend(trimmedMessage, replyTo?.id);
    setMessage("");
    
    if (onCancelReply) {
      onCancelReply();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    // Send on Enter (without Shift)
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }

    // Cancel reply on Escape
    if (e.key === "Escape" && replyTo && onCancelReply) {
      onCancelReply();
    }
  };

  return (
    <div className="border-t bg-background p-4">
      {/* Reply Context */}
      {replyTo && (
        <div className="mb-2 p-2 rounded-lg bg-muted border-l-2 border-blue-500 flex items-start gap-2">
          <div className="flex-1 min-w-0">
            <div className="text-xs text-muted-foreground mb-0.5">
              Replying to {replyTo.user.name}
            </div>
            <div className="text-sm truncate">{replyTo.message}</div>
          </div>
          <Button
            size="sm"
            variant="ghost"
            className="h-6 w-6 p-0"
            onClick={onCancelReply}
          >
            <X className="h-4 w-4" />
          </Button>
        </div>
      )}

      {/* Input Form */}
      <form onSubmit={handleSubmit} className="flex gap-2">
        <Textarea
          ref={textareaRef}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={disabled}
          className={cn(
            "min-h-[44px] max-h-32 resize-none",
            "focus:ring-2 focus:ring-blue-400"
          )}
          rows={1}
        />
        <Button
          type="submit"
          size="icon"
          disabled={!message.trim() || disabled}
          className="flex-shrink-0 h-[44px] w-[44px]"
        >
          <Send className="h-4 w-4" />
        </Button>
      </form>

      <div className="text-xs text-muted-foreground mt-2">
        Press <kbd className="px-1 py-0.5 rounded bg-muted">Enter</kbd> to send,{" "}
        <kbd className="px-1 py-0.5 rounded bg-muted">Shift+Enter</kbd> for new line
      </div>
    </div>
  );
}
