import { useSearchParams } from "react-router-dom";
import { useState } from "react";
import { KeyboardShortcutsHelp } from "@/components/issue/KeyboardShortcutsHelp";
import { IssueHeaderEditor } from "@/components/issue/IssueHeaderEditor";
import { IssueActionMenu } from "@/components/issue/IssueActionMenu";
import {
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Share2, Archive, X } from "lucide-react";
import type { Task } from "@/types/task.types";
import { toast } from "sonner";

interface IssueDetailHeaderProps {
  task: Task;
  isProjectArchived: boolean;
  isEditingTitle: boolean;
  editedTitle: string;
  canSaveTitle: boolean;
  onEditTitle: () => void;
  onSaveTitle: () => Promise<void>;
  onCancelEditTitle: () => void;
  onTitleChange: (title: string) => void;
  onArchive: () => void;
  onUnarchive: () => void;
  onDelete: () => void;
  onClose: () => void;
}

export function IssueDetailHeader({
  task,
  isProjectArchived,
  isEditingTitle,
  editedTitle,
  canSaveTitle,
  onEditTitle,
  onSaveTitle,
  onCancelEditTitle,
  onTitleChange,
  onArchive,
  onUnarchive,
  onDelete,
  onClose,
}: IssueDetailHeaderProps) {
  const [searchParams] = useSearchParams();

  const handleCopyLink = () => {
    const url = `${window.location.origin}/projects/${task?.projectId}/board?selectedIssue=${task.id}`;
    navigator.clipboard.writeText(url).then(() => {
      toast.success("Link copied!", {
        description: "Direct link to this issue copied to clipboard",
      });
    });
  };

  return (
    <DialogHeader className="px-6 py-4 border-b">
      <DialogTitle className="sr-only">Issue Details</DialogTitle>
      <DialogDescription className="sr-only">
        View and edit issue details, status, and comments
      </DialogDescription>
      <div className="flex items-center justify-between">
        <Breadcrumb>
          <BreadcrumbList>
            <BreadcrumbItem>
              <BreadcrumbLink href={`/projects/${task.projectId}`}>
                {task.projectName}
              </BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            <BreadcrumbItem>
              <BreadcrumbPage>TASK-{task.id}</BreadcrumbPage>
            </BreadcrumbItem>
          </BreadcrumbList>
        </Breadcrumb>

        <div className="flex items-center gap-2">
          {task.archived && (
            <Badge
              variant="outline"
              className="bg-orange-50 text-orange-700 border-orange-300"
            >
              <Archive className="h-3 w-3 mr-1" />
              Archived
            </Badge>
          )}
          <KeyboardShortcutsHelp />
          <Button
            variant="ghost"
            size="icon"
            onClick={handleCopyLink}
            title="Copy link to this issue"
          >
            <Share2 className="h-4 w-4" />
          </Button>
          <IssueActionMenu
            taskId={task.id}
            isArchived={task.archived || false}
            isProjectArchived={isProjectArchived}
            onArchive={onArchive}
            onUnarchive={onUnarchive}
            onDelete={onDelete}
            onCopyLink={handleCopyLink}
          />
          <Button
            variant="ghost"
            size="icon"
            onClick={onClose}
            title="Close (Esc)"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </DialogHeader>
  );
}
