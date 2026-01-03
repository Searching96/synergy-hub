/**
 * Sprint Service (TypeScript)
 * Uses shared api client with JWT interceptors
 */

import api from "./api";
import type {
  Sprint,
  SprintDetails,
  CreateSprintRequest,
  UpdateSprintRequest,
} from "@/types/sprint.types";

export const sprintService = {
  // Get all sprints for a project
  async getProjectSprints(projectId: string | number): Promise<Sprint[]> {
    const response = await api.get<Sprint[]>(`/projects/${projectId}/sprints`);
    return response.data;
  },

  // Get sprint by ID
  async getSprintById(sprintId: string | number): Promise<Sprint> {
    const response = await api.get<Sprint>(`/sprints/${sprintId}`);
    return response.data;
  },

  // Get sprint details with tasks and burndown
  async getSprintDetails(sprintId: string | number): Promise<SprintDetails> {
    const response = await api.get<SprintDetails>(`/sprints/${sprintId}/details`);
    return response.data;
  },

  // Create a new sprint
  async createSprint(sprintData: CreateSprintRequest): Promise<Sprint> {
    const response = await api.post<Sprint>("/sprints", sprintData);
    return response.data;
  },

  // Update sprint
  async updateSprint(sprintId: string | number, sprintData: UpdateSprintRequest): Promise<Sprint> {
    const response = await api.put<Sprint>(`/sprints/${sprintId}`, sprintData);
    return response.data;
  },

  // Start sprint
  async startSprint(sprintId: string | number): Promise<Sprint> {
    const response = await api.post<Sprint>(`/sprints/${sprintId}/start`);
    return response.data;
  },

  // Complete sprint
  async completeSprint(sprintId: string | number): Promise<Sprint> {
    const response = await api.post<Sprint>(`/sprints/${sprintId}/complete`);
    return response.data;
  },

  // Cancel sprint
  async cancelSprint(sprintId: string | number): Promise<Sprint> {
    const response = await api.post<Sprint>(`/sprints/${sprintId}/cancel`);
    return response.data;
  },

  // Get active sprint for project
  async getActiveSprint(projectId: string | number): Promise<Sprint | null> {
    const response = await api.get<Sprint | null>(`/sprints/projects/${projectId}/active`);
    return response.data;
  },

  // Get completed sprints for project
  async getCompletedSprints(projectId: string | number): Promise<Sprint[]> {
    const response = await api.get<Sprint[]>(`/sprints/projects/${projectId}/completed`);
    return response.data;
  },

  // Delete sprint
  async deleteSprint(sprintId: string | number): Promise<void> {
    await api.delete(`/sprints/${sprintId}`);
  },
};
