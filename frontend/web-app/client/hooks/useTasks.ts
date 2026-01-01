import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { taskService } from "@/services/task.service";
import { useToast } from "@/hooks/use-toast";

export function useCreateTask() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (taskData: any) => taskService.createTask(taskData),
    onSuccess: () => {
      // Invalidate all project-related queries to refresh task counts
      queryClient.invalidateQueries({ queryKey: ["projects"] });
      queryClient.invalidateQueries({ queryKey: ["project"] });
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      toast({
        title: "Success",
        description: "Issue created successfully",
      });
    },
    onError: (error: any) => {
      toast({
        title: "Error",
        description: error?.response?.data?.message || "Failed to create issue",
        variant: "destructive",
      });
    },
  });
}

export function useTask(taskId: number) {
  return useQuery({
    queryKey: ["task", taskId],
    queryFn: () => taskService.getTaskById(taskId),
    enabled: !!taskId,
  });
}

export function useProjectTasks(projectId: number, params?: any) {
  return useQuery({
    queryKey: ["tasks", projectId, params],
    queryFn: () => taskService.getProjectTasks(projectId, params),
    enabled: !!projectId,
  });
}
