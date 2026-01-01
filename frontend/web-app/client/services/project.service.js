import api from './api';

export const projectService = {
  // Get all projects for the current user
  getProjects: async () => {
    const response = await api.get('/projects');
    return response.data;
  },

  // Get project details by ID
  getProjectById: async (projectId) => {
    const response = await api.get(`/projects/${projectId}`);
    return response.data;
  },

  // Create a new project
  createProject: async (projectData) => {
    const response = await api.post('/projects', projectData);
    return response.data;
  },

  // Update an existing project
  updateProject: async (projectId, projectData) => {
    const response = await api.put(`/projects/${projectId}`, projectData);
    return response.data;
  },

  // Delete a project (permanently)
  deleteProject: async (projectId) => {
    const response = await api.delete(`/projects/${projectId}`);
    return response.data;
  },

  // Archive a project
  archiveProject: async (projectId) => {
    const response = await api.put(`/projects/${projectId}/archive`);
    return response.data;
  },

  // Unarchive a project
  unarchiveProject: async (projectId) => {
    const response = await api.put(`/projects/${projectId}/unarchive`);
    return response.data;
  },

  // Get project members
  getProjectMembers: async (projectId) => {
    const response = await api.get(`/projects/${projectId}/members`);
    return response.data;
  },

  // Add project member
  addProjectMember: async (projectId, memberData) => {
    const response = await api.post(`/projects/${projectId}/members`, memberData);
    return response.data;
  },

  // Remove project member
  removeProjectMember: async (projectId, userId) => {
    const response = await api.delete(`/projects/${projectId}/members/${userId}`);
    return response.data;
  },

  // Update member role
  updateMemberRole: async (projectId, userId, role) => {
    const response = await api.put(`/projects/${projectId}/members/${userId}/role`, { role });
    return response.data;
  },
};
