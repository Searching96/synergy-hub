/**
 * Sprint Service (TypeScript)
 * Uses shared api client with JWT interceptors
 */

import api from "./api";
import type { ApiResponse } from "@/types/auth.types";
import type {
  Sprint,
  SprintDetails,
  CreateSprintRequest,
  UpdateSprintRequest,
} from "@/types/sprint.types";

export const sprintService = {
  // Get all sprints for a project
  async getProjectSprints(projectId: string | number, status?: string): Promise<ApiResponse<Sprint[]>> {
    const params = status ? { status } : {};
    const response = await api.get<ApiResponse<Sprint[]>>(`/projects/${projectId}/sprints`, { params });
    return response.data;
  },

  // Get sprint by ID
  async getSprintById(sprintId: string | number): Promise<ApiResponse<Sprint>> {
    const response = await api.get<ApiResponse<Sprint>>(`/sprints/${sprintId}`);
    return response.data;
  },

  // Get sprint details with tasks and burndown
  async getSprintDetails(sprintId: string | number): Promise<ApiResponse<SprintDetails>> {
    const response = await api.get<ApiResponse<SprintDetails>>(`/sprints/${sprintId}/details`);
    return response.data;
  },

  // Create a new sprint
  async createSprint(sprintData: CreateSprintRequest): Promise<ApiResponse<Sprint>> {
    const response = await api.post<ApiResponse<Sprint>>("/sprints", sprintData);
    return response.data;
  },

  // Update sprint
  async updateSprint(sprintId: string | number, sprintData: UpdateSprintRequest): Promise<ApiResponse<Sprint>> {
    const response = await api.put<ApiResponse<Sprint>>(`/sprints/${sprintId}`, sprintData);
    return response.data;
  },

  // Start sprint
  async startSprint(sprintId: string | number): Promise<ApiResponse<Sprint>> {
    const response = await api.post<ApiResponse<Sprint>>(`/sprints/${sprintId}/start`);
    return response.data;
  },

  // Complete sprint
  async completeSprint(sprintId: string | number): Promise<ApiResponse<Sprint>> {
    const response = await api.post<ApiResponse<Sprint>>(`/sprints/${sprintId}/complete`);
    return response.data;
  },

  // Cancel sprint
  async cancelSprint(sprintId: string | number): Promise<ApiResponse<Sprint>> {
    const response = await api.post<ApiResponse<Sprint>>(`/sprints/${sprintId}/cancel`);
    return response.data;
  },

  // Get active sprint for project
  async getActiveSprint(projectId: string | number): Promise<ApiResponse<Sprint[]>> {
    const response = await api.get<ApiResponse<Sprint[]>>(`/projects/${projectId}/sprints`, {
      params: { status: 'ACTIVE' }
    });
    return response.data;
  },

  // Get completed sprints for project
  async getCompletedSprints(projectId: string | number): Promise<ApiResponse<Sprint[]>> {
    const response = await api.get<ApiResponse<Sprint[]>>(`/projects/${projectId}/sprints`, {
      params: { status: 'COMPLETED' }
    });
    return response.data;
  },

  // Delete sprint
  async deleteSprint(sprintId: string | number): Promise<void> {
    await api.delete(`/sprints/${sprintId}`);
  },
};
