/**
 * IssueCommentsSection Component
 * Displays comments list and provides comment input form
 */

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Separator } from "@/components/ui/separator";
import { MessageSquare, Send } from "lucide-react";
import { formatRelativeTime } from "@/lib/date";
import type { Comment } from "@/types/comment.types";

interface IssueCommentsSectionProps {
  taskId: number;
  comments: Comment[];
  isLoading?: boolean;
  isProjectArchived?: boolean;
  onAddComment: (text: string) => void;
}

function CommentCard({ comment }: { comment: Comment }) {
  const authorName = comment.author?.name || comment.userName || "Unknown";
  const authorInitial = authorName.charAt(0).toUpperCase();

  return (
    <div className="space-y-2">
      <div className="flex items-start gap-3">
        <Avatar className="h-8 w-8">
          <AvatarFallback className="bg-gradient-to-br from-blue-500 to-purple-500 text-white text-xs">
            {authorInitial}
          </AvatarFallback>
        </Avatar>
        <div className="flex-1 space-y-1">
          <div className="flex items-center gap-2">
            <span className="text-sm font-semibold">{authorName}</span>
            <span className="text-xs text-muted-foreground">
              {formatRelativeTime(comment.createdAt)}
            </span>
          </div>
          <div className="text-sm text-foreground whitespace-pre-wrap bg-muted rounded-lg p-3">
            {comment.content}
          </div>
        </div>
      </div>
    </div>
  );
}

export function IssueCommentsSection({
  taskId,
  comments,
  isLoading = false,
  isProjectArchived = false,
  onAddComment,
}: IssueCommentsSectionProps) {
  const [commentText, setCommentText] = useState("");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (commentText.trim()) {
      onAddComment(commentText);
      setCommentText("");
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <MessageSquare className="h-5 w-5 text-muted-foreground" />
        <h3 className="text-lg font-semibold">Comments</h3>
        <span className="text-sm text-muted-foreground">({comments.length})</span>
      </div>

      {/* Comments List */}
      <ScrollArea className="h-[300px] rounded-lg border p-4">
        {isLoading ? (
          <div className="flex items-center justify-center h-full text-muted-foreground">
            Loading comments...
          </div>
        ) : comments.length === 0 ? (
          <div className="flex items-center justify-center h-full text-muted-foreground">
            No comments yet. Be the first to comment!
          </div>
        ) : (
          <div className="space-y-4">
            {comments.map((comment, index) => (
              <div key={comment.id}>
                <CommentCard comment={comment} />
                {index < comments.length - 1 && <Separator className="mt-4" />}
              </div>
            ))}
          </div>
        )}
      </ScrollArea>

      {/* Add Comment Form */}
      {!isProjectArchived && (
        <form onSubmit={handleSubmit} className="space-y-3">
          <Textarea
            placeholder="Add a comment..."
            value={commentText}
            onChange={(e) => setCommentText(e.target.value)}
            className="min-h-[100px] resize-none"
            disabled={isProjectArchived}
          />
          <div className="flex justify-end">
            <Button
              type="submit"
              size="sm"
              disabled={!commentText.trim() || isProjectArchived}
            >
              <Send className="h-4 w-4 mr-2" />
              Add Comment
            </Button>
          </div>
        </form>
      )}

      {isProjectArchived && (
        <div className="text-sm text-muted-foreground text-center py-4 bg-muted rounded-lg">
          Comments are disabled for archived projects
        </div>
      )}
    </div>
  );
}
