import { Separator } from "@/components/ui/separator";
import { Textarea } from "@/components/ui/textarea";
import { IssueHeaderEditor } from "@/components/issue/IssueHeaderEditor";
import { IssueMetadataPanel } from "@/components/issue/IssueMetadataPanel";
import { IssueMetadataPanelSkeleton } from "@/components/issue/IssueMetadataPanelSkeleton";
import { IssueCommentsSection } from "@/components/issue/IssueCommentsSection";
import type { Task, TaskStatus, TaskPriority } from "@/types/task.types";
import type { ProjectMember } from "@/types/project.types";
import type { Comment } from "@/types/comment.types";

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
  return (
    <div className="flex-1 overflow-y-auto">
      <div className="grid grid-cols-3 gap-6 p-6">
        {/* Main Content Column */}
        <div className="col-span-2 space-y-6">
          {/* Title */}
          <IssueHeaderEditor
            task={task}
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

          <Separator />

          {/* Comments Section */}
          <IssueCommentsSection
            taskId={task.id}
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
              task={task}
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
