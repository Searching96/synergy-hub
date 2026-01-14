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

// Get organization ID from current user context
const getOrgId = (): number => {
  try {
    const userStr = localStorage.getItem("user");
    if (!userStr) throw new Error("User not found in localStorage");
    const user = JSON.parse(userStr);
    if (!user.organizationId) throw new Error("Organization ID not found in user object");
    return user.organizationId;
  } catch (error) {
    throw new Error(`Failed to get organization ID: ${error}`);
  }
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
