/**
 * Complete Organization Service
 * File: src/services/organization.service.ts
 */

import api from './api';

export interface Organization {
  id: number;
  name: string;
  address?: string;
  contactEmail?: string;
  createdAt: string;
  userCount?: number;
  inviteCode?: string;
  inviteCodeExpiresAt?: string;
}

export interface CreateOrganizationRequest {
  name: string;
  address?: string;
  contactEmail?: string;
}

export interface UpdateOrganizationRequest {
  name: string;
  address?: string;
  contactEmail?: string;
}

export interface JoinOrganizationRequest {
  inviteCode?: string;
}

export interface UserOrganizationResponse {
  organizationId: number | null;
  organizationName: string | null;
  hasOrganization: boolean;
  roles?: string[];
  userId?: number;
}

export interface OrganizationMember {
  userId: number;
  name: string;
  email: string;
  role: string;
  joinedAt: string;
  status: string;
}

export interface PagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export const organizationService = {
  // ========== EXISTING ENDPOINTS ==========

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

  // ========== NEW ENDPOINTS (YOU NEED TO IMPLEMENT THESE IN BACKEND) ==========

  // Create organization
  createOrganization: async (data: CreateOrganizationRequest) => {
    const response = await api.post('/organizations', data);
    return response.data;
  },

  // Check if current user has an organization
  checkUserOrganization: async (): Promise<{
    success: boolean;
    data: UserOrganizationResponse
  }> => {
    const response = await api.get('/users/me/organization');
    return response.data;
  },

  // Get all organizations for current user
  getMyOrganizations: async (): Promise<{
    success: boolean;
    data: UserOrganizationResponse[];
  }> => {
    const response = await api.get('/users/me/organizations');
    return response.data;
  },

  // Join organization with invite code
  joinOrganization: async (data: JoinOrganizationRequest) => {
    const response = await api.post('/organizations/join', data);
    return response.data;
  },

  // Request to join organization via email
  requestJoinOrganization: async (organizationEmail: string) => {
    const response = await api.post('/organizations/request-join', {
      organizationEmail,
    });
    return response.data;
  },

  // ========== ADMIN FEATURES (OPTIONAL - FOR LATER) ==========

  // Generate invite code (admin only)
  generateInviteCode: async (orgId: number) => {
    const response = await api.post(`/organizations/${orgId}/invite-code`);
    return response.data;
  },

  // Get pending join requests (admin only)
  getPendingRequests: async (orgId: number) => {
    const response = await api.get(`/organizations/${orgId}/pending-requests`);
    return response.data;
  },

  // Approve join request (admin only)
  approveJoinRequest: async (orgId: number, userId: number) => {
    const response = await api.post(`/organizations/${orgId}/approve-request/${userId}`);
    return response.data;
  },

  // Reject join request (admin only)
  rejectJoinRequest: async (orgId: number, userId: number) => {
    const response = await api.post(`/organizations/${orgId}/reject-request/${userId}`);
    return response.data;
  },

  // Get organization members with pagination
  getOrganizationMembers: async (orgId: number, page = 0, size = 10): Promise<{
    success: boolean;
    data: PagedResponse<OrganizationMember>;
  }> => {
    const response = await api.get(`/organizations/${orgId}/members`, {
      params: { page, size }
    });
    return response.data;
  },
};