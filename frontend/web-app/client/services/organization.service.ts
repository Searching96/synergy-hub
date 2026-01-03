import api from './api';

export interface Organization {
  id: number;
  name: string;
  address?: string;
  contactEmail?: string;
  createdAt: string;
}

export interface UpdateOrganizationRequest {
  name: string;
  address?: string;
  contactEmail?: string;
}

export const organizationService = {
  // Get organization by ID
  getOrganization: async (orgId: number) => {
    const response = await api.get(`/organizations/${orgId}`);
    return response.data;
  },

  // Update organization
  updateOrganization: async (orgId: number, data: UpdateOrganizationRequest) => {
    const response = await api.put(`/organizations/${orgId}`, data);
    return response.data;
  },

  // Delete organization
  deleteOrganization: async (orgId: number) => {
    const response = await api.delete(`/organizations/${orgId}`);
    return response.data;
  },
};
