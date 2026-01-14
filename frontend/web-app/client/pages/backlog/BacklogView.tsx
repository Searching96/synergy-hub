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
import { useQueryClient, useQuery } from "@tanstack/react-query";
import BacklogTaskRow from "@/components/backlog/BacklogTaskRow";
import EpicPanel from "@/components/backlog/EpicPanel";
import IssueDetailPanel from "@/components/backlog/IssueDetailPanel";
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
import { ProjectBreadcrumb } from "@/components/project/ProjectBreadcrumb";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Loader2, AlertCircle, CheckCircle2, Play, Pause, Plus, ChevronDown, ChevronRight } from "lucide-react";
import { toast } from "sonner";

export default function BacklogView() {
  const { projectId } = useParams<{ projectId: string }>();
  const { project } = useProject();
  const { user } = useAuth();
  const { data: tasksData, isLoading, error } = useBacklogTasks(projectId);
  const tasks = tasksData?.data?.content || [];
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

  type DraftIssue = { id: string; type: "STORY" | "TASK" | "BUG"; title: string };
  const [draftIssues, setDraftIssues] = useState<DraftIssue[]>([]);

  // New state for 3-column layout
  const [showEpicPanel, setShowEpicPanel] = useState(false);
  const [selectedEpicId, setSelectedEpicId] = useState<string | undefined>();
  const [showIssueDetail, setShowIssueDetail] = useState(false);
  const [selectedIssueId, setSelectedIssueId] = useState<number | undefined>();

  const [startForm, setStartForm] = useState({
    name: "",
    goal: "",
    startDate: "",
    endDate: "",
  });

  // Mock epics data - replace with actual API call
  const { data: epicsData } = useQuery({
    queryKey: ["epics", projectId],
    queryFn: async () => {
      if (!projectId) return [];
      const response = await taskService.getProjectEpics(projectId);
      return response.data || [];
    },
    enabled: !!projectId,
  });

  const epics = useMemo(() => {
    if (!epicsData) return [];
    return epicsData.map((epic) => {
      const epicTasks = tasks?.filter((t) => t.epicId === epic.id) || [];
      return {
        id: epic.id.toString(),
        name: epic.title,
        issueCount: epicTasks.length,
        startDate: epic.createdAt, // Using createdAt as fallback for startDate
        dueDate: epic.dueDate || "",
      };
    });
  }, [epicsData, tasks]);

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
          const response = await sprintService.createSprint({
            projectId: parseInt(projectId || "0", 10),
            name: nextName,
            goal: "Carry over incomplete work",
            startDate: startDate.toISOString().split("T")[0],
            endDate: endDate.toISOString().split("T")[0],
          });
          const newSprint = response.data;

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

  const addDraftIssue = () => {
    const id = typeof crypto !== "undefined" && crypto.randomUUID ? crypto.randomUUID() : `draft-${Date.now()}`;
    setDraftIssues((prev) => [...prev, { id, type: "STORY", title: "" }]);
  };

  const updateDraftIssue = (id: string, updates: Partial<DraftIssue>) => {
    setDraftIssues((prev) => prev.map((draft) => (draft.id === id ? { ...draft, ...updates } : draft)));
  };

  const removeDraftIssue = (id: string) => {
    setDraftIssues((prev) => prev.filter((draft) => draft.id !== id));
  };

  const handleDraftCreate = async (draft: DraftIssue) => {
    if (!projectId || !draft.title.trim()) return;
    try {
      await createTask.mutateAsync({
        projectId: parseInt(projectId, 10),
        title: draft.title.trim(),
        description: null,
        priority: "MEDIUM",
        type: draft.type,
        storyPoints: null,
        dueDate: null,
        sprintId: null,
        parentTaskId: null,
        assigneeId: null,
      });
      removeDraftIssue(draft.id);
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
    <>
      {/* Header Section - Outside flex layout */}
      <div className="space-y-6 pb-4">
        {/* Breadcrumbs */}
        <div className="mb-4">
          <ProjectBreadcrumb current="Backlog" />
        </div>

        {/* Page Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">Backlog</h1>
            <p className="text-sm text-muted-foreground mt-1">
              Plan and prioritize your team's work
            </p>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setShowEpicPanel(!showEpicPanel)}
          >
            {showEpicPanel ? "Hide Epics" : "Show Epics"}
          </Button>
        </div>

        {/* Toolbar - Search & Filters */}
        <div className="flex items-center gap-3 bg-white border rounded-lg p-3">
          <Input
            placeholder="Search backlog..."
            className="max-w-md"
          />
          <div className="flex items-center gap-2 ml-auto">
            <Button variant="outline" size="sm">
              Filter
            </Button>
          </div>
        </div>
      </div>

      {/* Main Content Area - 3 Column Flex Layout */}
      <div className="flex gap-0 h-[calc(100vh-19rem)] bg-white border rounded-lg overflow-hidden">
        {/* Epic Panel - Left Column (Collapsible) */}
        {showEpicPanel && (
          <EpicPanel
            epics={epics}
            selectedEpicId={selectedEpicId}
            onSelectEpic={(epicId) => {
              setSelectedEpicId(epicId);
              setShowIssueDetail(true);
            }}
            onClose={() => setShowEpicPanel(false)}
          />
        )}

        {/* Backlog Content - Center Column (Flex-grow) */}
        <div className="flex-1 min-w-0 flex flex-col overflow-hidden">
          <DragDropContext onDragEnd={handleDragEnd}>
            {/* Sprint Container */}
            <section className="border-b border-gray-200 flex-1 min-h-0 flex flex-col overflow-hidden">
              {/* Sprint Header */}
              <header
                className="flex items-center justify-between px-4 py-3 bg-white border-b cursor-pointer hover:bg-gray-50 transition-colors flex-shrink-0"
                onClick={() => setIsSprintCollapsed((prev) => !prev)}
              >
                <div className="flex items-center gap-3">
                  {isSprintCollapsed ? (
                    <ChevronRight className="h-5 w-5 text-gray-500" />
                  ) : (
                    <ChevronDown className="h-5 w-5 text-gray-500" />
                  )}
                  <div>
                    <div className="flex items-center gap-2">
                      <h2 className="text-sm font-bold uppercase tracking-wide">
                        {currentSprint ? currentSprint.name : "Sprint (No active sprint)"}
                      </h2>
                      {currentSprint && (
                        <span className="text-xs px-2 py-0.5 rounded-full bg-blue-100 text-blue-700 font-medium">
                          {currentSprint.status}
                        </span>
                      )}
                    </div>
                    {currentSprint && (
                      <p className="text-xs text-muted-foreground mt-0.5">
                        {currentSprint.startDate && currentSprint.endDate
                          ? `${new Date(currentSprint.startDate).toLocaleDateString()} - ${new Date(currentSprint.endDate).toLocaleDateString()}`
                          : "No dates set"}
                      </p>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <span className="text-xs px-2 py-1 bg-gray-100 rounded border text-gray-600 font-medium">
                    {sprintTasks.length} {sprintTasks.length === 1 ? "issue" : "issues"}
                  </span>
                  {currentSprint && currentSprint.status !== "ACTIVE" && (
                    <Button
                      size="sm"
                      variant="default"
                      onClick={(e) => {
                        e.stopPropagation();
                        setStartModalOpen(true);
                      }}
                      disabled={!isAdmin || isProjectArchived || sprintTasks.length === 0}
                      className="ml-2"
                    >
                      <Play className="h-4 w-4 mr-2" />
                      Start sprint
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
                      className="ml-2"
                    >
                      <CheckCircle2 className="h-4 w-4 mr-2" />
                      Complete sprint
                    </Button>
                  )}
                </div>
              </header>

              {/* Sprint Body */}
              {!isSprintCollapsed && (
                <Droppable droppableId="sprint">
                  {(provided, snapshot) => (
                    <div
                      ref={provided.innerRef}
                      {...provided.droppableProps}
                      className={`flex-1 min-h-0 overflow-y-auto p-4 transition-colors ${snapshot.isDraggingOver ? "bg-blue-50" : "bg-gray-50"
                        }`}
                    >
                      {sprintTasks.length === 0 ? (
                        <div className="border-2 border-dashed border-gray-300 rounded-lg min-h-[200px] flex flex-col items-center justify-center text-center p-8 bg-white">
                          <div className="mb-3 text-gray-400">
                            <svg className="h-16 w-16 mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                            </svg>
                          </div>
                          <h3 className="text-base font-semibold text-gray-900 mb-2">
                            Plan your sprint
                          </h3>
                          <p className="text-sm text-gray-500 max-w-md">
                            Drag issues from the backlog section below to plan your sprint. Once you're ready, start the sprint.
                          </p>
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

              {/* Sprint Footer */}
              {!isSprintCollapsed && (
                <div className="px-4 py-3 border-t bg-white flex-shrink-0">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={addDraftIssue}
                    disabled={isProjectArchived}
                    className="text-gray-600 hover:text-gray-900"
                  >
                    <Plus className="h-4 w-4 mr-2" />
                    Create issue
                  </Button>
                </div>
              )}
            </section>

            {/* Backlog Container */}
            <section className="border-b border-gray-200 flex-1 min-h-0 flex flex-col overflow-hidden">
              {/* Backlog Header */}
              <header className="flex items-center justify-between px-4 py-3 bg-white border-b flex-shrink-0">
                <div className="flex items-center gap-3">
                  <ChevronDown className="h-5 w-5 text-gray-500" />
                  <div>
                    <h2 className="text-sm font-bold uppercase tracking-wide">
                      Backlog ({backlogTasks.length} {backlogTasks.length === 1 ? "issue" : "issues"})
                    </h2>
                  </div>
                </div>

                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => setCreateSprintOpen(true)}
                  disabled={isProjectArchived}
                >
                  <Plus className="h-4 w-4 mr-2" />
                  Create sprint
                </Button>
              </header>

              {/* Backlog Body */}
              <Droppable droppableId="backlog">
                {(provided, snapshot) => (
                  <div
                    ref={provided.innerRef}
                    {...provided.droppableProps}
                    className={`flex-1 min-h-0 overflow-y-auto p-4 transition-colors ${snapshot.isDraggingOver ? "bg-blue-50" : "bg-gray-50"
                      }`}
                  >
                    {draftIssues.map((draft) => (
                      <div
                        key={draft.id}
                        className="bg-white border rounded-lg p-3 mb-2 grid grid-cols-[auto_auto_1fr_auto_auto] gap-3 items-center"
                      >
                        <Select
                          value={draft.type}
                          onValueChange={(value) => {
                            if (value === "MANAGE") {
                              window.open("/settings/issue-types", "_blank");
                              return;
                            }
                            updateDraftIssue(draft.id, { type: value as DraftIssue["type"] });
                          }}
                        >
                          <SelectTrigger className="h-8 text-xs w-24">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="STORY">Story</SelectItem>
                            <SelectItem value="TASK">Task</SelectItem>
                            <SelectItem value="BUG">Bug</SelectItem>
                            <SelectItem value="MANAGE">Manage issue types</SelectItem>
                          </SelectContent>
                        </Select>

                        <span className="text-sm font-medium text-muted-foreground">NEW</span>

                        <Input
                          autoFocus
                          value={draft.title}
                          onChange={(e) => updateDraftIssue(draft.id, { title: e.target.value })}
                          onKeyDown={(e) => {
                            if (e.key === "Enter") {
                              e.preventDefault();
                              handleDraftCreate(draft);
                            }
                            if (e.key === "Escape") {
                              removeDraftIssue(draft.id);
                            }
                          }}
                          placeholder="What needs to be done?"
                          className="h-8 text-sm"
                        />

                        <Button
                          variant="outline"
                          size="sm"
                          className="h-8"
                          onClick={() => handleDraftCreate(draft)}
                          disabled={createTask.isPending || draft.title.trim().length === 0}
                        >
                          Add
                        </Button>

                        <Button
                          variant="ghost"
                          size="sm"
                          className="h-8"
                          onClick={() => removeDraftIssue(draft.id)}
                        >
                          Cancel
                        </Button>
                      </div>
                    ))}

                    {backlogTasks.length === 0 && draftIssues.length === 0 ? (
                      <div className="border-2 border-dashed border-gray-300 rounded-lg min-h-[120px] flex items-center justify-center text-center p-6 bg-white">
                        <p className="text-sm text-gray-500">
                          Your backlog is empty
                        </p>
                      </div>
                    ) : (
                      backlogTasks.map((task, index) => (
                        <div
                          key={task.id}
                          onClick={() => {
                            setSelectedIssueId(task.id);
                            setShowIssueDetail(true);
                          }}
                          className="cursor-pointer"
                        >
                          <BacklogTaskRow
                            task={task}
                            index={index}
                            projectKey={projectKey}
                            members={members}
                            onUpdateStoryPoints={handleUpdateStoryPoints}
                            onUpdateAssignee={handleUpdateAssignee}
                            isProjectArchived={isProjectArchived}
                          />
                        </div>
                      ))
                    )}
                    {provided.placeholder}
                  </div>
                )}
              </Droppable>

              {/* Backlog Footer */}
              <div className="px-4 py-3 border-t bg-white flex-shrink-0">
                <div className="flex items-center gap-2">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-gray-600 hover:text-gray-900"
                    disabled={isProjectArchived}
                    onClick={addDraftIssue}
                  >
                    <Plus className="h-4 w-4 mr-2" />
                    Create issue
                  </Button>
                </div>
              </div>
            </section>
          </DragDropContext>
        </div>

        {/* Issue Detail Panel - Right Column (Collapsible) */}
        {showIssueDetail && selectedIssueId && (
          <IssueDetailPanel
            issueKey={`${projectKey}-${selectedIssueId}`}
            issueType="TASK"
            title="Sample Task Title"
            status="To Do"
            description="This is a sample task description"
            onClose={() => {
              setShowIssueDetail(false);
              setSelectedIssueId(undefined);
            }}
          />
        )}
      </div>

      {/* Dialogs & Modals */}
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
    </>
  );
}
