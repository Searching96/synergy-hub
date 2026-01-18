import { useState, useRef } from "react";
import { X, Lock, Eye, Share2, MoreHorizontal, Paperclip, GitBranch, LinkIcon, Trash2, Download, Plus, Pencil } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import {
  useTaskAttachments,
  useUploadAttachment,
  useDeleteAttachment,
  useTaskSubtasks,
  useCreateSubtask,
  useWatchTask,
  useUnwatchTask,
  useLinkTasks,
  useTask
} from "@/hooks/useTasks";
import { taskService } from "@/services/task.service";

interface IssueDetailPanelProps {
  taskId: number;
  issueKey: string;
  issueType: string;
  title: string;
  status: string;
  description?: string;
  projectId: number;
  onClose: () => void;
  className?: string;
}

export default function IssueDetailPanel({
  taskId,
  issueKey,
  issueType,
  title,
  status,
  description,
  projectId,
  onClose,
  className,
}: IssueDetailPanelProps) {
  const [isSubtaskDialogOpen, setIsSubtaskDialogOpen] = useState(false);
  const [subtaskTitle, setSubtaskTitle] = useState("");
  const fileInputRef = useRef<HTMLInputElement>(null);

  const { data: attachmentsData } = useTaskAttachments(taskId);
  const { data: subtasksResponse } = useTaskSubtasks(taskId);
  const { data: taskResponse } = useTask(taskId);

  const uploadAttachment = useUploadAttachment();
  const deleteAttachment = useDeleteAttachment();
  const createSubtask = useCreateSubtask();
  const watchTask = useWatchTask();
  const unwatchTask = useUnwatchTask();
  const linkTasks = useLinkTasks();

  const attachments = attachmentsData || [];
  const subtasks = subtasksResponse?.data || [];
  const task = taskResponse?.data;
  const isWatching = task?.watching || false;
  const watchersCount = task?.watchersCount || 0;
  const linkedTasksList = task?.linkedTasks || [];

  // Prefer fetched task data over props, fall back to props (which are likely from list view)
  const displayTitle = task?.title ?? title;
  const displayStatus = task?.status ?? status;
  const displayType = task?.type ?? issueType;
  const displayDescription = task?.description ?? description;


  const getTypeColor = (type: string) => {
    const colors: Record<string, string> = {
      EPIC: "bg-purple-600",
      STORY: "bg-blue-600",
      TASK: "bg-green-600",
      BUG: "bg-red-600",
      SUBTASK: "bg-gray-600",
    };
    return colors[type] || "bg-gray-600";
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 10 * 1024 * 1024) { // 10MB limit
      toast.error("File details is too large (max 10MB)");
      return;
    }

    try {
      await uploadAttachment.mutateAsync({ taskId, file });
    } catch (error) {
      console.error("Upload failed", error);
    } finally {
      // Reset input
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const handleCreateSubtask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!subtaskTitle.trim()) return;

    try {
      await createSubtask.mutateAsync({
        title: subtaskTitle,
        description: "",
        status: "TO_DO",
        priority: "MEDIUM",
        type: "SUBTASK",
        projectId: projectId,
        parentTaskId: taskId,
        storyPoints: null, // Backend validates @Min(1) so 0 fails. Null skips validation if not @NotNull.
        // reporterId is handled by backend from context usually, or passing current user
      });
      setSubtaskTitle("");
      setIsSubtaskDialogOpen(false);
    } catch (error) {
      console.error("Failed to create subtask", error);
    }
  };

  const handleToggleWatch = async () => {
    try {
      if (isWatching) {
        await unwatchTask.mutateAsync(taskId);
      } else {
        await watchTask.mutateAsync(taskId);
      }
    } catch (error) {
      console.error("Failed to toggle watch", error);
    }
  };

  const handleLinkTask = async () => {
    const linkedTaskIdStr = prompt("Enter Task ID to link:");
    if (!linkedTaskIdStr) return;

    const linkedTaskId = parseInt(linkedTaskIdStr);
    if (isNaN(linkedTaskId)) {
      toast.error("Invalid Task ID");
      return;
    }

    try {
      await linkTasks.mutateAsync({ taskId, linkedTaskId });
    } catch (error) {
      console.error("Failed to link task", error);
    }
  };

  return (
    <div className={cn("flex flex-col w-[420px] border-l border-gray-200 bg-white flex-shrink-0 overflow-hidden shadow-lg h-full", className)}>
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200 flex-shrink-0">
        <div className="flex items-center gap-2">
          <span className="text-xs font-bold text-gray-600 tracking-wide">
            {issueKey}
          </span>
        </div>
        <div className="flex items-center gap-1">
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8 text-gray-500"
            onClick={() => toast.info("Access control settings coming soon")}
          >
            <Lock className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className={`h-8 w-8 ${isWatching ? "text-blue-600 bg-blue-50" : "text-gray-500"}`}
            onClick={handleToggleWatch}
            disabled={watchTask.isPending || unwatchTask.isPending}
          >
            <div className="relative">
              <Eye className="h-4 w-4" />
              {watchersCount > 0 && (
                <span className="absolute -top-2 -right-2 bg-blue-600 text-white text-[8px] rounded-full w-3 h-3 flex items-center justify-center">
                  {watchersCount}
                </span>
              )}
            </div>
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8 text-gray-500"
            onClick={() => toast.info("Sharing feature coming soon")}
          >
            <Share2 className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8 text-gray-500"
          >
            <MoreHorizontal className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7 text-gray-500 hover:text-gray-900"
            onClick={() => toast.info("Edit task feature coming soon")}
          >
            <Pencil className="h-4 w-4" />
          </Button>
          <button
            onClick={onClose}
            className="p-1.5 hover:bg-gray-100 rounded transition-colors"
            aria-label="Close detail panel"
          >
            <X className="h-4 w-4 text-gray-500" />
          </button>
        </div>
      </div>

      {/* Scrollable Content */}
      <div className="flex-1 min-h-0 overflow-y-auto">
        {/* Title Section */}
        <div className="px-4 py-4 border-b border-gray-200">
          <div className="flex items-start gap-3">
            <div className={`${getTypeColor(displayType)} text-white rounded w-6 h-6 flex items-center justify-center flex-shrink-0 text-xs font-bold`}>
              {displayType.charAt(0)}
            </div>
            <div className="flex-1 min-w-0">
              <h2 className="text-lg font-semibold text-gray-900 leading-tight">
                {displayTitle}
              </h2>
            </div>
          </div>
        </div>

        {/* Action Bar */}
        <div className="px-4 py-3 border-b border-gray-200 flex gap-2">
          <Button
            variant="outline"
            size="sm"
            className="flex items-center gap-1 text-xs h-8"
            onClick={() => fileInputRef.current?.click()}
            disabled={uploadAttachment.isPending}
          >
            <Paperclip className="h-3.5 w-3.5" />
            {uploadAttachment.isPending ? "Uploading..." : "Attach"}
          </Button>
          <input
            type="file"
            ref={fileInputRef}
            className="hidden"
            onChange={handleFileUpload}
          />

          <Button
            variant="outline"
            size="sm"
            className="flex items-center gap-1 text-xs h-8"
            onClick={() => setIsSubtaskDialogOpen(true)}
          >
            <GitBranch className="h-3.5 w-3.5" />
            Subtask
          </Button>
          <Button
            variant="outline"
            size="sm"
            className="flex items-center gap-1 text-xs h-8"
            onClick={handleLinkTask}
            disabled={linkTasks.isPending}
          >
            <LinkIcon className="h-3.5 w-3.5" />
            {linkTasks.isPending ? "Linking..." : "Link"}
          </Button>
        </div>

        {/* Status */}
        <div className="px-4 py-3 border-b border-gray-200">
          <div className="inline-flex items-center gap-2 px-2 py-1 rounded-full bg-blue-100 text-blue-700 text-xs font-medium">
            {displayStatus}
          </div>
        </div>

        {/* Description */}
        <div className="px-4 py-4 border-b border-gray-200">
          <h3 className="text-xs font-bold text-gray-700 mb-2 uppercase tracking-wide">
            Description
          </h3>
          {displayDescription ? (
            <p className="text-sm text-gray-700 leading-relaxed whitespace-pre-wrap">{displayDescription}</p>
          ) : (
            <p className="text-sm text-gray-400 italic">Add a description...</p>
          )}
        </div>

        {/* Attachments Section */}
        <div className="px-4 py-4 border-b border-gray-200">
          <h3 className="text-xs font-bold text-gray-700 mb-2 uppercase tracking-wide flex items-center justify-between">
            Attachments
            {attachments.length > 0 && <span className="bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded-full text-[10px]">{attachments.length}</span>}
          </h3>

          <div className="space-y-2">
            {attachments.length === 0 ? (
              <p className="text-sm text-gray-500 italic">No attachments yet.</p>
            ) : (
              attachments.map((att: any) => (
                <div key={att.id} className="flex items-center justify-between p-2 rounded border border-gray-100 hover:bg-gray-50 group">
                  <div className="flex items-center gap-2 min-w-0">
                    <Paperclip className="h-3.5 w-3.5 text-gray-400 flex-shrink-0" />
                    <Button
                      variant="link"
                      className="text-sm text-blue-600 hover:underline truncate p-0 h-auto"
                      onClick={() => taskService.downloadAttachment(taskId, att)}
                    >
                      {att.fileName}
                    </Button>
                  </div>
                  <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-6 w-6 text-gray-500 hover:text-red-500"
                      onClick={() => deleteAttachment.mutate({ attachmentId: att.id, taskId })}
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </Button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>



        {/* Linked Issues Section */}
        <div className="px-4 py-4 border-b border-gray-200">
          <h3 className="text-xs font-bold text-gray-700 mb-2 uppercase tracking-wide flex items-center justify-between">
            Linked Issues
            <Button variant="ghost" size="sm" className="h-5 px-1 text-xs" onClick={handleLinkTask}>
              <Plus className="h-3 w-3 mr-1" /> Link
            </Button>
          </h3>

          <div className="space-y-1">
            {linkedTasksList.length === 0 ? (
              <p className="text-sm text-gray-500 italic">No linked issues yet.</p>
            ) : (
              linkedTasksList.map((lt: any) => (
                <div key={lt.id} className="flex items-center gap-2 p-2 rounded hover:bg-gray-50 cursor-pointer border border-transparent hover:border-gray-100">
                  <div className={`${getTypeColor(lt.type)} text-white rounded w-4 h-4 flex items-center justify-center flex-shrink-0 text-[10px] font-bold`}>
                    {lt.type.charAt(0)}
                  </div>
                  <span className="text-xs text-gray-500 font-mono">{issueKey.split('-')[0]}-{lt.id}</span>
                  <span className="text-sm text-gray-700 truncate flex-1">
                    {lt.title}
                  </span>
                  <span className="text-[10px] px-1.5 py-0.5 rounded bg-gray-100 text-gray-600">
                    {lt.status}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Subtasks Section */}
        <div className="px-4 py-4 border-b border-gray-200">
          <h3 className="text-xs font-bold text-gray-700 mb-2 uppercase tracking-wide flex items-center justify-between">
            Subtasks
            <Button variant="ghost" size="sm" className="h-5 px-1 text-xs" onClick={() => setIsSubtaskDialogOpen(true)}>
              <Plus className="h-3 w-3 mr-1" /> Add
            </Button>
          </h3>

          <div className="space-y-1">
            {subtasks.length === 0 ? (
              <p className="text-sm text-gray-500 italic">No subtasks yet.</p>
            ) : (
              subtasks.map((st: any) => (
                <div key={st.id} className="flex items-center gap-2 p-2 rounded hover:bg-gray-50 cursor-pointer border border-transparent hover:border-gray-100">
                  <div className="bg-gray-600 text-white rounded w-4 h-4 flex items-center justify-center flex-shrink-0 text-[9px] font-bold">
                    S
                  </div>
                  <span className="text-xs text-gray-500 font-mono">{issueKey.split('-')[0]}-{st.id}</span>
                  <span className={`text-sm ${st.status === 'DONE' ? 'line-through text-gray-400' : 'text-gray-700'} truncate flex-1`}>
                    {st.title}
                  </span>
                  <span className="text-[10px] px-1.5 py-0.5 rounded bg-gray-100 text-gray-600">
                    {st.status}
                  </span>
                </div>
              ))
            )}
            <Button
              variant="ghost"
              size="sm"
              className="w-full justify-start text-xs text-muted-foreground mt-2"
              onClick={() => setIsSubtaskDialogOpen(true)}
            >
              <Plus className="h-3.5 w-3.5 mr-2" />
              Create subtask
            </Button>
          </div>
        </div>

        {/* Confluence Pages */}
        <div className="px-4 py-4 border-b border-gray-200">
          <h3 className="text-xs font-bold text-gray-700 mb-2 uppercase tracking-wide">
            Confluence Pages
          </h3>
          <Button
            variant="outline"
            size="sm"
            className="text-xs h-8 w-full justify-start"
            onClick={() => toast.info("Confluence integration coming soon")}
          >
            <LinkIcon className="h-3.5 w-3.5 mr-2" />
            Link Confluence pages
          </Button>
        </div>

        {/* Pinned Fields */}
        <div className="px-4 py-4 mb-20">
          <h3 className="text-xs font-bold text-gray-700 mb-3 uppercase tracking-wide">
            Details
          </h3>
          <div className="space-y-3">
            <div className="flex justify-between items-center">
              <span className="text-xs text-gray-600">Type</span>
              <span className="text-xs font-medium text-gray-900">{displayType}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-xs text-gray-600">Status</span>
              <span className="text-xs font-medium text-gray-900">{displayStatus}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-xs text-gray-600">Task ID</span>
              <span className="text-xs font-medium text-gray-900">{taskId}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-xs text-gray-600">Priority</span>
              <span className="text-xs font-medium text-gray-900">{task?.priority || "None"}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-xs text-gray-600">Story Points</span>
              <span className="text-xs font-medium text-gray-900">{task?.storyPoints || "-"}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-xs text-gray-600">Assignee</span>
              <span className="text-xs font-medium text-gray-900">
                {task?.assigneeName || task?.assignee?.name || "Unassigned"}
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-xs text-gray-600">Reporter</span>
              <span className="text-xs font-medium text-gray-900">
                {task?.reporterName || task?.reporter?.name || "Unknown"}
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-xs text-gray-600">Due Date</span>
              <span className="text-xs font-medium text-gray-900">
                {task?.dueDate ? new Date(task.dueDate).toLocaleDateString() : "None"}
              </span>
            </div>
          </div>
        </div>
      </div >

      {/* Subtask Dialog */}
      < Dialog open={isSubtaskDialogOpen} onOpenChange={setIsSubtaskDialogOpen} >
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>Create Subtask</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleCreateSubtask}>
            <div className="grid gap-4 py-4">
              <div className="grid gap-2">
                <Label htmlFor="subtask-title">Title</Label>
                <Input
                  id="subtask-title"
                  value={subtaskTitle}
                  onChange={(e) => setSubtaskTitle(e.target.value)}
                  placeholder="What needs to be done?"
                  autoFocus
                />
              </div>
            </div>
            <DialogFooter>
              <Button type="button" variant="ghost" onClick={() => setIsSubtaskDialogOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={createSubtask.isPending || !subtaskTitle.trim()}>
                {createSubtask.isPending ? "Creating..." : "Create"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog >
    </div >
  );
}
