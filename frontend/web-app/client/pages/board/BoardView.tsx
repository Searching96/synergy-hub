import { useMemo, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { DragDropContext, DropResult } from "@hello-pangea/dnd";
import { useProject } from "@/context/ProjectContext";
import { useAuth } from "@/context/AuthContext";
import { usePermissionError } from "@/hooks/usePermissionError";
import {
  COLUMN_LABELS,
  COLUMN_ORDER,
  TaskStatus,
  useProjectBoard,
  groupTasksByStatus,
} from "@/hooks/useProjectBoard";
import BoardColumn from "@/components/board/BoardColumn";
import CreateSprintDialog from "@/components/sprint/CreateSprintDialog";
import SprintListDialog from "@/components/sprint/SprintListDialog";
import IssueDetailModal from "@/components/issue/IssueDetailModal";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input"; // Added
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"; // Added
import { ProjectBreadcrumb } from "@/components/project/ProjectBreadcrumb";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { AlertCircle, List, Loader2, Plus, X, Search } from "lucide-react"; // Added Search
import { toast } from "sonner";
import { canMoveTask } from "@/lib/rbac";

export default function BoardView() {
  const { projectId } = useParams<{ projectId: string }>();
  const [searchParams] = useSearchParams();
  const { project, projectKey } = useProject();
  const { user } = useAuth();
  const { error: permissionError, clearError } = usePermissionError();
  const {
    activeSprint,
    backlogTasks,
    isLoading,
    isFetching,
    error,
    moveTask,
    isMoving,
  } = useProjectBoard(projectId);

  const visibleBacklog = useMemo(
    () => (backlogTasks || []).filter((task) => !task.archived),
    [backlogTasks]
  );

  const [createSprintOpen, setCreateSprintOpen] = useState(false);
  const [sprintListOpen, setSprintListOpen] = useState(false);

  // Filter State
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedAssignee, setSelectedAssignee] = useState("ALL");

  const isProjectArchived = project?.status === "ARCHIVED";

  // Derive columns directly from activeSprint data (single source of truth)
  const columns = useMemo(() => {
    if (!activeSprint) return groupTasksByStatus([]);

    let filteredTasks = (activeSprint.tasks || []).filter((task) => !task.archived);

    // Filter by Search Query
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filteredTasks = filteredTasks.filter(task =>
        task.title.toLowerCase().includes(query)
      );
    }

    // Filter by Assignee
    if (selectedAssignee && selectedAssignee !== "ALL") {
      if (selectedAssignee === "UNASSIGNED") {
        filteredTasks = filteredTasks.filter(task => !task.assignee);
      } else {
        const assigneeId = parseInt(selectedAssignee);
        filteredTasks = filteredTasks.filter(task => task.assignee?.id === assigneeId);
      }
    }

    return groupTasksByStatus(filteredTasks);
  }, [activeSprint?.tasks, searchQuery, selectedAssignee]); // Added dependencies

  const handleDragEnd = async (result: DropResult) => {
    const { source, destination } = result;

    if (!destination) return;
    if (
      source.droppableId === destination.droppableId &&
      source.index === destination.index
    ) {
      return;
    }

    const sourceStatus = source.droppableId as TaskStatus;
    const destinationStatus = destination.droppableId as TaskStatus;

    const sourceTasks = columns[sourceStatus] || [];
    const taskToMove = sourceTasks[source.index];
    if (!taskToMove) return;

    // Show loading toast during operation
    const loadingToast = toast.loading(`Moving ${taskToMove.title}...`);

    // Let useProjectBoard handle optimistic update via React Query
    try {
      await moveTask({
        taskId: taskToMove.id,
        destinationStatus,
        destinationIndex: destination.index,
        sourceStatus,
        sourceIndex: source.index,
      });
      toast.success("Task moved successfully", { id: loadingToast });
    } catch (err: any) {
      const message =
        err?.response?.data?.message || err?.response?.data?.error || "Failed to move task";
      toast.error(message, { id: loadingToast });
    }
  };

  const userRole = project?.members?.find((m) => m.userId === user?.id)?.role as any;
  const canMove = userRole ? canMoveTask(userRole) && !isProjectArchived : !isProjectArchived;

  // Show loading for initial load OR when fetching new project data
  if (isLoading || (isFetching && !activeSprint)) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen">
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

  if (!activeSprint) {
    return (
      <div className="p-6">
        <div className="mb-4">
          <ProjectBreadcrumb current="Board" />
        </div>
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
              <h3 className="text-lg font-semibold mb-2">No Active Sprint</h3>
              <p className="text-sm text-muted-foreground mb-4">
                Start or create a sprint to use the board. You can also prioritize issues from the backlog.
              </p>
              <div className="flex items-center justify-center gap-3">
                <Button asChild variant="outline">
                  <Link to={`/projects/${projectId}/backlog`}>Go to Backlog</Link>
                </Button>
                <Button onClick={() => setCreateSprintOpen(true)} disabled={isProjectArchived}>
                  <Plus className="h-4 w-4 mr-2" />
                  Create Sprint
                </Button>
              </div>
            </div>
          </div>
        </div>

        <CreateSprintDialog open={createSprintOpen} onOpenChange={setCreateSprintOpen} />
        <SprintListDialog open={sprintListOpen} onOpenChange={setSprintListOpen} />
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="mb-4">
        <ProjectBreadcrumb current="Board" />
      </div>
      {/* Permission Error Banner - Persistent until dismissed */}
      {permissionError && (
        <Alert variant="destructive" className="mb-4">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Access Denied</AlertTitle>
          <AlertDescription className="flex items-center justify-between">
            <span>{permissionError}</span>
            <Button
              variant="ghost"
              size="sm"
              onClick={clearError}
              className="ml-4 h-6 px-2"
            >
              <X className="h-4 w-4" />
            </Button>
          </AlertDescription>
        </Alert>
      )}

      <div className="mb-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold">Board</h1>
          <p className="text-muted-foreground mt-1">
            {activeSprint.name} • {Math.round(activeSprint.metrics?.completionPercentage || 0)}% complete
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <div className="relative w-48">
            <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Filter tasks..."
              className="pl-8"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <Select value={selectedAssignee} onValueChange={setSelectedAssignee}>
            <SelectTrigger className="w-[180px]">
              <SelectValue placeholder="Assignee" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All Assignees</SelectItem>
              <SelectItem value="UNASSIGNED">Unassigned</SelectItem>
              {project?.members?.map((member) => (
                <SelectItem key={member.userId} value={member.userId?.toString() || ""}>
                  {member.name || member.email}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

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
        <ErrorBoundary>
          <div className="flex gap-4 overflow-x-auto pb-4 min-h-96">
            {COLUMN_ORDER.map((status) => (
              <BoardColumn
                key={status}
                status={status}
                title={COLUMN_LABELS[status]}
                tasks={columns[status] || []}
                projectKey={projectKey}
                isProjectArchived={isProjectArchived}
                isLoading={isLoading || isMoving}
                canMove={canMove}
              />
            ))}
          </div>
        </ErrorBoundary>
      </DragDropContext>


      {visibleBacklog && visibleBacklog.length > 0 && (
        <div className="mt-8 pt-8 border-t">
          <div className="flex items-center justify-between mb-3">
            <div>
              <h2 className="text-lg font-semibold">Backlog Preview</h2>
              <p className="text-sm text-muted-foreground">Unplanned issues still in backlog</p>
            </div>
            <Button asChild variant="ghost">
              <Link to={`/projects/${projectId}/backlog`}>Open Backlog</Link>
            </Button>
          </div>
          <div className="grid gap-2">
            {visibleBacklog.slice(0, 5).map((task) => (
              <div
                key={task.id}
                className="bg-white border rounded-lg p-3 flex items-center justify-between"
              >
                <span className="text-sm font-medium truncate">{task.title}</span>
                <span className="text-xs text-muted-foreground">{task.priority}</span>
              </div>
            ))}
            {visibleBacklog.length > 5 && (
              <p className="text-sm text-muted-foreground text-center">
                +{visibleBacklog.length - 5} more tasks in backlog
              </p>
            )}
          </div>
        </div>
      )}

      <CreateSprintDialog open={createSprintOpen} onOpenChange={setCreateSprintOpen} />
      <SprintListDialog open={sprintListOpen} onOpenChange={setSprintListOpen} />
      {searchParams.get("selectedIssue") && <IssueDetailModal />}
    </div>
  );
}
