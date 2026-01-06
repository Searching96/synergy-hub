import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { taskService } from "@/services/task.service";

export interface BacklogTask {
  id: number;
  title: string;
  description?: string;
  status: string;
  priority: string;
  type: string;
  projectId: number;
  sprintId?: number | null;
  sprintName?: string | null;
  assignee?: {
    id: number;
    name: string;
    email: string;
  } | null;
  reporter?: {
    id: number;
    name: string;
    email: string;
  };
  storyPoints?: number | null;
  dueDate?: string;
  createdAt: string;
  updatedAt: string;
  overdue: boolean;
  archived: boolean;
}

export function useBacklogTasks(projectId: string | undefined) {
  return useQuery({
    queryKey: ["backlog", projectId],
    queryFn: async () => {
      if (!projectId) throw new Error("Project ID is required");
      const response = await taskService.getProjectTasks(projectId, {});
      return response.data as BacklogTask[];
    },
    enabled: !!projectId,
  });
}

export function useMoveTaskToSprint(projectId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      taskId,
      sprintId,
    }: {
      taskId: number;
      sprintId: number | null;
    }) => {
      // If sprintId is null or 0, move to backlog, otherwise move to sprint
      const response = sprintId 
        ? await taskService.moveTaskToSprint(taskId, sprintId)
        : await taskService.moveTaskToBacklog(taskId);
      return response.data;
    },
    onMutate: async ({ taskId, sprintId }) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: ["backlog", projectId] });

      // Snapshot previous value
      const previousData = queryClient.getQueryData<BacklogTask[]>(["backlog", projectId]);

      // Optimistically update
      if (previousData) {
        const newData = previousData.map(task => 
          task.id === taskId 
            ? { ...task, sprintId }
            : task
        );
        queryClient.setQueryData(["backlog", projectId], newData);
      }

      return { previousData };
    },
    onError: (err, variables, context) => {
      // Rollback on error
      if (context?.previousData) {
        queryClient.setQueryData(["backlog", projectId], context.previousData);
      }
    },
    onSettled: () => {
      // Refetch after error or success
      queryClient.invalidateQueries({ queryKey: ["backlog", projectId] });
      queryClient.invalidateQueries({ queryKey: ["board", projectId] });
      queryClient.invalidateQueries({ queryKey: ["sprints", projectId] });
    },
  });
}

export function useUpdateTaskInline(projectId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      taskId,
      updates,
    }: {
      taskId: number;
      updates: Partial<BacklogTask>;
    }) => {
      const response = await taskService.updateTask(taskId, updates);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["backlog", projectId] });
      queryClient.invalidateQueries({ queryKey: ["board", projectId] });
    },
  });
}
