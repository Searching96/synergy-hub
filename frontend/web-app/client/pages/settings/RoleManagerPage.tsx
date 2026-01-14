/**
 * RoleManagerPage Component
 * Main page for Role & Permission Matrix management
 * Route: /settings/roles
 *
 * Layout:
 * - Left Sidebar: Role list with selection
 * - Right Panel: Permission matrix for selected role
 * - Header: Breadcrumbs and page title
 */

import { useState, useMemo, useEffect } from "react";
import { useAuth } from "@/context/AuthContext";
import { useRBAC } from "@/hooks/useRBAC";
import { RoleList } from "@/components/rbac/RoleList";
import { PermissionMatrix } from "@/components/rbac/PermissionMatrix";
import { toast } from "sonner";
import axios from "axios";

export function RoleManagerPage() {
  const { user } = useAuth();
  const canManageRoles = (user?.roles ?? []).some(
    (role) => role === "ORG_ADMIN" || role === "GLOBAL_ADMIN"
  );

  const {
    roles,
    permissions,
    isLoading,
    isCreatingRole,
    isDeletingRole,
    isUpdatingPermissions,
    createRole,
    deleteRole,
    updatePermissions,
    togglePermission,
    hasPermission,
    groupPermissionsByCategory,
  } = useRBAC();

  const [selectedRoleId, setSelectedRoleId] = useState<number | null>(null);
  const [isForbidden, setIsForbidden] = useState(false);

  // Find selected role
  const selectedRole = useMemo(
    () => roles.find((r) => r.id === selectedRoleId) || null,
    [roles, selectedRoleId]
  );

  // Use useEffect to auto-select the first role if none is selected
  useEffect(() => {
    if (!selectedRoleId && roles.length > 0) {
      setSelectedRoleId(roles[0].id);
    }
  }, [selectedRoleId, roles]);

  const displayedRole = useMemo(() => {
    return selectedRole || (roles.length > 0 ? roles[0] : null);
  }, [selectedRole, roles]);

  // Reset forbidden state when switching roles or logical contexts
  useEffect(() => {
    if (selectedRoleId) {
      setIsForbidden(false);
    }
  }, [selectedRoleId]);

  // Group permissions by category for current role
  const groupedPermissions = useMemo(
    () => groupPermissionsByCategory(permissions),
    [permissions, groupPermissionsByCategory]
  );

  // Handle role selection
  const handleSelectRole = (role: (typeof roles)[number]) => {
    setSelectedRoleId(role.id);
    setIsForbidden(false);
  };

  // Handle create role
  const handleCreateRole = (roleData: {
    name: string;
    description: string;
    permissionIds: number[];
  }) => {
    if (!canManageRoles) {
      toast.error("You have view-only access. Contact an admin to manage roles.");
      return;
    }
    try {
      createRole(roleData);
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 403) {
        setIsForbidden(true);
        toast.error(
          "Access Denied: You don't have permission to create roles."
        );
      } else {
        toast.error("Failed to create role");
      }
    }
  };

  // Handle delete role
  const handleDeleteRole = (roleId: number) => {
    if (!canManageRoles) {
      toast.error("You have view-only access. Contact an admin to manage roles.");
      return;
    }
    try {
      deleteRole(roleId);
      if (selectedRoleId === roleId) {
        setSelectedRoleId(null);
      }
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 403) {
        setIsForbidden(true);
        toast.error(
          "Access Denied: You don't have permission to delete roles."
        );
      } else {
        toast.error("Failed to delete role");
      }
    }
  };

  // Handle toggle permission
  const handleTogglePermission = (permissionId: number) => {
    if (!canManageRoles) {
      toast.error("You have view-only access. Contact an admin to manage roles.");
      return;
    }
    if (!displayedRole) return;

    try {
      togglePermission(displayedRole.id, permissionId);
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 403) {
        setIsForbidden(true);
        toast.error(
          "Access Denied: You don't have permission to modify role permissions."
        );
      } else {
        toast.error("Failed to update permissions");
      }
    }
  };

  // Show 403 error state
  if (isForbidden) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center px-4 py-12">
        <div className="text-center max-w-md">
          <div className="mb-4">
            <svg
              className="h-12 w-12 text-red-600 mx-auto"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 9v2m0 4v2m0 0a9 9 0 110-18 9 9 0 010 18z"
              />
            </svg>
          </div>
          <h2 className="text-xl font-semibold text-gray-900 mb-2">
            Access Denied
          </h2>
          <p className="text-gray-600 mb-6">
            You don't have permission to manage roles in this organization.
            Contact your organization administrator for access.
          </p>
          <a
            href="/settings"
            className="inline-flex items-center justify-center px-4 py-2 rounded-lg bg-blue-600 text-white hover:bg-blue-700 transition-colors"
          >
            Back to Settings
          </a>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col bg-gray-50">
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-200 bg-white">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              Role & Permission Management
            </h1>
            <p className="text-sm text-gray-600 mt-1">
              Manage roles and assign permissions for your organization
            </p>
          </div>
          {!canManageRoles && (
            <span className="inline-flex items-center rounded-full bg-amber-100 px-3 py-1 text-sm font-medium text-amber-900">
              View only — admin access required to edit
            </span>
          )}
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left Sidebar: Role List */}
        <div className="w-64 flex-shrink-0 overflow-hidden">
          <RoleList
            roles={roles}
            selectedRole={displayedRole}
            onSelectRole={handleSelectRole}
            onCreateRole={handleCreateRole}
            onDeleteRole={handleDeleteRole}
            isLoading={isLoading}
            isCreating={isCreatingRole}
            canManage={canManageRoles}
          />
        </div>

        {/* Right Panel: Permission Matrix */}
        <div className="flex-1 overflow-y-auto bg-white">
          <div className="max-w-4xl mx-auto p-6">
            {isLoading ? (
              <div className="flex items-center justify-center h-96">
                <div className="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
              </div>
            ) : (
              <PermissionMatrix
                role={displayedRole}
                permissions={permissions}
                isLoading={isLoading}
                isUpdating={isUpdatingPermissions}
                groupedPermissions={groupedPermissions}
                hasPermission={hasPermission}
                onTogglePermission={handleTogglePermission}
                canManage={canManageRoles}
              />
            )}
          </div>
        </div>
      </div>

      {/* Fallback: No Roles */}
      {!isLoading && roles.length === 0 && (
        <div className="absolute inset-0 flex items-center justify-center bg-white/50">
          <div className="text-center">
            <p className="text-lg font-medium text-gray-900">
              No roles found
            </p>
            <p className="text-sm text-gray-600 mt-2">
              Create your first role to get started
            </p>
          </div>
        </div>
      )}
    </div>
  );
}

export default RoleManagerPage;
