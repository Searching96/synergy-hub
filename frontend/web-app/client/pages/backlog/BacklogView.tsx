import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { DragDropContext, DropResult, Droppable } from "@hello-pangea/dnd";
import { useProject } from "@/context/ProjectContext";
import { useAuth } from "@/context/AuthContext";
import {
  useBacklogTasks,
  useMoveTaskToSprint,
  useUpdateTaskInline,
} from "@/hooks/useBacklog";
import { useProjectSprints, useCompleteSprint, useStartSprint } from "@/hooks/useSprints";
import { useCreateTask } from "@/hooks/useTasks";
import { taskService } from "@/services/task.service";
import { sprintService } from "@/services/sprint.service";
import { useQueryClient } from "@tanstack/react-query";
import BacklogTaskRow from "@/components/backlog/BacklogTaskRow";
import CreateSprintDialog from "@/components/sprint/CreateSprintDialog";
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
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Loader2, AlertCircle, CheckCircle2, Play, Pause, Plus } from "lucide-react";
import { toast } from "sonner";

export default function BacklogView() {
  const { projectId } = useParams<{ projectId: string }>();
  const { project } = useProject();
  const { user } = useAuth();
  const { data: tasks, isLoading, error } = useBacklogTasks(projectId);
  const { data: sprints } = useProjectSprints(projectId);
  const moveTask = useMoveTaskToSprint(projectId);
  const updateTask = useUpdateTaskInline(projectId);
  const completeSprint = useCompleteSprint(projectId);
  const startSprint = useStartSprint(projectId);
  const createTask = useCreateTask();
  const queryClient = useQueryClient();

  const [createSprintOpen, setCreateSprintOpen] = useState(false);
  const [isSprintCollapsed, setIsSprintCollapsed] = useState(false);
  const [startModalOpen, setStartModalOpen] = useState(false);
  const [completeModalOpen, setCompleteModalOpen] = useState(false);
  const [completeDestination, setCompleteDestination] = useState<"backlog" | "new">("backlog");
  const [inlineTitle, setInlineTitle] = useState("");
  const [startForm, setStartForm] = useState({
    name: "",
    goal: "",
    startDate: "",
    endDate: "",
  });

  const currentSprint = useMemo(() => {
    if (!sprints || sprints.length === 0) return null;
    return (
      sprints.find((s) => s.status === "ACTIVE") ||
      sprints.find((s) => s.status === "PLANNED" || s.status === "PLANNING") ||
      sprints[0]
    );
  }, [sprints]);

  useEffect(() => {
    if (currentSprint) {
      setStartForm({
        name: currentSprint.name,
        goal: currentSprint.goal || "",
        startDate: currentSprint.startDate?.split("T")[0] || new Date().toISOString().split("T")[0],
        endDate: currentSprint.endDate?.split("T")[0] || "",
      });
    }
  }, [currentSprint?.id]);

  const { sprintTasks, backlogTasks } = useMemo(() => {
    if (!tasks) return { sprintTasks: [], backlogTasks: [] };
    const activeTasks = tasks.filter((task) => !task.archived);
    const sprintId = currentSprint?.id;
    return {
      sprintTasks: activeTasks.filter((task) => task.sprintId === sprintId),
      backlogTasks: activeTasks.filter((task) => !task.sprintId),
    };
  }, [tasks, currentSprint?.id]);

  const projectKey = project?.name?.substring(0, 4).toUpperCase() || "PROJ";
  const isAdmin = user?.roles?.includes("ADMIN") || user?.roles?.includes("OWNER");
  const isProjectArchived = project?.status === "ARCHIVED";

  const members = (project?.members as any[] || [])
    .filter((member: any) => member?.user?.id && member?.user?.name)
    .map((member: any) => ({
      id: member.user.id,
      name: member.user.name,
      email: member.user.email,
    }));

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
    const targetSprintId = destination.droppableId === "sprint" ? currentSprint?.id || null : null;

    try {
      await moveTask.mutateAsync({ taskId, sprintId: targetSprintId });
      toast.success(targetSprintId ? "Task moved to sprint" : "Task moved to backlog");
    } catch (err: any) {
      const message = err?.response?.data?.error || err?.response?.data?.message || "Failed to move task";
      toast.error(message);
    }
  };

  const handleUpdateStoryPoints = async (taskId: number, storyPoints: number | null) => {
    try {
      await updateTask.mutateAsync({ taskId, updates: { storyPoints } });
    } catch (err: any) {
      const message = err?.response?.data?.error || err?.response?.data?.message || "Failed to update story points";
      toast.error(message);
    }
  };

  const handleUpdateAssignee = async (taskId: number, assigneeId: number | null) => {
    try {
      if (assigneeId === null) return;
      await taskService.updateTaskAssignee(taskId, assigneeId);
      queryClient.invalidateQueries({ queryKey: ["backlog", projectId] });
      queryClient.invalidateQueries({ queryKey: ["board", projectId] });
      toast.success("Assignee updated");
    } catch (err: any) {
      const message = err?.response?.data?.error || err?.response?.data?.message || "Failed to update assignee";
      toast.error(message);
    }
  };

  const handleStartSprint = async () => {
    if (!currentSprint) return;

    if (!startForm.name.trim() || !startForm.startDate || !startForm.endDate) {
      toast.error("Name, start date, and end date are required");
      return;
    }

    try {
      await sprintService.updateSprint(currentSprint.id, {
        name: startForm.name,
        goal: startForm.goal,
        startDate: startForm.startDate,
        endDate: startForm.endDate,
      });
      await startSprint.mutateAsync(currentSprint.id);
      toast.success("Sprint started");
      setStartModalOpen(false);
    } catch (err: any) {
      const message = err?.response?.data?.error || err?.response?.data?.message || "Failed to start sprint";
      toast.error(message);
    }
  };

  const handleCompleteSprint = async () => {
    if (!currentSprint) return;

    try {
      await completeSprint.mutateAsync(currentSprint.id);

      const incomplete = sprintTasks.filter((task) => (task.status || "").toUpperCase() !== "DONE");

      if (incomplete.length > 0) {
        if (completeDestination === "backlog") {
          await Promise.all(
            incomplete.map((task) => taskService.moveTaskToBacklog(task.id))
          );
        } else {
          const nextName = `Sprint ${sprints ? sprints.length + 1 : 1}`;
          const startDate = new Date();
          const endDate = new Date(startDate);
          endDate.setDate(startDate.getDate() + 14);
          const newSprint = await sprintService.createSprint({
            projectId: parseInt(projectId || "0", 10),
            name: nextName,
            goal: "Carry over incomplete work",
            startDate: startDate.toISOString().split("T")[0],
            endDate: endDate.toISOString().split("T")[0],
          });

          const newSprintId = newSprint?.id;
          if (!newSprintId) {
            throw new Error("Unable to create new sprint for carryover");
          }

          await Promise.all(
            incomplete.map((task) => taskService.moveTaskToSprint(task.id, newSprintId))
          );
        }
      }

      toast.success("Sprint completed");
      setCompleteModalOpen(false);
    } catch (err: any) {
      const message = err?.response?.data?.error || err?.response?.data?.message || "Failed to complete sprint";
      toast.error(message);
    }
  };

  const handleInlineCreate = async () => {
    if (!inlineTitle.trim() || !projectId) return;
    try {
      await createTask.mutateAsync({
        projectId: parseInt(projectId, 10),
        title: inlineTitle,
        description: null,
        priority: "MEDIUM",
        type: "TASK",
        storyPoints: null,
        dueDate: null,
        sprintId: null,
        parentTaskId: null,
        assigneeId: null,
      });
      setInlineTitle("");
    } catch {
      // notification handled in mutation
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
          <h2 className="text-xl font-semibold text-destructive mb-2">Failed to Load Backlog</h2>
          <p className="text-sm text-muted-foreground">
            {error.message || "An error occurred while loading the backlog"}
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Backlog</h1>
          <p className="text-muted-foreground mt-1">Plan sprints and manage unassigned issues</p>
        </div>
        <Button onClick={() => setCreateSprintOpen(true)} disabled={isProjectArchived}>
          <Plus className="h-4 w-4 mr-2" />
          Create Sprint
        </Button>
      </div>

      <DragDropContext onDragEnd={handleDragEnd}>
        <section className="border rounded-lg bg-white">
          <header
            className="flex items-center justify-between p-4 border-b cursor-pointer hover:bg-gray-50"
            onClick={() => setIsSprintCollapsed((prev) => !prev)}
          >
            <div className="flex items-center gap-2">
              {isSprintCollapsed ? <Play className="h-4 w-4" /> : <Pause className="h-4 w-4" />}
              <div>
                <p className="text-lg font-semibold">
                  {currentSprint ? `${currentSprint.name} (${currentSprint.status})` : "No sprint planned"}
                </p>
                <p className="text-xs text-muted-foreground">
                  Drag issues here to plan or run the sprint
                </p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              {currentSprint && currentSprint.status !== "ACTIVE" && (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={(e) => {
                    e.stopPropagation();
                    setStartModalOpen(true);
                  }}
                  disabled={!isAdmin || isProjectArchived}
                >
                  <Play className="h-4 w-4 mr-2" />
                  Start Sprint
                </Button>
              )}
              {currentSprint && currentSprint.status === "ACTIVE" && (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={(e) => {
                    e.stopPropagation();
                    setCompleteModalOpen(true);
                  }}
                  disabled={!isAdmin || isProjectArchived}
                >
                  <CheckCircle2 className="h-4 w-4 mr-2" />
                  Complete Sprint
                </Button>
              )}
            </div>
          </header>

          {!isSprintCollapsed && (
            <Droppable droppableId="sprint">
              {(provided, snapshot) => (
                <div
                  ref={provided.innerRef}
                  {...provided.droppableProps}
                  className={`p-4 min-h-[200px] ${snapshot.isDraggingOver ? "bg-blue-50" : ""}`}
                >
                  {sprintTasks.length === 0 ? (
                    <div className="flex items-center justify-center h-24 text-sm text-muted-foreground">
                      No issues in this sprint yet
                    </div>
                  ) : (
                    sprintTasks.map((task, index) => (
                      <BacklogTaskRow
                        key={task.id}
                        task={task}
                        index={index}
                        projectKey={projectKey}
                        members={members}
                        onUpdateStoryPoints={handleUpdateStoryPoints}
                        onUpdateAssignee={handleUpdateAssignee}
                        isProjectArchived={isProjectArchived}
                      />
                    ))
                  )}
                  {provided.placeholder}
                </div>
              )}
            </Droppable>
          )}
        </section>

        <section className="border rounded-lg bg-white">
          <header className="flex items-center justify-between p-4 border-b">
            <div>
              <p className="text-lg font-semibold">Backlog</p>
              <p className="text-xs text-muted-foreground">Unassigned issues waiting for a sprint</p>
            </div>
          </header>
          <Droppable droppableId="backlog">
            {(provided, snapshot) => (
              <div
                ref={provided.innerRef}
                {...provided.droppableProps}
                className={`p-4 min-h-[200px] ${snapshot.isDraggingOver ? "bg-blue-50" : ""}`}
              >
                {backlogTasks.length === 0 ? (
                  <div className="flex items-center justify-center h-24 text-sm text-muted-foreground">
                    Backlog is empty
                  </div>
                ) : (
                  backlogTasks.map((task, index) => (
                    <BacklogTaskRow
                      key={task.id}
                      task={task}
                      index={index}
                      projectKey={projectKey}
                      members={members}
                      onUpdateStoryPoints={handleUpdateStoryPoints}
                      onUpdateAssignee={handleUpdateAssignee}
                      isProjectArchived={isProjectArchived}
                    />
                  ))
                )}
                {provided.placeholder}
                <div className="mt-3 flex items-center gap-2">
                  <span className="text-sm text-muted-foreground">+ Create Issue</span>
                  <Input
                    placeholder="Quick issue title"
                    value={inlineTitle}
                    onChange={(e) => setInlineTitle(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault();
                        handleInlineCreate();
                      }
                    }}
                    disabled={isProjectArchived || createTask.isPending}
                  />
                </div>
              </div>
            )}
          </Droppable>
        </section>
      </DragDropContext>

      <CreateSprintDialog open={createSprintOpen} onOpenChange={setCreateSprintOpen} />

      <Dialog open={startModalOpen} onOpenChange={setStartModalOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Start Sprint</DialogTitle>
            <DialogDescription>Set sprint details before starting.</DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <div className="space-y-1">
              <Label htmlFor="sprint-name">Name</Label>
              <Input
                id="sprint-name"
                value={startForm.name}
                onChange={(e) => setStartForm((prev) => ({ ...prev, name: e.target.value }))}
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="sprint-goal">Goal</Label>
              <Textarea
                id="sprint-goal"
                rows={3}
                value={startForm.goal}
                onChange={(e) => setStartForm((prev) => ({ ...prev, goal: e.target.value }))}
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label htmlFor="sprint-start">Start Date</Label>
                <Input
                  id="sprint-start"
                  type="date"
                  value={startForm.startDate}
                  onChange={(e) => setStartForm((prev) => ({ ...prev, startDate: e.target.value }))}
                />
              </div>
              <div className="space-y-1">
                <Label htmlFor="sprint-end">End Date</Label>
                <Input
                  id="sprint-end"
                  type="date"
                  value={startForm.endDate}
                  onChange={(e) => setStartForm((prev) => ({ ...prev, endDate: e.target.value }))}
                />
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setStartModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleStartSprint} disabled={startSprint.isPending}>
              {startSprint.isPending && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
              Start Sprint
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <AlertDialog open={completeModalOpen} onOpenChange={setCompleteModalOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Complete Sprint?</AlertDialogTitle>
            <AlertDialogDescription>
              Move incomplete issues to a new sprint or back to the backlog.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <div className="space-y-2">
            <Label className="text-sm font-medium">Destination</Label>
            <div className="flex items-center gap-3">
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input
                  type="radio"
                  name="complete-destination"
                  value="backlog"
                  checked={completeDestination === "backlog"}
                  onChange={() => setCompleteDestination("backlog")}
                />
                Backlog
              </label>
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input
                  type="radio"
                  name="complete-destination"
                  value="new"
                  checked={completeDestination === "new"}
                  onChange={() => setCompleteDestination("new")}
                />
                New Sprint
              </label>
            </div>
          </div>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleCompleteSprint} disabled={completeSprint.isPending}>
              {completeSprint.isPending && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
              Complete Sprint
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
