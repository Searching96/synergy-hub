import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { taskService } from "@/services/task.service";

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
  IN_REVIEW: BoardTask[];
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

export type TaskStatus = "TO_DO" | "IN_PROGRESS" | "IN_REVIEW" | "DONE";

export const COLUMN_ORDER: TaskStatus[] = ["TO_DO", "IN_PROGRESS", "IN_REVIEW", "DONE"];

export const COLUMN_LABELS: Record<TaskStatus, string> = {
  TO_DO: "To Do",
  IN_PROGRESS: "In Progress",
  IN_REVIEW: "In Review",
  DONE: "Done",
};

// Helper function to group tasks by status
export const groupTasksByStatus = (tasks: BoardTask[]): SprintColumn => {
  const grouped: SprintColumn = {
    TO_DO: [],
    IN_PROGRESS: [],
    IN_REVIEW: [],
    DONE: [],
  };

  tasks.forEach((task) => {
    const status = task.status as TaskStatus;
    if (grouped[status]) {
      grouped[status].push(task);
    }
  });

  return grouped;
};

export function useProjectBoard(projectId: string | undefined) {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ["board", projectId],
    queryFn: async () => {
      if (!projectId) throw new Error("Project ID is required");
      const response = await taskService.getProjectBoard(projectId);
      return response.data as BoardData;
    },
    enabled: !!projectId,
    staleTime: 2 * 60 * 1000, // Cache for 2 minutes
  });

  const updateTaskMutation = useMutation({
    mutationFn: async ({
      taskId,
      status,
    }: {
      taskId: number;
      status: TaskStatus;
    }) => {
      const response = await taskService.updateTask(taskId, { status });
      return response.data;
    },
    onMutate: async ({ taskId, status }) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: ["board", projectId] });

      // Snapshot previous value
      const previousData = queryClient.getQueryData<{ data: BoardData }>(["board", projectId]);

      // Optimistically update to the new value
      if (previousData?.data) {
        const newData = JSON.parse(JSON.stringify(previousData)) as typeof previousData;
        const activeSprint = newData.data.activeSprints[0];
        
        if (activeSprint) {
          const taskIndex = activeSprint.tasks.findIndex(t => t.id === taskId);
          if (taskIndex !== -1) {
            activeSprint.tasks[taskIndex].status = status;
          }
        }
        
        queryClient.setQueryData(["board", projectId], newData);
      }

      return { previousData };
    },
    onError: (err, variables, context) => {
      // Rollback to previous value on error
      if (context?.previousData) {
        queryClient.setQueryData(["board", projectId], context.previousData);
      }
    },
    onSettled: () => {
      // Always refetch after error or success
      queryClient.invalidateQueries({ queryKey: ["board", projectId] });
    },
  });

  const moveTask = async (taskId: number, newStatus: TaskStatus) => {
    await updateTaskMutation.mutateAsync({ taskId, status: newStatus });
  };

  return {
    ...query,
    moveTask,
    isMoving: updateTaskMutation.isPending,
  };
}
