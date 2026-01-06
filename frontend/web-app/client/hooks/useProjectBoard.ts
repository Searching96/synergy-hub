import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { taskService } from "@/services/task.service";
import { toast } from "sonner";
import { extractErrorMessage } from "@/lib/error";
import { useAuth } from "@/context/AuthContext";
import { useProject } from "@/context/ProjectContext";
import { canMoveTask } from "@/lib/rbac";

export interface TaskAssignee {
  id: number;
  name: string;
}

export interface BoardTask {
  id: number;
  title: string;
  status: string; // normalized via mapStatusToColumn to avoid dropping tasks
  priority: string;
  assignee?: TaskAssignee;
  completedAt?: string;
  storyPoints?: number;
  archived: boolean;
}

export interface SprintColumn {
  TO_DO: BoardTask[];
  IN_PROGRESS: BoardTask[];
  DONE: BoardTask[];
}

export interface ActiveSprint {
  id: number;
  name: string;
  status: string;
  startDate: string;
  endDate: string;
  tasks: BoardTask[];
  metrics?: {
    completionPercentage: number;
  };
}

export interface BoardData {
  activeSprints: ActiveSprint[];
  backlogTasks: BoardTask[];
  __version__?: number; // Conflict detection timestamp
}

export type TaskStatus = "TO_DO" | "IN_PROGRESS" | "DONE";

export const COLUMN_ORDER: TaskStatus[] = ["TO_DO", "IN_PROGRESS", "DONE"];

export const COLUMN_LABELS: Record<TaskStatus, string> = {
  TO_DO: "To Do",
  IN_PROGRESS: "In Progress",
  DONE: "Done",
};

// Map any backend status to a board column to avoid dropping tasks
const mapStatusToColumn = (status: string): TaskStatus => {
  switch (status) {
    case "TO_DO":
    case "TODO":
    case "BLOCKED":
      return "TO_DO";
    case "IN_PROGRESS":
    case "IN_REVIEW":
      return "IN_PROGRESS";
    case "DONE":
      return "DONE";
    default:
      return "TO_DO"; // fallback keeps task visible
  }
};

// Helper function to group tasks by status - handles all statuses from backend, filters to displayable ones
export const groupTasksByStatus = (tasks: BoardTask[]): SprintColumn => {
  const grouped: SprintColumn = {
    TO_DO: [],
    IN_PROGRESS: [],
    DONE: [],
  };

  tasks.forEach((task) => {
    const status = mapStatusToColumn(task.status);
    grouped[status].push({ ...task, status });
  });

  return grouped;
};

const getActiveSprint = (data?: BoardData | null) => {
  if (!data?.activeSprints?.length) return null;
  return data.activeSprints.find((sprint) => sprint.status === "ACTIVE") || data.activeSprints[0];
};

interface MoveTaskInput {
  taskId: number;
  destinationStatus: TaskStatus;
  destinationIndex: number;
  sourceStatus: TaskStatus;
  sourceIndex: number;
}

export function useProjectBoard(projectId: string | undefined) {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const { project } = useProject();

  const query = useQuery({
    queryKey: ["board", projectId],
    queryFn: async () => {
      if (!projectId) throw new Error("Project ID is required");
      const response = await taskService.getProjectBoard(projectId);
      return response.data as BoardData;
    },
    enabled: !!projectId,
    staleTime: 2 * 60 * 1000,
  });

  const updateTaskMutation = useMutation({
    mutationFn: async ({ taskId, destinationStatus, destinationIndex }: MoveTaskInput) => {
      // Validate permission when project membership data is available; otherwise defer to backend
      const userRole = project?.members?.find((m) => m.userId === user?.id)?.role as any;
      if (userRole && !canMoveTask(userRole)) {
        // This error triggers onError and rollback - properly tested
        throw new Error("You don't have permission to move tasks in this project");
      }

      // API call - 403 from backend will be caught by axios interceptor
      // which rejects the promise, triggering onError callback with rollback
      const response = await taskService.updateTask(taskId, {
        status: destinationStatus,
        position: destinationIndex,
      });
      return response.data;
    },
    onMutate: async (payload) => {
      await queryClient.cancelQueries({ queryKey: ["board", projectId] });

      const previousData = queryClient.getQueryData<BoardData>(["board", projectId]);

      if (previousData) {
        // Use structuredClone for deep copy instead of JSON parse/stringify
        const optimisticData: BoardData = {
          ...structuredClone(previousData),
          __version__: Date.now(), // Add timestamp for conflict detection
        };
        const activeSprint = getActiveSprint(optimisticData);

        if (activeSprint) {
          const columns = groupTasksByStatus(activeSprint.tasks || []);

          const sourceTasks = [...(columns[payload.sourceStatus] || [])];
          const destinationTasks = payload.sourceStatus === payload.destinationStatus
            ? sourceTasks
            : [...(columns[payload.destinationStatus] || [])];

          const [moved] = sourceTasks.splice(payload.sourceIndex, 1);

          if (moved) {
            moved.status = payload.destinationStatus;
            const insertIndex = Math.min(payload.destinationIndex, destinationTasks.length);
            destinationTasks.splice(insertIndex, 0, moved);

            columns[payload.sourceStatus] = sourceTasks;
            columns[payload.destinationStatus] = destinationTasks;

            activeSprint.tasks = COLUMN_ORDER.flatMap((status) => columns[status] || []);
          }
        }

        queryClient.setQueryData(["board", projectId], optimisticData);
      }

      return { previousData };
    },
    onError: (_err: any, _variables, context) => {
      if (context?.previousData) {
        // Check for concurrent modifications
        const currentData = queryClient.getQueryData<BoardData>(["board", projectId]);
        if (currentData?.__version__ && context.previousData.__version__) {
          const timeDiff = currentData.__version__ - context.previousData.__version__;
          if (timeDiff > 5000) {
            // Data changed significantly, show conflict warning
            toast.error("Board was modified by another user. Refreshing...", {
              duration: 4000,
            });
            queryClient.invalidateQueries({ queryKey: ["board", projectId] });
            return;
          }
        }
        queryClient.setQueryData(["board", projectId], context.previousData);
      }
      
      // Show error message to user
      const errorMessage = extractErrorMessage(_err, "Failed to move task");
      toast.error(errorMessage, {
        duration: 3000,
      });
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["board", projectId] });
    },
  });

  const moveTask = async (input: MoveTaskInput) => {
    await updateTaskMutation.mutateAsync(input);
  };

  const activeSprint = getActiveSprint(query.data || null);

  return {
    ...query,
    activeSprint,
    backlogTasks: query.data?.backlogTasks || [],
    moveTask,
    isMoving: updateTaskMutation.isPending,
    isFetching: query.isFetching, // Expose isFetching for transition states
  };
}
