/**
 * useIssueDetail Hook
 * Consolidates all queries and mutations for the Issue Detail Modal
 * Provides a clean API for issue operations
 */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTask } from "@/hooks/useTasks";
import { useUpdateTask, useUpdateTaskAssignee } from "@/hooks/useUpdateTask";
import { useTaskComments, useAddComment } from "@/hooks/useComments";
import { projectService } from "@/services/project.service";
import { taskService } from "@/services/task.service";
import { useToast } from "@/hooks/use-toast";
import { extractErrorMessage } from "@/lib/error";
import type { Task } from "@/types/task.types";
import type { ProjectMember } from "@/types/project.types";

export interface UseIssueDetailOptions {
  taskId: number | null;
  onClose?: () => void;
}

export function useIssueDetail({ taskId, onClose }: UseIssueDetailOptions) {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  // Queries
  const taskQuery = useTask(taskId!);
  const commentsQuery = useTaskComments(taskId);
  
  const subtasksQuery = useQuery({
    queryKey: ["subtasks", taskId],
    queryFn: () => taskService.getTaskSubtasks(taskId!),
    enabled: !!taskId,
  });

  const task = taskQuery.data?.data;

  const projectQuery = useQuery({
    queryKey: ["project", task?.projectId],
    queryFn: () => projectService.getProjectById(task!.projectId.toString()),
    enabled: !!task?.projectId,
  });

  const membersQuery = useQuery({
    queryKey: ["project-members", task?.projectId],
    queryFn: () => projectService.getProjectMembers(task!.projectId),
    enabled: !!task?.projectId,
  });

  // Mutations
  const updateTaskMutation = useUpdateTask();
  const updateAssigneeMutation = useUpdateTaskAssignee();
  const addCommentMutation = useAddComment();

  const deleteTaskMutation = useMutation({
    mutationFn: (taskId: number) => taskService.deleteTask(taskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      queryClient.invalidateQueries({ queryKey: ["board"] });
      queryClient.invalidateQueries({ queryKey: ["backlog"] });
      toast({
        title: "Success",
        description: "Task deleted permanently",
      });
      onClose?.();
    },
    onError: (error) => {
      toast({
        title: "Error",
        description: extractErrorMessage(error, "Failed to delete task"),
        variant: "destructive",
      });
    },
  });

  const archiveTaskMutation = useMutation({
    mutationFn: (taskId: number) => taskService.archiveTask(taskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      queryClient.invalidateQueries({ queryKey: ["board"] });
      queryClient.invalidateQueries({ queryKey: ["backlog"] });
      toast({
        title: "Success",
        description: "Task archived successfully",
      });
      onClose?.();
    },
    onError: (error) => {
      toast({
        title: "Error",
        description: extractErrorMessage(error, "Failed to archive task"),
        variant: "destructive",
      });
    },
  });

  const unarchiveTaskMutation = useMutation({
    mutationFn: (taskId: number) => taskService.unarchiveTask(taskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      queryClient.invalidateQueries({ queryKey: ["board"] });
      queryClient.invalidateQueries({ queryKey: ["backlog"] });
      toast({
        title: "Success",
        description: "Task unarchived successfully",
      });
      onClose?.();
    },
    onError: (error) => {
      toast({
        title: "Error",
        description: extractErrorMessage(error, "Failed to unarchive task"),
        variant: "destructive",
      });
    },
  });

  return {
    // Data
    task: task as Task | undefined,
    comments: commentsQuery.data?.data || [],
    subtasks: subtasksQuery.data?.data || [],
    project: projectQuery.data?.data,
    members: (membersQuery.data?.data || []) as ProjectMember[],
    
    // Loading states
    isLoading: taskQuery.isLoading,
    isLoadingComments: commentsQuery.isLoading,
    isLoadingSubtasks: subtasksQuery.isLoading,
    
    // Computed values
    isProjectArchived: projectQuery.data?.data?.status === "ARCHIVED",
    
    // Actions
    updateTask: updateTaskMutation.mutate,
    updateTaskAsync: updateTaskMutation.mutateAsync,
    updateAssignee: updateAssigneeMutation.mutate,
    updateAssigneeAsync: updateAssigneeMutation.mutateAsync,
    addComment: addCommentMutation.mutate,
    deleteTask: deleteTaskMutation.mutate,
    archiveTask: archiveTaskMutation.mutate,
    unarchiveTask: unarchiveTaskMutation.mutate,
    
    // Mutation states
    isUpdating: updateTaskMutation.isPending,
    isDeleting: deleteTaskMutation.isPending,
    isArchiving: archiveTaskMutation.isPending,
    
    // Query reference for error handling
    taskQuery,
  };
}
