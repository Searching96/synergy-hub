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

const getOrgId = (): number => {
  const orgId = localStorage.getItem("organizationId");
  if (!orgId) throw new Error("Organization ID not found");
  return parseInt(orgId, 10);
};

export const ssoService = {
  /**
   * Fetch all SSO providers for an organization
   */
  getSsoProviders: async (): Promise<SsoProviderResponse[]> => {
    const orgId = getOrgId();
    const response = await api.get<ApiResponse<SsoProviderResponse[]>>(
      `/organizations/${orgId}/sso/providers`
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
