import api from './api';

export const sprintService = {
  // Get all sprints for a project
  getProjectSprints: async (projectId) => {
    const response = await api.get(`/projects/${projectId}/sprints`);
    return response.data;
  },

  // Get sprint by ID
  getSprintById: async (sprintId) => {
    const response = await api.get(`/sprints/${sprintId}`);
    return response.data;
  },

  // Create a new sprint
  createSprint: async (sprintData) => {
    const response = await api.post('/sprints', sprintData);
    return response.data;
  },

  // Update sprint
  updateSprint: async (sprintId, sprintData) => {
    const response = await api.put(`/sprints/${sprintId}`, sprintData);
    return response.data;
  },

  // Start sprint
  startSprint: async (sprintId) => {
    const response = await api.post(`/sprints/${sprintId}/start`);
    return response.data;
  },

  // Complete sprint
  completeSprint: async (sprintId) => {
    const response = await api.post(`/sprints/${sprintId}/complete`);
    return response.data;
  },

  // Cancel sprint
  cancelSprint: async (sprintId) => {
    const response = await api.post(`/sprints/${sprintId}/cancel`);
    return response.data;
  },

  // Get active sprint for project
  getActiveSprint: async (projectId) => {
    const response = await api.get(`/sprints/projects/${projectId}/active`);
    return response.data;
  },
};
