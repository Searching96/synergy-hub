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

  // Update an existing comment
  async updateComment(taskId: number | string, commentId: number, content: string): Promise<ApiResponse<Comment>> {
    const response = await api.put<ApiResponse<Comment>>(`/tasks/${taskId}/comments/${commentId}`, { content });
    return response.data;
  },

  // Delete a comment
  async deleteComment(taskId: number | string, commentId: number): Promise<ApiResponse<void>> {
    const response = await api.delete<ApiResponse<void>>(`/tasks/${taskId}/comments/${commentId}`);
    return response.data;
  },
};
