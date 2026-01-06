import { useMutation, useQueryClient } from "@tanstack/react-query";
import { taskService } from "@/services/task.service";
import { useToast } from "@/hooks/use-toast";
import type { Task } from "@/types/task.types";
import type { ApiResponse } from "@/types/auth.types";

export function useUpdateTask() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: ({ taskId, data }: { taskId: number; data: any }) =>
      taskService.updateTask(taskId, data),
    onMutate: async ({ taskId, data }) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: ["task", taskId] });
      await queryClient.cancelQueries({ queryKey: ["tasks"] });
      await queryClient.cancelQueries({ queryKey: ["board"] });
      await queryClient.cancelQueries({ queryKey: ["backlog"] });

      // Snapshot previous value INCLUDING any metadata for conflict detection
      const previousTask = queryClient.getQueryData<ApiResponse<Task>>(["task", taskId]);

      // Optimistically update the cache
      if (previousTask) {
        queryClient.setQueryData<ApiResponse<Task>>(["task", taskId], {
          ...previousTask,
          data: {
            ...previousTask.data,
            ...data,
            // Mark as optimistic (not yet persisted)
            __isOptimistic__: true,
          },
        });
      }

      return { previousTask };
    },
    onError: (error: any, variables, context) => {
      // CRITICAL FIX: Check if error is due to concurrent edit (409 Conflict)
      if (error?.response?.status === 409) {
        toast({
          title: "Conflict Detected",
          description: "This task was modified by another user. The page will refresh to show latest changes.",
          variant: "destructive",
          duration: 5000,
        });
        
        // Trigger immediate refetch to get latest version
        queryClient.invalidateQueries({ queryKey: ["task", variables.taskId] });
        queryClient.invalidateQueries({ queryKey: ["board"] });
        queryClient.invalidateQueries({ queryKey: ["backlog"] });
        return;
      }
      
      // Rollback on other errors
      if (context?.previousTask) {
        queryClient.setQueryData(["task", variables.taskId], context.previousTask);
      }
      
      toast({
        title: "Error",
        description: error?.response?.data?.message || "Failed to update task",
        variant: "destructive",
      });
    },
    onSuccess: (response, variables) => {
      // Update with server response and clear optimistic flag
      queryClient.setQueryData(["task", variables.taskId], {
        ...response,
        data: {
          ...response.data,
          __isOptimistic__: false,
        },
      });
      
      toast({
        title: "Success",
        description: "Task updated successfully",
      });
    },
    onSettled: (_, __, variables) => {
      // Refetch to ensure consistency
      queryClient.invalidateQueries({ queryKey: ["task", variables.taskId] });
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      queryClient.invalidateQueries({ queryKey: ["board"] });
      queryClient.invalidateQueries({ queryKey: ["backlog"] });
    },
  });
}

export function useUpdateTaskAssignee() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: ({ taskId, assigneeId }: { taskId: number; assigneeId: number | null }) =>
      taskService.updateTaskAssignee(taskId, assigneeId),
    onMutate: async ({ taskId, assigneeId }) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: ["task", taskId] });
      await queryClient.cancelQueries({ queryKey: ["board"] });

      // Snapshot previous value
      const previousTask = queryClient.getQueryData<ApiResponse<Task>>(["task", taskId]);

      // Optimistically update the cache
      if (previousTask) {
        queryClient.setQueryData<ApiResponse<Task>>(["task", taskId], {
          ...previousTask,
          data: {
            ...previousTask.data,
            assigneeId,
            // Update assignee object if available in cache
            assignee: assigneeId ? previousTask.data.assignee : null,
          },
        });
      }

      return { previousTask };
    },
    onError: (error: any, variables, context) => {
      // Rollback on error
      if (context?.previousTask) {
        queryClient.setQueryData(["task", variables.taskId], context.previousTask);
      }
      
      toast({
        title: "Error",
        description: error?.response?.data?.message || "Failed to update assignee",
        variant: "destructive",
      });
    },
    onSuccess: (response, variables) => {
      // Update with server response
      queryClient.setQueryData(["task", variables.taskId], response);
    },
    onSettled: (_, __, variables) => {
      queryClient.invalidateQueries({ queryKey: ["task", variables.taskId] });
      queryClient.invalidateQueries({ queryKey: ["board"] });
    },
  });
}
