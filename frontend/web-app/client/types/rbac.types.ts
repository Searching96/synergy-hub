/**
 * RBAC Type Definitions
 * Matches backend DTOs exactly (RoleResponse, PermissionResponse, etc.)
 */

export interface Permission {
  id: number;
  name: string;
  description: string;
  category?: string; // Inferred from name for UI grouping
}

export interface Role {
  id: number;
  name: string;
  description: string;
  permissions: Permission[];
  isSystemRole: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateRoleRequest {
  name: string;
  description: string;
  permissionIds: number[];
}

export interface UpdateRoleRequest {
  name: string;
  description: string;
  permissionIds: number[];
}

export interface AssignPermissionsRequest {
  permissionIds: number[];
}

export interface RoleResponse {
  id: number;
  name: string;
  description: string;
  permissions: Permission[];
  isSystemRole: boolean;
}

export interface PermissionResponse {
  id: number;
  name: string;
  description: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}
