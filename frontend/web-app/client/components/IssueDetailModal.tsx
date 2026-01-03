import { useState, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { useTask } from "@/hooks/useTasks";
import { useUpdateTask, useUpdateTaskAssignee } from "@/hooks/useUpdateTask";
import { useTaskComments, useAddComment } from "@/hooks/useComments";
import { useProjectById } from "@/hooks/useProject";
import { projectService } from "@/services/project.service";
import { taskService } from "@/services/task.service";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useToast } from "@/hooks/use-toast";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Separator } from "@/components/ui/separator";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";
import {
  Share2,
  MoreHorizontal,
  Calendar,
  User,
  AlertCircle,
  Tag,
  Clock,
  CheckCircle2,
  Trash2,
  Copy,
  Link,
  Archive,
  X,
} from "lucide-react";
import { cn } from "@/lib/utils";

const PRIORITY_COLORS = {
  LOW: "bg-gray-100 text-gray-700",
  MEDIUM: "bg-blue-100 text-blue-700",
  HIGH: "bg-orange-100 text-orange-700",
  CRITICAL: "bg-red-100 text-red-700",
};

const STATUS_COLORS = {
  TODO: "bg-gray-100 text-gray-700",
  IN_PROGRESS: "bg-blue-100 text-blue-700",
  IN_REVIEW: "bg-purple-100 text-purple-700",
  DONE: "bg-green-100 text-green-700",
  BLOCKED: "bg-red-100 text-red-700",
};

export default function IssueDetailModal() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const selectedIssue = searchParams.get("selectedIssue");
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const [isEditingTitle, setIsEditingTitle] = useState(false);
  const [editedTitle, setEditedTitle] = useState("");
  const [editedDescription, setEditedDescription] = useState("");
  const [commentText, setCommentText] = useState("");
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [archiveDialogOpen, setArchiveDialogOpen] = useState(false);

  const taskId = selectedIssue ? parseInt(selectedIssue) : null;
  const { data: taskResponse, isLoading: isLoadingTask } = useTask(taskId!);
  const { data: commentsResponse, isLoading: isLoadingComments } = useTaskComments(taskId);
  const { data: subtasksResponse, isLoading: isLoadingSubtasks } = useQuery({
    queryKey: ["subtasks", taskId],
    queryFn: () => taskService.getTaskSubtasks(taskId!),
    enabled: !!taskId,
  });
  const updateTask = useUpdateTask();
  const updateAssignee = useUpdateTaskAssignee();
  const addComment = useAddComment();

  const deleteTask = useMutation({
    mutationFn: (taskId: number) => taskService.deleteTask(taskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      queryClient.invalidateQueries({ queryKey: ["board"] });
      queryClient.invalidateQueries({ queryKey: ["backlog"] });
      toast({
        title: "Success",
        description: "Task deleted permanently",
      });
      handleClose();
    },
    onError: (error: any) => {
      toast({
        title: "Error",
        description: error?.response?.data?.message || "Failed to delete task",
        variant: "destructive",
      });
    },
  });

  const archiveTask = useMutation({
    mutationFn: (taskId: number) => taskService.archiveTask(taskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      queryClient.invalidateQueries({ queryKey: ["board"] });
      queryClient.invalidateQueries({ queryKey: ["backlog"] });
      toast({
        title: "Success",
        description: "Task archived successfully",
      });
      handleClose();
    },
    onError: (error: any) => {
      toast({
        title: "Error",
        description: error?.response?.data?.message || "Failed to archive task",
        variant: "destructive",
      });
    },
  });

  const unarchiveTask = useMutation({
    mutationFn: (taskId: number) => taskService.unarchiveTask(taskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      queryClient.invalidateQueries({ queryKey: ["board"] });
      queryClient.invalidateQueries({ queryKey: ["backlog"] });
      toast({
        title: "Success",
        description: "Task unarchived successfully",
      });
      handleClose();
    },
    onError: (error: any) => {
      toast({
        title: "Error",
        description: error?.response?.data?.message || "Failed to unarchive task",
        variant: "destructive",
      });
    },
  });

  const task = taskResponse?.data;
  const comments = commentsResponse?.data || [];
  const subtasks = subtasksResponse?.data || [];

  // Fetch project data to check if archived
  const { data: projectResponse } = useQuery({
    queryKey: ["project", task?.projectId],
    queryFn: () => projectService.getProjectById(task!.projectId.toString()),
    enabled: !!task?.projectId,
  });

  const project = projectResponse?.data;
  const isProjectArchived = project?.status === "ARCHIVED";

  // Fetch project members
  const { data: membersResponse } = useQuery({
    queryKey: ["project-members", task?.projectId],
    queryFn: () => projectService.getProjectMembers(task!.projectId),
    enabled: !!task?.projectId,
  });

  const members = membersResponse?.data || [];

  useEffect(() => {
    if (task) {
      setEditedTitle(task.title);
      setEditedDescription(task.description || "");
    }
  }, [task]);

  const handleClose = () => {
    searchParams.delete("selectedIssue");
    setSearchParams(searchParams);
  };

  const handleSaveTitle = async () => {
    if (editedTitle.trim() && editedTitle !== task?.title) {
      await updateTask.mutateAsync({
        taskId: taskId!,
        data: { title: editedTitle },
      });
    }
    setIsEditingTitle(false);
  };

  const handleSaveDescription = async () => {
    if (editedDescription !== task?.description) {
      await updateTask.mutateAsync({
        taskId: taskId!,
        data: { description: editedDescription || null },
      });
    }
  };

  const handleStatusChange = async (newStatus: string) => {
    await updateTask.mutateAsync({
      taskId: taskId!,
      data: { status: newStatus },
    });
  };

  const handlePriorityChange = async (newPriority: string) => {
    await updateTask.mutateAsync({
      taskId: taskId!,
      data: { priority: newPriority },
    });
  };

  const handleAssigneeChange = async (assigneeId: string) => {
    await updateAssignee.mutateAsync({
      taskId: taskId!,
      assigneeId: assigneeId === "unassigned" ? null : parseInt(assigneeId),
    });
  };

  const handleAddComment = async () => {
    if (commentText.trim()) {
      await addComment.mutateAsync({
        taskId: taskId!,
        content: commentText,
      });
      setCommentText("");
    }
  };

  const handleShare = () => {
    const url = window.location.href;
    navigator.clipboard.writeText(url).then(() => {
      toast({
        title: "Link copied!",
        description: "Issue link copied to clipboard",
      });
    }).catch(() => {
      toast({
        title: "Failed to copy",
        description: "Could not copy link to clipboard",
        variant: "destructive",
      });
    });
  };

  const handleDelete = () => {
    if (taskId) {
      deleteTask.mutate(taskId);
    }
  };

  const handleCopyLink = () => {
    const url = `${window.location.origin}/projects/${task?.projectId}/board?selectedIssue=${taskId}`;
    navigator.clipboard.writeText(url).then(() => {
      toast({
        title: "Link copied!",
        description: "Direct link to this issue copied to clipboard",
      });
    });
  };

  const getInitials = (name: string) => {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2);
  };

  if (!selectedIssue) return null;

  return (
    <Dialog open={!!selectedIssue} onOpenChange={(open) => !open && handleClose()}>
      <DialogContent className="max-w-6xl max-h-[90vh] p-0 gap-0 flex flex-col" hideClose>
        {isLoadingTask ? (
          <div className="flex items-center justify-center h-96">
            <div className="text-muted-foreground">Loading...</div>
          </div>
        ) : !task ? (
          <div className="flex items-center justify-center h-96">
            <div className="text-muted-foreground">Issue not found</div>
          </div>
        ) : (
          <>
            {/* Header */}
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
                    <Badge variant="outline" className="bg-orange-50 text-orange-700 border-orange-300">
                      <Archive className="h-3 w-3 mr-1" />
                      Archived
                    </Badge>
                  )}
                  <Button variant="ghost" size="icon" onClick={handleShare}>
                    <Share2 className="h-4 w-4" />
                  </Button>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon">
                        <MoreHorizontal className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" className="w-48">
                      <DropdownMenuLabel>Actions</DropdownMenuLabel>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem onClick={handleCopyLink}>
                        <Link className="h-4 w-4 mr-2" />
                        Copy Link
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => handleCopyLink()}>
                        <Copy className="h-4 w-4 mr-2" />
                        Duplicate Issue
                      </DropdownMenuItem>
                      {task?.archived ? (
                        <DropdownMenuItem
                          onClick={() => unarchiveTask.mutate(taskId!)}
                        >
                          <Archive className="h-4 w-4 mr-2" />
                          Unarchive
                        </DropdownMenuItem>
                      ) : (
                        <DropdownMenuItem
                          onClick={() => setArchiveDialogOpen(true)}
                        >
                          <Archive className="h-4 w-4 mr-2" />
                          Archive
                        </DropdownMenuItem>
                      )}
                      <DropdownMenuSeparator />
                      <DropdownMenuItem
                        className="text-red-600 focus:text-red-600"
                        onClick={() => setDeleteDialogOpen(true)}
                      >
                        <Trash2 className="h-4 w-4 mr-2" />
                        Delete Permanently
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                  <Button variant="ghost" size="icon" onClick={handleClose}>
                    <X className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </DialogHeader>

            {/* Main Content */}
            <div className="flex flex-1 overflow-hidden">
              {/* Left Column - Main Content */}
              <div className="flex-1 overflow-y-auto px-6 py-6">
                {/* Title */}
                <div className="mb-6">
                  {isEditingTitle ? (
                    <Input
                      value={editedTitle}
                      onChange={(e) => setEditedTitle(e.target.value)}
                      onBlur={handleSaveTitle}
                      onKeyDown={(e) => {
                        if (e.key === "Enter") handleSaveTitle();
                        if (e.key === "Escape") {
                          setEditedTitle(task.title);
                          setIsEditingTitle(false);
                        }
                      }}
                      className="text-2xl font-bold border-0 px-0 focus-visible:ring-0"
                      autoFocus
                    />
                  ) : (
                    <h1
                      className={`text-2xl font-bold px-2 py-1 -mx-2 rounded ${
                        (task.archived || isProjectArchived) ? '' : 'cursor-pointer hover:bg-gray-50'
                      }`}
                      onClick={() => !task.archived && !isProjectArchived && setIsEditingTitle(true)}
                    >
                      {task.title}
                    </h1>
                  )}
                </div>

                {/* Description */}
                <div className="mb-6">
                  <Label className="text-sm font-semibold mb-2 block">Description</Label>
                  <Textarea
                    value={editedDescription}
                    onChange={(e) => setEditedDescription(e.target.value)}
                    onBlur={handleSaveDescription}
                    placeholder="Add a description..."
                    className="min-h-[120px] resize-none"
                    disabled={task.archived || isProjectArchived}
                  />
                </div>

                <Separator className="my-6" />

                {/* Subtasks Section */}
                {subtasks.length > 0 && (
                  <>
                    <div className="mb-6">
                      <h3 className="text-lg font-semibold mb-3">Subtasks ({subtasks.length})</h3>
                      <div className="space-y-2">
                        {subtasks.map((subtask: any) => (
                          <div
                            key={subtask.id}
                            className="flex items-center gap-3 p-3 border rounded-lg hover:bg-gray-50 cursor-pointer"
                            onClick={() => {
                              searchParams.set("selectedIssue", subtask.id.toString());
                              setSearchParams(searchParams);
                            }}
                          >
                            <CheckCircle2
                              className={cn(
                                "h-4 w-4 flex-shrink-0",
                                subtask.status === "DONE" ? "text-green-600" : "text-gray-400"
                              )}
                            />
                            <div className="flex-1 min-w-0">
                              <p className={cn(
                                "text-sm",
                                subtask.status === "DONE" && "line-through text-muted-foreground"
                              )}>
                                {subtask.title}
                              </p>
                            </div>
                            <Badge
                              variant="secondary"
                              className={cn(
                                "text-xs",
                                STATUS_COLORS[subtask.status as keyof typeof STATUS_COLORS] || "bg-gray-100"
                              )}
                            >
                              {subtask.status}
                            </Badge>
                          </div>
                        ))}
                      </div>
                    </div>
                    <Separator className="my-6" />
                  </>
                )}

                {/* Comments Section */}
                <div>
                  <h3 className="text-lg font-semibold mb-4">
                    Comments ({task.commentCount || comments.length})
                  </h3>

                  {/* Add Comment */}
                  {!task.archived && !isProjectArchived && (
                    <div className="mb-6">
                      <Textarea
                        value={commentText}
                        onChange={(e) => setCommentText(e.target.value)}
                        placeholder="Add a comment..."
                        className="mb-2"
                      />
                      <Button
                        onClick={handleAddComment}
                        disabled={!commentText.trim() || addComment.isPending}
                        size="sm"
                      >
                        Add Comment
                      </Button>
                    </div>
                  )}

                  {/* Comments List */}
                  <ScrollArea className="h-[400px]">
                    <div className="space-y-4">
                      {isLoadingComments ? (
                        <div className="text-sm text-muted-foreground">Loading comments...</div>
                      ) : comments.length === 0 ? (
                        <div className="text-sm text-muted-foreground">
                          No comments yet. Be the first to comment!
                        </div>
                      ) : (
                        comments
                          .filter((comment: any) => comment && (comment.userName || comment.author))
                          .map((comment: any) => (
                            <div key={comment.id} className="flex gap-3">
                              <Avatar className="h-8 w-8 mt-1">
                                <AvatarFallback className="text-xs">
                                  {getInitials(comment.userName || comment.author?.name || "U")}
                                </AvatarFallback>
                              </Avatar>
                              <div className="flex-1">
                                <div className="flex items-center gap-2 mb-1">
                                  <span className="text-sm font-medium">{comment.userName || comment.author?.name || "Unknown"}</span>
                                  <span className="text-xs text-muted-foreground">
                                    {new Date(comment.createdAt).toLocaleString()}
                                  </span>
                                  {comment.edited && (
                                    <Badge variant="outline" className="text-xs">
                                      Edited
                                    </Badge>
                                  )}
                                </div>
                                <p className="text-sm text-muted-foreground whitespace-pre-wrap">
                                  {comment.content}
                              </p>
                            </div>
                          </div>
                        ))
                      )}
                    </div>
                  </ScrollArea>
                </div>
              </div>

              {/* Right Column - Metadata */}
              <div className="w-80 border-l bg-gray-50/50 overflow-y-auto">
                <div className="space-y-6 px-6 py-6">
                  {/* Status */}
                  <div>
                    <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
                      <CheckCircle2 className="h-4 w-4" />
                      Status
                    </Label>
                    <Select value={task.status} onValueChange={handleStatusChange} disabled={task.archived || isProjectArchived}>
                      <SelectTrigger>
                        <SelectValue>
                          <Badge className={cn(STATUS_COLORS[task.status as keyof typeof STATUS_COLORS])}>
                            {task.status.replace("_", " ")}
                          </Badge>
                        </SelectValue>
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="TODO">To Do</SelectItem>
                        <SelectItem value="IN_PROGRESS">In Progress</SelectItem>
                        <SelectItem value="IN_REVIEW">In Review</SelectItem>
                        <SelectItem value="DONE">Done</SelectItem>
                        <SelectItem value="BLOCKED">Blocked</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  {/* Assignee */}
                  <div>
                    <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
                      <User className="h-4 w-4" />
                      Assignee
                    </Label>
                    <Select
                      value={task.assigneeId?.toString() || "unassigned"}
                      onValueChange={handleAssigneeChange}
                      disabled={task.archived || isProjectArchived}
                    >
                      <SelectTrigger>
                        <SelectValue>
                          {task.assigneeId && task.assigneeName ? (
                            <div className="flex items-center gap-2">
                              <Avatar className="h-6 w-6">
                                <AvatarFallback className="text-xs">
                                  {getInitials(task.assigneeName)}
                                </AvatarFallback>
                              </Avatar>
                              <span>{task.assigneeName}</span>
                            </div>
                          ) : (
                            "Unassigned"
                          )}
                        </SelectValue>
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="unassigned">Unassigned</SelectItem>
                        {members
                          .filter((member: any) => member?.user?.id && member?.user?.name)
                          .map((member: any) => (
                            <SelectItem key={member.user.id} value={member.user.id.toString()}>
                              <div className="flex items-center gap-2">
                                <Avatar className="h-6 w-6">
                                  <AvatarFallback className="text-xs">
                                    {getInitials(member.user.name)}
                                  </AvatarFallback>
                                </Avatar>
                                <span>{member.user.name}</span>
                              </div>
                            </SelectItem>
                          ))}
                      </SelectContent>
                    </Select>
                  </div>

                  {/* Reporter */}
                  {(task.reporterName || task.createdBy?.name) && (
                    <div>
                      <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
                        <User className="h-4 w-4" />
                        Reporter
                      </Label>
                      <div className="flex items-center gap-2 p-2 bg-white rounded-md border">
                        <Avatar className="h-6 w-6">
                          <AvatarFallback className="text-xs">
                            {getInitials(task.reporterName || task.createdBy?.name || "U")}
                          </AvatarFallback>
                        </Avatar>
                        <span className="text-sm">{task.reporterName || task.createdBy?.name || "Unknown"}</span>
                      </div>
                    </div>
                  )}

                  {/* Priority */}
                  <div>
                    <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
                      <AlertCircle className="h-4 w-4" />
                      Priority
                    </Label>
                    <Select value={task.priority} onValueChange={handlePriorityChange} disabled={task.archived || isProjectArchived}>
                      <SelectTrigger>
                        <SelectValue>
                          <Badge className={cn(PRIORITY_COLORS[task.priority as keyof typeof PRIORITY_COLORS])}>
                            {task.priority}
                          </Badge>
                        </SelectValue>
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="LOW">Low</SelectItem>
                        <SelectItem value="MEDIUM">Medium</SelectItem>
                        <SelectItem value="HIGH">High</SelectItem>
                        <SelectItem value="CRITICAL">Critical</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  {/* Labels */}
                  <div>
                    <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
                      <Tag className="h-4 w-4" />
                      Labels
                    </Label>
                    <div className="flex flex-wrap gap-1">
                      <Badge variant="outline">{task.type}</Badge>
                      {task.sprintName && <Badge variant="outline">{task.sprintName}</Badge>}
                    </div>
                  </div>

                  {/* Due Date */}
                  {task.dueDate && (
                    <div>
                      <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
                        <Calendar className="h-4 w-4" />
                        Due Date
                      </Label>
                      <div className="p-2 bg-white rounded-md border text-sm">
                        {new Date(task.dueDate).toLocaleDateString()}
                      </div>
                    </div>
                  )}

                  {/* Time Tracking */}
                  <div>
                    <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
                      <Clock className="h-4 w-4" />
                      Time Tracking
                    </Label>
                    <div className="space-y-1 text-sm">
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">Estimated:</span>
                        <span>{task.estimatedHours || 0}h</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">Actual:</span>
                        <span>{task.actualHours || 0}h</span>
                      </div>
                    </div>
                  </div>

                  <Separator />

                  {/* Metadata */}
                  <div className="space-y-2 text-xs text-muted-foreground">
                    <div>
                      Created {new Date(task.createdAt).toLocaleDateString()}
                    </div>
                    <div>
                      Updated {new Date(task.updatedAt).toLocaleDateString()}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </>
        )}
      </DialogContent>

      {/* Archive Confirmation Dialog */}
      <AlertDialog open={archiveDialogOpen} onOpenChange={setArchiveDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Archive Issue?</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to archive this issue? You can unarchive it later if needed.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                archiveTask.mutate(taskId!);
                setArchiveDialogOpen(false);
              }}
              className="bg-orange-600 hover:bg-orange-700"
            >
              Archive Issue
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Issue Permanently?</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to permanently delete this issue? This action cannot be undone.
              All comments and attachments will be permanently removed.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="bg-red-600 hover:bg-red-700"
            >
              Delete Permanently
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Dialog>
  );
}
