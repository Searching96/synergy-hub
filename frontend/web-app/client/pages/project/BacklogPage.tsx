import { useState, useMemo } from "react";
import { useParams } from "react-router-dom";
import { DragDropContext, DropResult } from "@hello-pangea/dnd";
import { useProject } from "@/context/ProjectContext";
import { useAuth } from "@/context/AuthContext";
import { useBacklogTasks, useMoveTaskToSprint, useUpdateTaskInline } from "@/hooks/useBacklog";
import { useProjectSprints, useCompleteSprint } from "@/hooks/useSprints";
import { taskService } from "@/services/task.service";
import { useQueryClient } from "@tanstack/react-query";
import BacklogSection from "@/components/backlog/BacklogSection";
import IssueDetailModal from "@/components/IssueDetailModal";
import { Loader2, AlertCircle } from "lucide-react";
import { toast } from "sonner";
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

export default function BacklogPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { project } = useProject();
  const { user } = useAuth();
  const { data: tasks, isLoading, error } = useBacklogTasks(projectId);
  const { data: sprints } = useProjectSprints(projectId);
  const queryClient = useQueryClient();
  const moveTask = useMoveTaskToSprint(projectId);
  const updateTask = useUpdateTaskInline(projectId);
  const completeSprint = useCompleteSprint(projectId);
  const [completeDialogOpen, setCompleteDialogOpen] = useState(false);

  const activeSprint = useMemo(() => {
    return sprints?.find((s) => s.status === "ACTIVE");
  }, [sprints]);

  const { sprintTasks, backlogTasks } = useMemo(() => {
    if (!tasks) return { sprintTasks: [], backlogTasks: [] };

    // Filter out archived tasks
    const activeTasks = tasks.filter(t => !t.archived);

    return {
      sprintTasks: activeTasks.filter((t) => t.sprintId === activeSprint?.id),
      backlogTasks: activeTasks.filter((t) => !t.sprintId),
    };
  }, [tasks, activeSprint]);

  const projectKey = project?.name?.substring(0, 4).toUpperCase() || "PROJ";
  const isAdmin = user?.roles?.includes("ADMIN") || user?.roles?.includes("OWNER");
  const isProjectArchived = project?.status === "ARCHIVED";

  const handleDragEnd = async (result: DropResult) => {
    const { source, destination, draggableId } = result;

    if (!destination) return;

    if (
      source.droppableId === destination.droppableId &&
      source.index === destination.index
    ) {
      return;
    }

    const taskId = parseInt(draggableId.replace("task-", ""));
    const targetSprintId =
      destination.droppableId === "active-sprint" ? activeSprint?.id || null : null;

    const startTime = Date.now();
    
    try {
      await moveTask.mutateAsync({ taskId, sprintId: targetSprintId });
      toast.success(
        targetSprintId
          ? "Task moved to active sprint"
          : "Task moved to backlog"
      );
    } catch (error: any) {
      // Wait at least 1 second before showing error and rolling back
      const elapsed = Date.now() - startTime;
      if (elapsed < 1000) {
        await new Promise(resolve => setTimeout(resolve, 1000 - elapsed));
      }
      
      const errorMessage =
        error?.response?.data?.error ||
        error?.response?.data?.message ||
        "Failed to move task";
      toast.error(errorMessage);
      console.error("Error moving task:", error);
    }
  };

  const handleUpdateStoryPoints = async (taskId: number, storyPoints: number | null) => {
    try {
      await updateTask.mutateAsync({
        taskId,
        updates: { storyPoints },
      });
    } catch (error: any) {
      const errorMessage =
        error?.response?.data?.error ||
        error?.response?.data?.message ||
        "Failed to update story points";
      toast.error(errorMessage);
    }
  };

  const handleUpdateAssignee = async (taskId: number, assigneeId: number | null) => {
    try {
      if (assigneeId === null) {
        // Handle unassignment if needed
        return;
      }
      await taskService.updateTaskAssignee(taskId, assigneeId);
      // Invalidate queries to refresh data
      queryClient.invalidateQueries({ queryKey: ["backlog", projectId] });
      queryClient.invalidateQueries({ queryKey: ["board", projectId] });
      toast.success("Assignee updated");
    } catch (error: any) {
      const errorMessage =
        error?.response?.data?.error ||
        error?.response?.data?.message ||
        "Failed to update assignee";
      toast.error(errorMessage);
    }
  };

  const handleCompleteSprint = async () => {
    if (!activeSprint) return;

    try {
      await completeSprint.mutateAsync(activeSprint.id);
      toast.success("Sprint completed successfully");
      setCompleteDialogOpen(false);
    } catch (error: any) {
      const errorMessage =
        error?.response?.data?.error ||
        error?.response?.data?.message ||
        "Failed to complete sprint";
      toast.error(errorMessage);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-[calc(100vh-12rem)]">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-[calc(100vh-12rem)]">
        <div className="text-center">
          <AlertCircle className="h-12 w-12 text-destructive mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-destructive mb-2">
            Failed to Load Backlog
          </h2>
          <p className="text-sm text-muted-foreground">
            {error.message || "An error occurred while loading the backlog"}
          </p>
        </div>
      </div>
    );
  }

  // Transform project members to match expected interface
  // API returns: { user: { id, name, email }, role }
  const members = (project?.members as any[] || [])
    .filter((m: any) => m?.user?.id && m?.user?.name)
    .map((m: any) => ({
      id: m.user.id,
      name: m.user.name,
      email: m.user.email,
    }));

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-3xl font-bold">Backlog</h1>
        <p className="text-muted-foreground mt-1">
          Manage your product backlog for {project?.name}
        </p>
      </div>

      <DragDropContext onDragEnd={handleDragEnd}>
        <div className="space-y-4">
          {/* Active Sprint Section */}
          {activeSprint && (
            <BacklogSection
              title={`${activeSprint.name} (Active Sprint)`}
              droppableId="active-sprint"
              tasks={sprintTasks}
              projectKey={projectKey}
              collapsible={true}
              showCompleteButton={isAdmin}
              onComplete={() => setCompleteDialogOpen(true)}
              members={members}
              onUpdateStoryPoints={handleUpdateStoryPoints}
              onUpdateAssignee={handleUpdateAssignee}
              isProjectArchived={isProjectArchived}
            />
          )}

          {/* Backlog Section */}
          <BacklogSection
            title="Backlog"
            droppableId="backlog"
            tasks={backlogTasks}
            projectKey={projectKey}
            members={members}
            onUpdateStoryPoints={handleUpdateStoryPoints}
            onUpdateAssignee={handleUpdateAssignee}
            isProjectArchived={isProjectArchived}
          />
        </div>
      </DragDropContext>

      {/* Complete Sprint Confirmation Dialog */}
      <AlertDialog open={completeDialogOpen} onOpenChange={setCompleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Complete Sprint?</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to complete {activeSprint?.name}? This will move all
              incomplete tasks back to the backlog.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleCompleteSprint}>
              Complete Sprint
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <IssueDetailModal />
    </div>
  );
}
