/**
 * useRBAC Hook
 * Manages RBAC queries and mutations with TanStack Query
 * Handles optimistic updates and error handling (including 403 Forbidden)
 */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback } from "react";
import { rbacService } from "@/services/rbac.service";
import type {
  Role,
  Permission,
  CreateRoleRequest,
  AssignPermissionsRequest,
} from "@/types/rbac.types";
import { toast } from "sonner";
import axios from "axios";

/**
 * Query key factory for TanStack Query
 */
const rbacKeys = {
  all: ["rbac"] as const,
  roles: ["rbac", "roles"] as const,
  role: (roleId: number) => ["rbac", "roles", roleId] as const,
  permissions: ["rbac", "permissions"] as const,
};

/**
 * Main RBAC hook
 */
export const useRBAC = () => {
  const queryClient = useQueryClient();

  /**
   * Query: Fetch all roles and permissions in parallel
   */
  const rolesQuery = useQuery({
    queryKey: rbacKeys.roles,
    queryFn: rbacService.getRoles,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  const permissionsQuery = useQuery({
    queryKey: rbacKeys.permissions,
    queryFn: rbacService.getPermissions,
    staleTime: 10 * 60 * 1000, // 10 minutes
  });

  /**
   * Mutation: Assign/Revoke permissions to a role
   * Optimistic update: Update UI immediately, revert on error
   */
  const permissionsMutation = useMutation({
    mutationFn: ({
      roleId,
      permissionIds,
    }: {
      roleId: number;
      permissionIds: number[];
    }) =>
      rbacService.assignPermissionsToRole(roleId, {
        permissionIds,
      }),

    onMutate: async ({ roleId, permissionIds }) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: rbacKeys.roles });

      // Snapshot previous state for rollback
      const previousRoles = queryClient.getQueryData<Role[]>(rbacKeys.roles);

      // Optimistic update
      queryClient.setQueryData<Role[]>(rbacKeys.roles, (oldRoles) => {
        if (!oldRoles) return oldRoles;
        return oldRoles.map((role) => {
          if (role.id === roleId) {
            const allPermissions = queryClient.getQueryData<Permission[]>(
              rbacKeys.permissions
            ) || [];
            return {
              ...role,
              permissions: allPermissions.filter((p) =>
                permissionIds.includes(p.id)
              ),
            };
          }
          return role;
        });
      });

      return { previousRoles };
    },

    onError: (error, _variables, context) => {
      // Rollback on error
      if (context?.previousRoles) {
        queryClient.setQueryData(rbacKeys.roles, context.previousRoles);
      }

      // Handle 403 Forbidden - RBAC error
      if (axios.isAxiosError(error) && error.response?.status === 403) {
        toast.error(
          "Access Denied: You don't have permission to modify roles."
        );
        return;
      }

      // Handle system role modification attempt
      if (
        error instanceof Error &&
        error.message.includes("system role")
      ) {
        toast.error("Cannot modify system roles. They are protected.");
        return;
      }

      toast.error("Failed to update permissions. Please try again.");
    },

    onSuccess: () => {
      toast.success("Permissions updated successfully!");
      queryClient.invalidateQueries({ queryKey: rbacKeys.roles });
    },
  });

  /**
   * Mutation: Create a new role
   */
  const createRoleMutation = useMutation({
    mutationFn: (request: CreateRoleRequest) => rbacService.createRole(request),

    onMutate: async (newRole) => {
      await queryClient.cancelQueries({ queryKey: rbacKeys.roles });
      const previousRoles = queryClient.getQueryData<Role[]>(rbacKeys.roles);

      // Optimistic add with temporary ID
      queryClient.setQueryData<Role[]>(rbacKeys.roles, (oldRoles) => {
        if (!oldRoles) return oldRoles;
        return [
          ...oldRoles,
          {
            id: -1, // Temporary ID
            ...newRole,
            isSystemRole: false,
            permissions: [],
          } as Role,
        ];
      });

      return { previousRoles };
    },

    onError: (_error, _variables, context) => {
      if (context?.previousRoles) {
        queryClient.setQueryData(rbacKeys.roles, context.previousRoles);
      }
      toast.error("Failed to create role. Please try again.");
    },

    onSuccess: () => {
      toast.success("Role created successfully!");
      queryClient.invalidateQueries({ queryKey: rbacKeys.roles });
    },
  });

  /**
   * Mutation: Delete a role
   */
  const deleteRoleMutation = useMutation({
    mutationFn: (roleId: number) => rbacService.deleteRole(roleId),

    onMutate: async (roleId) => {
      await queryClient.cancelQueries({ queryKey: rbacKeys.roles });
      const previousRoles = queryClient.getQueryData<Role[]>(rbacKeys.roles);

      // Optimistic delete
      queryClient.setQueryData<Role[]>(rbacKeys.roles, (oldRoles) => {
        if (!oldRoles) return oldRoles;
        return oldRoles.filter((role) => role.id !== roleId);
      });

      return { previousRoles };
    },

    onError: (_error, _variables, context) => {
      if (context?.previousRoles) {
        queryClient.setQueryData(rbacKeys.roles, context.previousRoles);
      }
      toast.error("Failed to delete role. Please try again.");
    },

    onSuccess: () => {
      toast.success("Role deleted successfully!");
      queryClient.invalidateQueries({ queryKey: rbacKeys.roles });
    },
  });

  /**
   * Helper: Toggle a single permission on/off
   * Finds current permissions and toggles the given permissionId
   */
  const togglePermission = useCallback(
    (roleId: number, permissionId: number) => {
      const currentRoles = queryClient.getQueryData<Role[]>(rbacKeys.roles);
      const currentRole = currentRoles?.find((r) => r.id === roleId);

      if (!currentRole) return;

      const currentPermissionIds = currentRole.permissions.map((p) => p.id);
      const newPermissionIds = currentPermissionIds.includes(permissionId)
        ? currentPermissionIds.filter((id) => id !== permissionId)
        : [...currentPermissionIds, permissionId];

      permissionsMutation.mutate({ roleId, permissionIds: newPermissionIds });
    },
    [queryClient, permissionsMutation]
  );

  /**
   * Helper: Check if a role has a specific permission
   */
  const hasPermission = useCallback(
    (roleId: number, permissionId: number): boolean => {
      const roles = queryClient.getQueryData<Role[]>(rbacKeys.roles);
      const role = roles?.find((r) => r.id === roleId);
      return role?.permissions.some((p) => p.id === permissionId) ?? false;
    },
    [queryClient]
  );

  /**
   * Helper: Group permissions by category for display
   */
  const groupPermissionsByCategory = useCallback(
    (permissions: Permission[]) => {
      const grouped = permissions.reduce(
        (acc, permission) => {
          const category = permission.category || "Other";
          if (!acc[category]) {
            acc[category] = [];
          }
          acc[category].push(permission);
          return acc;
        },
        {} as Record<string, Permission[]>
      );

      return Object.entries(grouped).sort(([a], [b]) => a.localeCompare(b));
    },
    []
  );

  return {
    // Queries
    roles: rolesQuery.data ?? [],
    permissions: permissionsQuery.data ?? [],
    isLoadingRoles: rolesQuery.isLoading,
    isLoadingPermissions: permissionsQuery.isLoading,
    isLoading: rolesQuery.isLoading || permissionsQuery.isLoading,
    error: rolesQuery.error || permissionsQuery.error,

    // Mutations
    updatePermissions: permissionsMutation.mutate,
    createRole: createRoleMutation.mutate,
    deleteRole: deleteRoleMutation.mutate,
    isUpdatingPermissions: permissionsMutation.isPending,
    isCreatingRole: createRoleMutation.isPending,
    isDeletingRole: deleteRoleMutation.isPending,

    // Helpers
    togglePermission,
    hasPermission,
    groupPermissionsByCategory,
  };
};
