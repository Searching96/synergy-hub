import api from './api';

export const commentService = {
  // Get comments for a task
  getTaskComments: async (taskId, page = 0, size = 50) => {
    const response = await api.get(`/tasks/${taskId}/comments`, {
      params: { page, size }
    });
    return response.data;
  },

  // Add a new comment to a task
  addComment: async (taskId, content) => {
    const response = await api.post(`/tasks/${taskId}/comments`, { content });
    return response.data;
  },
};
