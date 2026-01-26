/**
 * SSO Service
 * Handles all API calls to SSO provider endpoints
 */

import api from "./api";
import type {
  SsoProviderResponse,
  RegisterSsoProviderRequest,
  UpdateSsoProviderRequest,
  ApiResponse,
} from "@/types/sso.types";

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

  return -1;
};

const getOrgIdAsync = async (): Promise<Result<number>> => {
  const orgId = getOrgId();
  if (orgId !== -1) {
    return { success: true, data: orgId };
  }

  return {
    success: false,
    error: new Error("Organization ID not found. Please try logging in again or selecting an organization.")
  };
};

export const ssoService = {
  /**
   * Fetch all SSO providers for an organization
   */
  getSsoProviders: async (orgId?: number): Promise<SsoProviderResponse[]> => {
    const resolvedOrgId = orgId ?? getOrgId();
    const response = await api.get<ApiResponse<SsoProviderResponse[]>>(
      `/organizations/${resolvedOrgId}/sso/providers`
    );
    return response.data.data;
  },

  /**
   * Fetch a single SSO provider by ID
   */
  getSsoProvider: async (providerId: number): Promise<SsoProviderResponse> => {
    const orgId = getOrgId();
    const response = await api.get<ApiResponse<SsoProviderResponse>>(
      `/organizations/${orgId}/sso/providers/${providerId}`
    );
    return response.data.data;
  },

  /**
   * Register a new SSO provider
   */
  registerSsoProvider: async (
    request: RegisterSsoProviderRequest
  ): Promise<SsoProviderResponse> => {
    const orgId = getOrgId();
    const response = await api.post<ApiResponse<SsoProviderResponse>>(
      `/organizations/${orgId}/sso/providers`,
      request
    );
    return response.data.data;
  },

  /**
   * Update SSO provider configuration
   */
  updateSsoProvider: async (
    providerId: number,
    request: UpdateSsoProviderRequest
  ): Promise<SsoProviderResponse> => {
    const orgId = getOrgId();
    const response = await api.put<ApiResponse<SsoProviderResponse>>(
      `/organizations/${orgId}/sso/providers/${providerId}`,
      request
    );
    return response.data.data;
  },

  /**
   * Enable SSO provider
   */
  enableSsoProvider: async (providerId: number): Promise<SsoProviderResponse> => {
    const orgId = getOrgId();
    const response = await api.put<ApiResponse<SsoProviderResponse>>(
      `/organizations/${orgId}/sso/providers/${providerId}/enable`
    );
    return response.data.data;
  },

  /**
   * Disable SSO provider
   */
  disableSsoProvider: async (
    providerId: number
  ): Promise<SsoProviderResponse> => {
    const orgId = getOrgId();
    const response = await api.put<ApiResponse<SsoProviderResponse>>(
      `/organizations/${orgId}/sso/providers/${providerId}/disable`
    );
    return response.data.data;
  },

  /**
   * Delete SSO provider
   */
  deleteSsoProvider: async (providerId: number): Promise<void> => {
    const orgId = getOrgId();
    await api.delete(`/organizations/${orgId}/sso/providers/${providerId}`);
  },

  /**
   * Rotate SSO provider secret
   */
  rotateSsoSecret: async (providerId: number): Promise<SsoProviderResponse> => {
    const orgId = getOrgId();
    const response = await api.post<ApiResponse<SsoProviderResponse>>(
      `/organizations/${orgId}/sso/providers/${providerId}/rotate-secret`
    );
    return response.data.data;
  },
};
