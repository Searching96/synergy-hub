import api from "./api";
import type { ApiResponse } from "@/types/auth.types";
import type { Comment } from "@/types/comment.types";

export const commentService = {
  // Get comments for a task
  async getTaskComments(
    taskId: number | string,
    page = 0,
    size = 50
  ): Promise<ApiResponse<Comment[]>> {
    const response = await api.get<ApiResponse<Comment[]>>(`/tasks/${taskId}/comments`, {
      params: { page, size },
    });
    return response.data;
  },

  // Add a new comment to a task
  async addComment(taskId: number | string, content: string): Promise<ApiResponse<Comment>> {
    const response = await api.post<ApiResponse<Comment>>(`/tasks/${taskId}/comments`, { content });
    return response.data;
  },
};
