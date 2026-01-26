import api from "./api";
import type { ApiResponse } from "@/types/auth.types";
import type {
  Project,
  ProjectDetails,
  ProjectMember,
  CreateProjectDto,
  UpdateProjectDto,
  PaginatedResponse,
  ProjectFilter,
} from "@/types/project.types";

export const projectService = {
  // Get all projects for the current user
  async getProjects(filter?: ProjectFilter): Promise<ApiResponse<PaginatedResponse<Project>>> {
    const response = await api.get<ApiResponse<PaginatedResponse<Project>>>("/projects", {
      params: filter
    });
    return response.data;
  },

  // Get project details by ID
  async getProjectById(projectId: number | string): Promise<ApiResponse<ProjectDetails>> {
    const response = await api.get<ApiResponse<ProjectDetails>>(`/projects/${projectId}`);
    return response.data;
  },

  // Create a new project
  async createProject(projectData: CreateProjectDto): Promise<ApiResponse<Project>> {
    const response = await api.post<ApiResponse<Project>>("/projects", projectData);
    return response.data;
  },

  // Update an existing project
  async updateProject(projectId: number | string, projectData: UpdateProjectDto): Promise<ApiResponse<Project>> {
    const response = await api.put<ApiResponse<Project>>(`/projects/${projectId}`, projectData);
    return response.data;
  },

  // Delete a project (permanently)
  async deleteProject(projectId: number | string): Promise<ApiResponse<unknown>> {
    const response = await api.delete<ApiResponse<unknown>>(`/projects/${projectId}`);
    return response.data;
  },

  // Archive a project
  async archiveProject(projectId: number | string): Promise<ApiResponse<Project>> {
    const response = await api.put<ApiResponse<Project>>(`/projects/${projectId}/archive`);
    return response.data;
  },

  // Unarchive a project
  async unarchiveProject(projectId: number | string): Promise<ApiResponse<Project>> {
    const response = await api.put<ApiResponse<Project>>(`/projects/${projectId}/unarchive`);
    return response.data;
  },

  // Get project members
  async getProjectMembers(projectId: number | string): Promise<ApiResponse<ProjectMember[]>> {
    const response = await api.get<ApiResponse<ProjectMember[]>>(`/projects/${projectId}/members`);
    return response.data;
  },

  // Add project member
  async addProjectMember(
    projectId: number | string,
    memberData: { email: string; role: string }
  ): Promise<ApiResponse<ProjectMember>> {
    const response = await api.post<ApiResponse<ProjectMember>>(`/projects/${projectId}/members`, memberData);
    return response.data;
  },

  // Remove project member
  async removeProjectMember(projectId: number | string, userId: number | string): Promise<ApiResponse<unknown>> {
    const response = await api.delete<ApiResponse<unknown>>(`/projects/${projectId}/members/${userId}`);
    return response.data;
  },

  // Update member role
  async updateMemberRole(
    projectId: number | string,
    userId: number | string,
    role: string
  ): Promise<ApiResponse<ProjectMember>> {
    const response = await api.put<ApiResponse<ProjectMember>>(`/projects/${projectId}/members/${userId}/role`, { role });
    return response.data;
  },

  // Assign team to project
  async assignTeamToProject(projectId: number | string, teamId: number | string): Promise<ApiResponse<Project>> {
    const response = await api.put<ApiResponse<Project>>(`/projects/${projectId}/team/${teamId}`);
    return response.data;
  },
};
