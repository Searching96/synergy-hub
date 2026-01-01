import { useState } from "react";
import { useParams } from "react-router-dom";
import { DragDropContext, DropResult } from "@hello-pangea/dnd";
import { useProject } from "@/context/ProjectContext";
import { useProjectBoard, COLUMN_ORDER, COLUMN_LABELS, TaskStatus, groupTasksByStatus } from "@/hooks/useProjectBoard";
import BoardColumn from "@/components/board/BoardColumn";
import CreateSprintDialog from "@/components/sprint/CreateSprintDialog";
import SprintListDialog from "@/components/sprint/SprintListDialog";
import IssueDetailModal from "@/components/IssueDetailModal";
import { Button } from "@/components/ui/button";
import { Loader2, AlertCircle, Plus, List } from "lucide-react";
import { toast } from "sonner";

export default function BoardPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { project } = useProject();
  const { data: boardData, isLoading, error, moveTask } = useProjectBoard(projectId);
  const [createSprintOpen, setCreateSprintOpen] = useState(false);
  const [sprintListOpen, setSprintListOpen] = useState(false);

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
    const newStatus = destination.droppableId as TaskStatus;

    const startTime = Date.now();
    
    try {
      await moveTask(taskId, newStatus);
      toast.success("Task moved successfully");
    } catch (error: any) {
      // Wait at least 1 second before showing error
      const elapsed = Date.now() - startTime;
      if (elapsed < 1000) {
        await new Promise(resolve => setTimeout(resolve, 1000 - elapsed));
      }
      
      const errorMessage = error?.response?.data?.error || error?.response?.data?.message || "Failed to move task";
      toast.error(errorMessage);
      console.error("Error moving task:", error);
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
            Failed to Load Board
          </h2>
          <p className="text-sm text-muted-foreground">
            {error.message || "An error occurred while loading the board"}
          </p>
        </div>
      </div>
    );
  }

  if (!boardData || !boardData.activeSprints || boardData.activeSprints.length === 0) {
    return (
      <div>
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">Board</h1>
            <p className="text-muted-foreground mt-1">
              Kanban board for {project?.name}
            </p>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => setSprintListOpen(true)}>
              <List className="h-4 w-4 mr-2" />
              All Sprints
            </Button>
            <Button onClick={() => setCreateSprintOpen(true)} disabled={isProjectArchived}>
              <Plus className="h-4 w-4 mr-2" />
              Create Sprint
            </Button>
          </div>
        </div>
        
        <div className="border rounded-lg p-12 text-center">
          <div className="max-w-md mx-auto space-y-4">
            <AlertCircle className="h-12 w-12 text-muted-foreground mx-auto" />
            <div>
              <h3 className="text-lg font-semibold mb-2">No Active Sprints</h3>
              <p className="text-sm text-muted-foreground mb-4">
                Create a sprint and start it to begin using the board
              </p>
            </div>
          </div>
        </div>

        <CreateSprintDialog open={createSprintOpen} onOpenChange={setCreateSprintOpen} />
        <SprintListDialog open={sprintListOpen} onOpenChange={setSprintListOpen} />
      </div>
    );
  }

  const activeSprint = boardData.activeSprints[0];
  const projectKey = project?.name?.substring(0, 4).toUpperCase() || "PROJ";
  
  // Filter out archived tasks from sprint tasks and backlog
  const activeSprintTasks = (activeSprint.tasks || []).filter(task => !task.archived);
  const activeBacklogTasks = (boardData.backlogTasks || []).filter(task => !task.archived);
  
  const columns = groupTasksByStatus(activeSprintTasks);

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Board</h1>
          <p className="text-muted-foreground mt-1">
            {activeSprint.name} • {Math.round(activeSprint.metrics?.completionPercentage || 0)}% complete
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setSprintListOpen(true)}>
            <List className="h-4 w-4 mr-2" />
            All Sprints
          </Button>
          <Button onClick={() => setCreateSprintOpen(true)} disabled={isProjectArchived}>
            <Plus className="h-4 w-4 mr-2" />
            Create Sprint
          </Button>
        </div>
      </div>

      <DragDropContext onDragEnd={handleDragEnd}>
        <div className="flex gap-4 overflow-x-auto pb-4 h-[calc(100vh-16rem)]">
          {COLUMN_ORDER.map((status) => (
            <BoardColumn
              key={status}
              status={status}
              title={COLUMN_LABELS[status]}
              tasks={columns[status] || []}
              projectKey={projectKey}
              isProjectArchived={isProjectArchived}
            />
          ))}
        </div>
      </DragDropContext>

      {activeBacklogTasks && activeBacklogTasks.length > 0 && (
        <div className="mt-8 pt-8 border-t">
          <h2 className="text-lg font-semibold mb-4">
            Backlog ({activeBacklogTasks.length})
          </h2>
          <div className="grid gap-2">
            {activeBacklogTasks.slice(0, 5).map((task) => (
              <div
                key={task.id}
                className="bg-white border rounded-lg p-3 flex items-center justify-between"
              >
                <span className="text-sm font-medium">{task.title}</span>
                <span className="text-xs text-muted-foreground">
                  {task.priority}
                </span>
              </div>
            ))}
            {activeBacklogTasks.length > 5 && (
              <p className="text-sm text-muted-foreground text-center">
                +{activeBacklogTasks.length - 5} more tasks in backlog
              </p>
            )}
          </div>
        </div>
      )}

      <CreateSprintDialog open={createSprintOpen} onOpenChange={setCreateSprintOpen} />
      <SprintListDialog open={sprintListOpen} onOpenChange={setSprintListOpen} />
      <IssueDetailModal />
    </div>
  );
}
