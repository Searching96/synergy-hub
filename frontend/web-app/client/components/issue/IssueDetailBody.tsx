import { Separator } from "@/components/ui/separator";
import { Textarea } from "@/components/ui/textarea";
import { IssueHeaderEditor } from "@/components/issue/IssueHeaderEditor";
import { IssueMetadataPanel } from "@/components/issue/IssueMetadataPanel";
import { IssueMetadataPanelSkeleton } from "@/components/issue/IssueMetadataPanelSkeleton";
import { IssueCommentsSection } from "@/components/issue/IssueCommentsSection";
import { IssueHierarchySection } from "@/components/issue/IssueHierarchySection";
import { IssueAttachmentsSection } from "@/components/issue/IssueAttachmentsSection";
import type { Task, TaskStatus, TaskPriority } from "@/types/task.types";
import type { ProjectMember } from "@/types/project.types";
import type { Comment } from "@/types/comment.types";
import type { Attachment } from "@/types/attachment.types";
import { useState, useEffect } from "react";
import { taskService } from "@/services/task.service";
import { toast } from "sonner"; // Assuming sonner is used for toasts based on other files

interface IssueDetailBodyProps {
  task: Task;
  comments: Comment[];
  members: ProjectMember[] | undefined;
  isLoading: boolean;
  isProjectArchived: boolean;
  isEditingTitle: boolean;
  editedTitle: string;
  editedDescription: string;
  canSaveTitle: boolean;
  canSaveDescription: boolean;
  onEditTitle: () => void;
  onSaveTitle: () => Promise<void>;
  onCancelEditTitle: () => void;
  onTitleChange: (title: string) => void;
  onDescriptionChange: (description: string) => void;
  onSaveDescription: () => Promise<void>;
  onStatusChange: (status: TaskStatus) => void;
  onPriorityChange: (priority: TaskPriority) => void;
  onAssigneeChange: (assigneeId: number | null) => void;
  onAddComment: (text: string) => void;
}

export function IssueDetailBody({
  task,
  comments,
  members,
  isLoading,
  isProjectArchived,
  isEditingTitle,
  editedTitle,
  editedDescription,
  canSaveTitle,
  canSaveDescription,
  onEditTitle,
  onSaveTitle,
  onCancelEditTitle,
  onTitleChange,
  onDescriptionChange,
  onSaveDescription,
  onStatusChange,
  onPriorityChange,
  onAssigneeChange,
  onAddComment,
}: IssueDetailBodyProps) {
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [subtasks, setSubtasks] = useState<Task[]>([]);
  // We can also store epic/parent info if needed, but task object typically has basic info

  useEffect(() => {
    if (task?.id) {
      loadAttachments();
      loadSubtasks();
    }
  }, [task?.id]);

  const loadAttachments = async () => {
    try {
      const response = await taskService.getTaskAttachments(task.id);
      setAttachments(response || []);
    } catch (error) {
      console.error("Failed to load attachments", error);
    }
  };

  const loadSubtasks = async () => {
    try {
      const response = await taskService.getTaskSubtasks(task.id);
      setSubtasks(response.data || []);
    } catch (error) {
      console.error("Failed to load subtasks", error);
    }
  };

  const handleIssueClick = (issueId: number) => {
    // Update URL to show the clicked issue
    const searchParams = new URLSearchParams(window.location.search);
    searchParams.set('selectedIssue', issueId.toString());
    window.history.pushState({}, '', `?${searchParams.toString()}`);
    window.dispatchEvent(new PopStateEvent('popstate'));
  };

  // Combine fetched data with task prop
  const taskWithData = {
    ...task,
    attachments: attachments,
    subtasks: subtasks
  };

  return (
    <div className="flex-1 overflow-y-auto">
      <div className="grid grid-cols-3 gap-6 p-6">
        {/* Main Content Column */}
        <div className="col-span-2 space-y-6">
          {/* Title */}
          <IssueHeaderEditor
            task={taskWithData}
            isEditing={isEditingTitle}
            isProjectArchived={isProjectArchived}
            onEdit={onEditTitle}
            onSave={onSaveTitle}
            onCancel={onCancelEditTitle}
          />

          <Separator />

          {/* Description */}
          <div>
            <h3 className="text-sm font-semibold mb-2">Description</h3>
            <Textarea
              value={editedDescription}
              onChange={(e) => onDescriptionChange(e.target.value)}
              onBlur={onSaveDescription}
              placeholder="Add a description..."
              className="min-h-[150px] resize-none transition-all duration-150 focus:ring-2 focus:ring-blue-400"
              disabled={isProjectArchived}
            />
          </div>

          {/* Issue Hierarchy Section */}
          <IssueHierarchySection
            task={taskWithData}
            onIssueClick={handleIssueClick}
          />

          {/* Attachments Section */}
          <IssueAttachmentsSection
            entityId={taskWithData.id}
            attachments={attachments}
            onUpload={async (entityId, file, onProgress) => {
              const response = await taskService.uploadAttachment(entityId, file, onProgress);
              // Refresh attachments
              loadAttachments();
              return response.data;
            }}
            onDelete={async (attachmentId) => {
              await taskService.deleteAttachment(task.id, attachmentId);
              loadAttachments();
            }}
            onDownload={(attachment) => taskService.downloadAttachment(task.id, attachment)}
            onBulkDownload={(attachmentIds) => taskService.bulkDownloadAttachments(task.id, attachmentIds)}
            isReadOnly={isProjectArchived}
          />

          <Separator />

          {/* Comments Section */}
          <IssueCommentsSection
            taskId={taskWithData.id}
            comments={comments}
            isProjectArchived={isProjectArchived}
            onAddComment={onAddComment}
          />
        </div>

        {/* Metadata Sidebar */}
        <div className="border-l pl-6">
          {isLoading ? (
            <IssueMetadataPanelSkeleton />
          ) : !members || members.length === 0 ? (
            <div className="space-y-4">
              <div className="text-sm text-muted-foreground">
                Unable to load project members
              </div>
            </div>
          ) : (
            <IssueMetadataPanel
              task={taskWithData}
              members={members}
              isProjectArchived={isProjectArchived}
              onStatusChange={onStatusChange}
              onPriorityChange={onPriorityChange}
              onAssigneeChange={onAssigneeChange}
            />
          )}
        </div>
      </div>
    </div>
  );
}
