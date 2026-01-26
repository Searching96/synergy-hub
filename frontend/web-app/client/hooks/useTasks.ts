import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { taskService } from "@/services/task.service";
import { useToast } from "@/hooks/use-toast";
import { useAuth } from "@/context/AuthContext";

export function useCreateTask() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (taskData: any) => taskService.createTask(taskData),
    onSuccess: () => {
      // Invalidate all task-related queries to refresh everywhere
      queryClient.invalidateQueries({ queryKey: ["projects"] });
      queryClient.invalidateQueries({ queryKey: ["project"] });
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      queryClient.invalidateQueries({ queryKey: ["my-issues"] });
      queryClient.invalidateQueries({ queryKey: ["backlog"] });
      queryClient.invalidateQueries({ queryKey: ["board"] });
      queryClient.invalidateQueries({ queryKey: ["my-projects"] });
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
    queryFn: () => taskService.getTask(taskId),
    enabled: !!taskId,
  });
}

export function useProjectTasks(projectId: number | string, params?: any) {
  return useQuery({
    queryKey: ["tasks", projectId, params],
    queryFn: async () => {
      const response = await taskService.getProjectTasks(projectId, params);
      const data = response.data;
      if (Array.isArray(data)) return data;
      return (data as any)?.content || [];
    },
    enabled: !!projectId,
  });
}

export function useProjectEpics(projectId: number | string) {
  return useQuery({
    queryKey: ["epics", projectId],
    queryFn: async () => {
      const response = await taskService.getProjectEpics(projectId);
      return response.data || [];
    },
    enabled: !!projectId,
  });
}

export function useTaskSubtasks(taskId: number | undefined) {
  return useQuery({
    queryKey: ["task", taskId, "subtasks"],
    queryFn: () => taskService.getTaskSubtasks(taskId!),
    enabled: !!taskId,
  });
}

export function useCreateSubtask() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (data: any) => taskService.createTask(data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["task", variables.parentTaskId, "subtasks"] });
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      toast({
        title: "Success",
        description: "Subtask created successfully",
      });
    },
    onError: (error: any) => {
      toast({
        title: "Error",
        description: error?.response?.data?.message || "Failed to create subtask",
        variant: "destructive",
      });
    },
  });
}

export function useTaskAttachments(taskId: number | undefined) {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: ["task", taskId, "attachments"],
    queryFn: () => taskService.getTaskAttachments(taskId!),
    enabled: !!taskId && isAuthenticated,
  });
}

export function useUploadAttachment() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: ({ taskId, file }: { taskId: number; file: File }) =>
      taskService.uploadAttachment(taskId, file),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["task", variables.taskId, "attachments"] });
      toast({
        title: "Success",
        description: "Attachment uploaded successfully",
      });
    },
    onError: (error: any) => {
      toast({
        title: "Error",
        description: error?.response?.data?.message || "Failed to upload attachment",
        variant: "destructive",
      });
    },
  });
}

export function useDeleteAttachment() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: ({ attachmentId, taskId }: { attachmentId: number, taskId: number }) =>
      taskService.deleteAttachment(taskId, attachmentId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["task", variables.taskId, "attachments"] });
      toast({
        title: "Success",
        description: "Attachment deleted successfully",
      });
    },
    onError: (error: any) => {
      toast({
        title: "Error",
        description: error?.response?.data?.message || "Failed to delete attachment",
        variant: "destructive",
      });
    },
  });
}

export function useArchiveTask() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (taskId: number | string) => taskService.archiveTask(taskId),
    onSuccess: (_, taskId) => {
      // Invalidate all task-related queries
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      queryClient.invalidateQueries({ queryKey: ["task", taskId] });
      queryClient.invalidateQueries({ queryKey: ["backlog"] });
      queryClient.invalidateQueries({ queryKey: ["board"] });
      toast({
        title: "Success",
        description: "Issue archived successfully",
      });
    },
    onError: (error: any) => {
      toast({
        title: "Error",
        description: error?.response?.data?.message || "Failed to archive issue",
        variant: "destructive",
      });
    },
  });
}

export function useUnarchiveTask() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (taskId: number | string) => taskService.unarchiveTask(taskId),
    onSuccess: (_, taskId) => {
      // Invalidate all task-related queries
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      queryClient.invalidateQueries({ queryKey: ["task", taskId] });
      queryClient.invalidateQueries({ queryKey: ["backlog"] });
      queryClient.invalidateQueries({ queryKey: ["board"] });
      toast({
        title: "Success",
        description: "Issue unarchived successfully",
      });
    },
    onError: (error: any) => {
      toast({
        title: "Error",
        description: error?.response?.data?.message || "Failed to unarchive issue",
        variant: "destructive",
      });
    },
  });
}

export function useWatchTask() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (taskId: number | string) => taskService.watchTask(taskId),
    onSuccess: (response, taskId) => {
      queryClient.setQueryData(["task", typeof taskId === 'string' ? parseInt(taskId) : taskId], response);
      // Invalidate projects to update counter if any
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      toast({
        title: "Watching",
        description: "You are now watching this issue",
      });
    },
    onError: (error: any) => {
      toast({
        title: "Error",
        description: error?.response?.data?.message || "Failed to watch issue",
        variant: "destructive",
      });
    },
  });
}

export function useUnwatchTask() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (taskId: number | string) => taskService.unwatchTask(taskId),
    onSuccess: (response, taskId) => {
      queryClient.setQueryData(["task", typeof taskId === 'string' ? parseInt(taskId) : taskId], response);
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      toast({
        title: "Unwatched",
        description: "You stopped watching this issue",
      });
    },
    onError: (error: any) => {
      toast({
        title: "Error",
        description: error?.response?.data?.message || "Failed to unwatch issue",
        variant: "destructive",
      });
    },
  });
}

export function useLinkTasks() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: ({ taskId, linkedTaskId }: { taskId: number | string, linkedTaskId: number | string }) =>
      taskService.linkTasks(taskId, linkedTaskId),
    onSuccess: (_, variables) => {
      const id = typeof variables.taskId === 'string' ? parseInt(variables.taskId) : variables.taskId;
      queryClient.invalidateQueries({ queryKey: ["task", id] });
      toast({
        title: "Linked",
        description: "Tasks linked successfully",
      });
    },
    onError: (error: any) => {
      toast({
        title: "Error",
        description: error?.response?.data?.message || "Failed to link tasks",
        variant: "destructive",
      });
    },
  });
}

export function useDeleteTask() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (taskId: number | string) => taskService.deleteTask(taskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      queryClient.invalidateQueries({ queryKey: ["backlog"] });
      queryClient.invalidateQueries({ queryKey: ["board"] });
      toast({
        title: "Success",
        description: "Issue deleted successfully",
      });
    },
    onError: (error: any) => {
      toast({
        title: "Error",
        description: error?.response?.data?.message || "Failed to delete issue",
        variant: "destructive",
      });
    },
  });
}
