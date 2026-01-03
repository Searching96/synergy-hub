/**
 * RBAC Service
 * Handles all API calls to role and permission endpoints
 */

import api from "./api";
import type {
  Role,
  Permission,
  CreateRoleRequest,
  UpdateRoleRequest,
  AssignPermissionsRequest,
  ApiResponse,
} from "@/types/rbac.types";

// Get organization ID from current context (from auth or localStorage)
const getOrgId = (): number => {
  // This would typically come from your auth context
  // For now, assuming it's stored or available from user context
  const orgId = localStorage.getItem("organizationId");
  if (!orgId) throw new Error("Organization ID not found");
  return parseInt(orgId, 10);
};

export const rbacService = {
  /**
   * Fetch all roles for an organization
   */
  getRoles: async (): Promise<Role[]> => {
    const orgId = getOrgId();
    const response = await api.get<ApiResponse<Role[]>>(
      `/organizations/${orgId}/roles`
    );
    return response.data.data;
  },

  /**
   * Fetch a single role with its permissions
   */
  getRole: async (roleId: number): Promise<Role> => {
    const response = await api.get<ApiResponse<Role>>(
      `/roles/${roleId}`
    );
    return response.data.data;
  },

  /**
   * Fetch all available permissions (system-wide)
   */
  getPermissions: async (): Promise<Permission[]> => {
    const response = await api.get<ApiResponse<Permission[]>>(
      `/permissions`
    );
    return response.data.data;
  },

  /**
   * Create a new role with initial permissions
   */
  createRole: async (request: CreateRoleRequest): Promise<Role> => {
    const orgId = getOrgId();
    const response = await api.post<ApiResponse<Role>>(
      `/organizations/${orgId}/roles`,
      request
    );
    return response.data.data;
  },

  /**
   * Update role metadata and permissions
   */
  updateRole: async (
    roleId: number,
    request: UpdateRoleRequest
  ): Promise<Role> => {
    const orgId = getOrgId();
    const response = await api.put<ApiResponse<Role>>(
      `/organizations/${orgId}/roles/${roleId}`,
      request
    );
    return response.data.data;
  },

  /**
   * Assign permissions to a role (replaces all permissions)
   * This is the preferred method for toggling individual permissions
   */
  assignPermissionsToRole: async (
    roleId: number,
    request: AssignPermissionsRequest
  ): Promise<Role> => {
    const orgId = getOrgId();
    const response = await api.post<ApiResponse<Role>>(
      `/organizations/${orgId}/roles/${roleId}/permissions`,
      request
    );
    return response.data.data;
  },

  /**
   * Delete a role
   * Note: Cannot delete system roles (will return 400 error)
   */
  deleteRole: async (roleId: number): Promise<void> => {
    const orgId = getOrgId();
    await api.delete(
      `/organizations/${orgId}/roles/${roleId}`
    );
  },
};
