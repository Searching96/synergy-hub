import api from './api';

export const taskService = {
  // Create a new task/issue
  createTask: async (taskData) => {
    const response = await api.post('/tasks', taskData);
    return response.data;
  },

  // Get task by ID
  getTaskById: async (taskId) => {
    const response = await api.get(`/tasks/${taskId}`);
    return response.data;
  },

  // Update task
  updateTask: async (taskId, taskData) => {
    const response = await api.put(`/tasks/${taskId}`, taskData);
    return response.data;
  },

  // Delete task (permanently)
  deleteTask: async (taskId) => {
    const response = await api.delete(`/tasks/${taskId}`);
    return response.data;
  },

  // Archive task
  archiveTask: async (taskId) => {
    const response = await api.put(`/tasks/${taskId}/archive`);
    return response.data;
  },

  // Unarchive task
  unarchiveTask: async (taskId) => {
    const response = await api.put(`/tasks/${taskId}/unarchive`);
    return response.data;
  },

  // Get project tasks
  getProjectTasks: async (projectId, params) => {
    const response = await api.get(`/projects/${projectId}/tasks`, { params });
    return response.data;
  },

  // Get project board (Kanban view)
  getProjectBoard: async (projectId) => {
    const response = await api.get(`/projects/${projectId}/board`);
    return response.data;
  },

  // Move task to sprint
  moveTaskToSprint: async (taskId, sprintId) => {
    const response = await api.post(`/tasks/${taskId}/move-to-sprint/${sprintId}`);
    return response.data;
  },

  // Update task assignee
  updateTaskAssignee: async (taskId, assigneeId) => {
    const response = await api.put(`/tasks/${taskId}/assignee`, { assigneeId });
    return response.data;
  },
};
