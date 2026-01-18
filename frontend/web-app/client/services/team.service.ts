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

export interface Result<T, E = Error> {
  success: boolean;
  data?: T;
  error?: E;
}

const getOrgId = (): number => {
  const orgId = localStorage.getItem("organizationId");
  if (orgId) return parseInt(orgId, 10);

  // Fallback: Try to get from auth storage
  try {
    const userStr = localStorage.getItem("user");
    if (userStr) {
      const user = JSON.parse(userStr);
      if (user.organizationId) {
        localStorage.setItem("organizationId", String(user.organizationId));
        return user.organizationId;
      }
    }
  } catch (e) {
    console.error("Error parsing user data:", e);
  }

  // Instead of throwing synchronously, return a sentinel value that will cause
  // API calls to fail gracefully and allow UI to handle it
  return -1;
};

const getOrgIdAsync = async (): Promise<Result<number>> => {
  const orgId = getOrgId();
  if (orgId !== -1) {
    return { success: true, data: orgId };
  }

  // If we can't get orgId, this is a critical error that should be handled by UI
  return {
    success: false,
    error: new Error("Organization context is missing. Please try logging in again or selecting an organization.")
  };
};

export const teamService = {
  async createTeam(request: CreateTeamRequest): Promise<Team> {
    const orgIdResult = await getOrgIdAsync();
    if (!orgIdResult.success) {
      throw orgIdResult.error;
    }

    const response = await api.post<ApiResponse<Team>>(
      `/organizations/${orgIdResult.data}/teams`,
      request
    );
    return response.data.data;
  },

  async getOrganizationTeams(): Promise<Team[]> {
    const orgIdResult = await getOrgIdAsync();
    if (!orgIdResult.success) {
      throw orgIdResult.error;
    }

    const response = await api.get<ApiResponse<Team[]>>(
      `/organizations/${orgIdResult.data}/teams`
    );
    return response.data.data;
  },

  async getTeam(teamId: number): Promise<Team> {
    const orgIdResult = await getOrgIdAsync();
    if (!orgIdResult.success) {
      throw orgIdResult.error;
    }

    const response = await api.get<ApiResponse<Team>>(
      `/organizations/${orgIdResult.data}/teams/${teamId}`
    );
    return response.data.data;
  },

  async deleteTeam(teamId: number): Promise<void> {
    const orgIdResult = await getOrgIdAsync();
    if (!orgIdResult.success) {
      throw orgIdResult.error;
    }

    await api.delete(`/organizations/${orgIdResult.data}/teams/${teamId}`);
  },

  async addMember(teamId: number, userId: number): Promise<Team> {
    const orgIdResult = await getOrgIdAsync();
    if (!orgIdResult.success) {
      throw orgIdResult.error;
    }

    const response = await api.post<ApiResponse<Team>>(
      `/organizations/${orgIdResult.data}/teams/${teamId}/members/${userId}`
    );
    return response.data.data;
  },

  async removeMember(teamId: number, userId: number): Promise<Team> {
    const orgIdResult = await getOrgIdAsync();
    if (!orgIdResult.success) {
      throw orgIdResult.error;
    }

    const response = await api.delete<ApiResponse<Team>>(
      `/organizations/${orgIdResult.data}/teams/${teamId}/members/${userId}`
    );
    return response.data.data;
  },
};
