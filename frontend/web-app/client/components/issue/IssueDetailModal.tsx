import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useIssueDetail } from "@/hooks/useIssueDetail";
import { useIssueDetailKeyboardShortcuts } from "@/hooks/useIssueDetailKeyboardShortcuts";
import { useIssueDetailDialogs } from "@/hooks/useIssueDetailDialogs";
import { useDebouncedCallback } from "@/hooks/useDebounce";
import { IssueDetailSkeleton } from "@/components/issue/IssueDetailSkeleton";
import { IssueDetailHeader } from "@/components/issue/IssueDetailHeader";
import { IssueDetailBody } from "@/components/issue/IssueDetailBody";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import { cn } from "@/lib/utils";
import {
  Dialog,
  DialogContent,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
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
import { AlertCircle } from "lucide-react";
import { useIssueEdit } from "@/hooks/useIssueEdit";
import type { TaskStatus, TaskPriority } from "@/types/task.types";
import { toast } from "sonner";

/**
 * IssueDetailModal - Refactored Orchestrator
 * Delegates to specialized sub-components:
 * - IssueDetailHeader: Title, breadcrumb, action buttons
 * - IssueDetailBody: Description, metadata, comments
 * - Dialogs: Archive and delete confirmations
 */
export default function IssueDetailModal() {
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedIssue = searchParams.get("selectedIssue");

  const taskId = selectedIssue ? parseInt(selectedIssue) : null;

  // Dialog state management
  const dialogs = useIssueDetailDialogs();

  const handleClose = () => {
    setSearchParams(prev => {
      prev.delete("selectedIssue");
      return prev;
    });
  };

  const {
    task,
    comments,
    members,
    isLoading,
    isProjectArchived,
    updateTask,
    updateTaskAsync,
    updateAssignee,
    addComment,
    deleteTask,
    archiveTask,
    unarchiveTask,
    taskQuery,
  } = useIssueDetail({ taskId, onClose: handleClose });

  // Show loading skeleton while fetching
  if (!selectedIssue) return null;

  if (isLoading) {
    return <IssueDetailSkeleton />;
  }

  // Show error state
  if (!task) {
    const error = taskQuery?.error as any;
    const isNotFound = error?.response?.status === 404;
    const isForbidden = error?.response?.status === 403;

    return (
      <Dialog open={!!selectedIssue} onOpenChange={handleClose}>
        <DialogContent className="max-w-md">
          <DialogTitle className="sr-only">Error Loading Issue</DialogTitle>
          <DialogDescription className="sr-only">
            An error occurred while trying to load the issue details.
          </DialogDescription>
          <div className="flex flex-col items-center justify-center py-8">
            <AlertCircle className={cn(
              "h-12 w-12 mb-4",
              isForbidden ? "text-orange-600" : "text-destructive"
            )} />
            <h2 className={cn(
              "text-lg font-semibold mb-2",
              isForbidden ? "text-orange-600" : "text-destructive"
            )}>
              {isNotFound ? "Issue Not Found" : isForbidden ? "Access Denied" : "Failed to Load Issue"}
            </h2>
            <p className="text-sm text-muted-foreground text-center">
              {isNotFound
                ? "This issue may have been deleted or the ID is incorrect."
                : isForbidden
                  ? "You don't have permission to view this issue."
                  : "The issue could not be loaded. Please try again."
              }
            </p>
            <Button onClick={handleClose} className="mt-4">
              Close
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    );
  }

  // Edit state hook for title and description
  const {
    isEditingTitle,
    setIsEditingTitle,
    editedTitle,
    setEditedTitle,
    editedDescription,
    setEditedDescription,
    canSaveTitle,
    canSaveDescription,
  } = useIssueEdit(task);

  // Event Handlers
  const handleSaveTitle = async () => {
    if (canSaveTitle) {
      await updateTaskAsync({
        taskId: taskId!,
        data: { title: editedTitle },
      });
      setIsEditingTitle(false);
    }
  };

  const handleStatusToggle = () => {
    const newStatus: TaskStatus = task?.status === "DONE" ? "TO_DO" : "DONE";
    handleStatusChange(newStatus);
  };

  // Keyboard shortcuts
  useIssueDetailKeyboardShortcuts({
    isOpen: !!selectedIssue,
    isEditingTitle,
    canSaveTitle,
    taskStatus: task?.status,
    onClose: handleClose,
    onSaveTitle: handleSaveTitle,
    onStatusToggle: handleStatusToggle,
  });

  const handleSaveDescription = async () => {
    if (canSaveDescription) {
      await updateTaskAsync({
        taskId: taskId!,
        data: { description: editedDescription || null },
      });
    }
  };

  // Debounced auto-save for description (saves 800ms after user stops typing)
  const debouncedSaveDescription = useDebouncedCallback(handleSaveDescription, 800);

  const handleStatusChange = (status: TaskStatus) => {
    const previousStatus = task?.status;

    // Optimistic update
    updateTask({ taskId: taskId!, data: { status } });

    // Show toast with undo
    if (previousStatus) {
      toast.success(`Status changed to ${status.replace(/_/g, " ")}`, {
        description: "Click Undo to revert this change",
        action: {
          label: "Undo",
          onClick: () =>
            updateTask({ taskId: taskId!, data: { status: previousStatus } }),
        },
        duration: 5000,
      });
    }
  };

  const handlePriorityChange = (priority: TaskPriority) => {
    const previousPriority = task?.priority;

    // Optimistic update
    updateTask({ taskId: taskId!, data: { priority } });

    // Show toast with undo
    if (previousPriority) {
      toast.success(`Priority changed to ${priority}`, {
        description: "Click Undo to revert this change",
        action: {
          label: "Undo",
          onClick: () =>
            updateTask({ taskId: taskId!, data: { priority: previousPriority } }),
        },
        duration: 5000,
      });
    }
  };

  const handleAssigneeChange = (assigneeId: number | null) => {
    updateAssignee({ taskId: taskId!, assigneeId });
  };

  const handleAddComment = (text: string) => {
    addComment({ taskId: taskId!, content: text });
  };

  return (
    <ErrorBoundary fallback={(error, retry) => (
      <Dialog open={!!selectedIssue} onOpenChange={handleClose}>
        <DialogContent className="max-w-md">
          <DialogTitle className="sr-only">Unexpected Error</DialogTitle>
          <DialogDescription className="sr-only">
            An unexpected error occurred.
          </DialogDescription>
          <div className="flex flex-col items-center justify-center py-8">
            <AlertCircle className="h-12 w-12 text-destructive mb-4" />
            <h2 className="text-lg font-semibold text-destructive mb-2">
              Something Went Wrong
            </h2>
            <p className="text-sm text-muted-foreground text-center mb-4">
              An unexpected error occurred while loading this issue.
            </p>
            <Button onClick={handleClose}>Close</Button>
          </div>
        </DialogContent>
      </Dialog>
    )}>
      <Dialog open={!!selectedIssue} onOpenChange={(open) => !open && handleClose()}>
        <DialogContent className="max-w-6xl max-h-[90vh] p-0 gap-0 flex flex-col" hideClose>
          <DialogTitle className="sr-only">Issue Details</DialogTitle>
          <DialogDescription className="sr-only">
            View and edit details for this issue.
          </DialogDescription>
          {/* Header Component */}
          <IssueDetailHeader
            task={task}
            isProjectArchived={isProjectArchived}
            isEditingTitle={isEditingTitle}
            editedTitle={editedTitle}
            canSaveTitle={canSaveTitle}
            onEditTitle={() => setIsEditingTitle(true)}
            onSaveTitle={handleSaveTitle}
            onCancelEditTitle={() => {
              setEditedTitle(task.title);
              setIsEditingTitle(false);
            }}
            onTitleChange={setEditedTitle}
            onArchive={dialogs.openArchiveDialog}
            onUnarchive={() => unarchiveTask(taskId!)}
            onDelete={dialogs.openDeleteDialog}
            onClose={handleClose}
          />

          {/* Body Component */}
          <IssueDetailBody
            task={task}
            comments={comments}
            members={members}
            isLoading={isLoading}
            isProjectArchived={isProjectArchived}
            isEditingTitle={isEditingTitle}
            editedTitle={editedTitle}
            editedDescription={editedDescription}
            canSaveTitle={canSaveTitle}
            canSaveDescription={canSaveDescription}
            onEditTitle={() => setIsEditingTitle(true)}
            onSaveTitle={handleSaveTitle}
            onCancelEditTitle={() => {
              setEditedTitle(task.title);
              setIsEditingTitle(false);
            }}
            onTitleChange={setEditedTitle}
            onDescriptionChange={setEditedDescription}
            onSaveDescription={handleSaveDescription}
            onStatusChange={handleStatusChange}
            onPriorityChange={handlePriorityChange}
            onAssigneeChange={handleAssigneeChange}
            onAddComment={handleAddComment}
          />
        </DialogContent>
      </Dialog>

      {/* Archive Confirmation Dialog */}
      <AlertDialog open={dialogs.archiveDialogOpen} onOpenChange={dialogs.setArchiveDialogOpen}>
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
                archiveTask(taskId!);
                dialogs.closeArchiveDialog();
              }}
              className="bg-orange-600 hover:bg-orange-700"
            >
              Archive Issue
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={dialogs.deleteDialogOpen} onOpenChange={dialogs.setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Issue Permanently?</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to permanently delete this issue? This action cannot be undone. All comments and attachments will be permanently removed.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                deleteTask(taskId!);
                dialogs.closeDeleteDialog();
              }}
              className="bg-red-600 hover:bg-red-700"
            >
              Delete Permanently
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </ErrorBoundary>
  );
}
