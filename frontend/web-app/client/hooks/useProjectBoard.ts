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
  status: string;
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
}

export type TaskStatus = "TO_DO" | "IN_PROGRESS" | "DONE";

export const COLUMN_ORDER: TaskStatus[] = ["TO_DO", "IN_PROGRESS", "DONE"];

export const COLUMN_LABELS: Record<TaskStatus, string> = {
  TO_DO: "To Do",
  IN_PROGRESS: "In Progress",
  DONE: "Done",
};

// Helper function to group tasks by status - handles all statuses from backend, filters to displayable ones
export const groupTasksByStatus = (tasks: BoardTask[]): SprintColumn => {
  const grouped: SprintColumn = {
    TO_DO: [],
    IN_PROGRESS: [],
    DONE: [],
  };

  tasks.forEach((task) => {
    // Handle all possible statuses from backend, but only display the main three
    // IN_REVIEW and BLOCKED statuses are filtered out (can be added to board later if needed)
    const status = task.status as TaskStatus;
    if (status === "TO_DO" || status === "IN_PROGRESS" || status === "DONE") {
      grouped[status].push(task);
    }
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
      // CRITICAL FIX: Validate permission BEFORE making request
      const userRole = (project?.members?.find(m => m.userId === user?.id)?.role || "VIEWER") as any;
      if (!canMoveTask(userRole)) {
        throw new Error("You don't have permission to move tasks in this project");
      }

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
        const optimisticData: BoardData = structuredClone(previousData);
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
  };
}
