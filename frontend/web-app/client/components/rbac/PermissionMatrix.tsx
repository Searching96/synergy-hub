/**
 * PermissionMatrix Component
 * Main matrix view showing all permissions grouped by category
 * Each row is a permission with toggles for the selected role
 */

import { useState } from "react";
import { PermissionToggle } from "./PermissionToggle";
import type { Role, Permission } from "@/types/rbac.types";
import { cn } from "@/lib/utils";

interface PermissionMatrixProps {
  role: Role | null;
  permissions: Permission[];
  isLoading?: boolean;
  isUpdating?: boolean;
  groupedPermissions: Array<[string, Permission[]]>;
  hasPermission: (roleId: number, permissionId: number) => boolean;
  onTogglePermission: (permissionId: number) => void;
  canManage?: boolean;
}

export function PermissionMatrix({
  role,
  permissions,
  isLoading = false,
  isUpdating = false,
  groupedPermissions,
  hasPermission,
  onTogglePermission,
  canManage = true,
}: PermissionMatrixProps) {
  const [expandedCategory, setExpandedCategory] = useState<string | null>(
    groupedPermissions[0]?.[0] || null
  );

  if (!role) {
    return (
      <div className="flex items-center justify-center h-96">
        <p className="text-gray-500 text-center">
          <span className="block text-lg font-medium mb-2">
            Select a role to view permissions
          </span>
          <span className="text-sm">
            Choose a role from the sidebar to manage its permissions
          </span>
        </p>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Role Header */}
      <div className="border-b pb-4">
        <div className="flex items-start gap-3">
          <div className="flex-1">
            <h2 className="text-lg font-semibold text-gray-900">{role.name}</h2>
            <p className="text-sm text-gray-600 mt-1">{role.description}</p>
          </div>
          <div className="flex items-center gap-2">
            {!canManage && (
              <span className="inline-flex items-center rounded-full bg-amber-50 px-3 py-1 text-xs font-medium text-amber-900">
                View only
              </span>
            )}
            {role.isSystemRole && (
              <span className="inline-flex items-center rounded-full bg-amber-100 px-3 py-1">
                <span className="text-xs font-medium text-amber-900">
                  System Role
                </span>
              </span>
            )}
          </div>
        </div>
        <p className="text-xs text-gray-500 mt-3">
          {role.permissions.length} permission{role.permissions.length !== 1 ? "s" : ""} assigned
        </p>
      </div>

      {/* Permissions by Category */}
      <div className="space-y-3">
        {groupedPermissions.length === 0 ? (
          <p className="text-sm text-gray-500 py-4">No permissions available</p>
        ) : (
          groupedPermissions.map(([category, categoryPermissions]) => (
            <div key={category} className="border rounded-lg overflow-hidden">
              {/* Category Header */}
              <button
                onClick={() =>
                  setExpandedCategory(
                    expandedCategory === category ? null : category
                  )
                }
                className={cn(
                  "w-full px-4 py-3 flex items-center justify-between hover:bg-gray-100 transition-colors",
                  "bg-gray-50 border-b"
                )}
              >
                <h3 className="font-medium text-gray-900">{category}</h3>
                <svg
                  className={cn(
                    "h-5 w-5 text-gray-600 transition-transform",
                    expandedCategory === category && "rotate-180"
                  )}
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M19 14l-7 7m0 0l-7-7m7 7V3"
                  />
                </svg>
              </button>

              {/* Category Permissions */}
              {expandedCategory === category && (
                <div className="bg-white divide-y">
                  {categoryPermissions.map((permission) => (
                    <PermissionToggle
                      key={permission.id}
                      permission={permission}
                      role={role}
                      isChecked={hasPermission(role.id, permission.id)}
                      isDisabled={role.isSystemRole || !canManage}
                      isLoading={isUpdating}
                      onToggle={() => {
                        if (!role.isSystemRole && canManage) {
                          onTogglePermission(permission.id);
                        }
                      }}
                    />
                  ))}
                </div>
              )}
            </div>
          ))
        )}
      </div>

      {/* Info Message */}
      {role.isSystemRole && (
        <div className="rounded-lg bg-amber-50 border border-amber-200 p-4">
          <p className="text-sm text-amber-900">
            <span className="font-medium">System Role:</span> This role is
            protected and cannot be modified. Contact your system administrator
            if you need to change its permissions.
          </p>
        </div>
      )}

      {!canManage && (
        <div className="rounded-lg bg-blue-50 border border-blue-200 p-4">
          <p className="text-sm text-blue-900">
            You have view-only access. Ask an organization admin to grant you editing rights.
          </p>
        </div>
      )}
    </div>
  );
}
