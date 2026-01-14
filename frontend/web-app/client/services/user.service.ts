import api from "./api";

export const userService = {
  // Get tasks assigned to current user
  getMyIssues: async () => {
    const response = await api.get("/tasks/my-tasks");
    return response.data;
  },
  // Get projects for current user
  getMyProjects: async () => {
    const response = await api.get("/projects");
    return response.data;
  },
  // Note: Activity is project-scoped, not user-global
  // Use activityService.getProjectActivity(projectId) instead

  // Update user profile
  updateProfile: async (data: { name: string }) => {
    const response = await api.put("/users/me", data);
    return response.data;
  },

  // Change password
  changePassword: async (data: { currentPassword: string; newPassword: string }) => {
    const response = await api.put("/users/me/password", data);
    return response.data;
  },
};
