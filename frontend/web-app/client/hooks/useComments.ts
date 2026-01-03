import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { commentService } from "@/services/comment.service";
import { useToast } from "@/hooks/use-toast";
import type { Comment } from "@/types/comment.types";
import type { ApiResponse } from "@/types/auth.types";

export function useTaskComments(taskId: number | null) {
  return useQuery<ApiResponse<Comment[]>>({
    queryKey: ["comments", taskId],
    queryFn: () => commentService.getTaskComments(taskId!),
    enabled: !!taskId,
  });
}

export function useAddComment() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: ({ taskId, content }: { taskId: number; content: string }) =>
      commentService.addComment(taskId, content),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["comments", variables.taskId] });
      queryClient.invalidateQueries({ queryKey: ["task", variables.taskId] });
      toast({
        title: "Success",
        description: "Comment added successfully",
      });
    },
    onError: (error: any) => {
      toast({
        title: "Error",
        description: error?.response?.data?.message || "Failed to add comment",
        variant: "destructive",
      });
    },
  });
}
