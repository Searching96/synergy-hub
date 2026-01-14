import api from "./api";

export interface Team {
  id: number;
  organizationId: number;
  name: string;
  description?: string;
  memberIds: number[];
  memberCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTeamRequest {
  name: string;
  description?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

const getOrgId = (): number => {
  const orgId = localStorage.getItem("organizationId");
  if (!orgId) throw new Error("Organization ID not found");
  return parseInt(orgId, 10);
};

export const teamService = {
  async createTeam(request: CreateTeamRequest): Promise<Team> {
    const orgId = getOrgId();
    const response = await api.post<ApiResponse<Team>>(
      `/organizations/${orgId}/teams`,
      request
    );
    return response.data.data;
  },

  async getOrganizationTeams(): Promise<Team[]> {
    const orgId = getOrgId();
    const response = await api.get<ApiResponse<Team[]>>(
      `/organizations/${orgId}/teams`
    );
    return response.data.data;
  },

  async getTeam(teamId: number): Promise<Team> {
    const orgId = getOrgId();
    const response = await api.get<ApiResponse<Team>>(
      `/organizations/${orgId}/teams/${teamId}`
    );
    return response.data.data;
  },

  async deleteTeam(teamId: number): Promise<void> {
    const orgId = getOrgId();
    await api.delete(`/organizations/${orgId}/teams/${teamId}`);
  },

  async addMember(teamId: number, userId: number): Promise<Team> {
    const orgId = getOrgId();
    const response = await api.post<ApiResponse<Team>>(
      `/organizations/${orgId}/teams/${teamId}/members/${userId}`
    );
    return response.data.data;
  },

  async removeMember(teamId: number, userId: number): Promise<Team> {
    const orgId = getOrgId();
    const response = await api.delete<ApiResponse<Team>>(
      `/organizations/${orgId}/teams/${teamId}/members/${userId}`
    );
    return response.data.data;
  },
};
