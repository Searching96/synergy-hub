/**
 * RoleList Component
 * Sidebar showing all roles with selection and "Create Role" button
 * Indicates system roles with a badge
 */

import { useState } from "react";
import { CreateRoleDialog } from "@/components/rbac/CreateRoleDialog";
import type { Role } from "@/types/rbac.types";
import { cn } from "@/lib/utils";

interface RoleListProps {
  roles: Role[];
  selectedRole: Role | null;
  onSelectRole: (role: Role) => void;
  onCreateRole: (roleData: { name: string; description: string; permissionIds: number[] }) => void;
  onDeleteRole?: (roleId: number) => void;
  isLoading?: boolean;
  isCreating?: boolean;
}

export function RoleList({
  roles,
  selectedRole,
  onSelectRole,
  onCreateRole,
  onDeleteRole,
  isLoading = false,
  isCreating = false,
}: RoleListProps) {
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [deleteConfirmRoleId, setDeleteConfirmRoleId] = useState<number | null>(
    null
  );

  const handleDeleteClick = (e: React.MouseEvent, roleId: number) => {
    e.stopPropagation();
    setDeleteConfirmRoleId(roleId);
  };

  const handleConfirmDelete = (roleId: number) => {
    onDeleteRole?.(roleId);
    setDeleteConfirmRoleId(null);
  };

  return (
    <div className="flex flex-col h-full bg-white border-r border-gray-200">
      {/* Header */}
      <div className="p-4 border-b border-gray-200">
        <h2 className="font-semibold text-gray-900">Roles</h2>
        <p className="text-xs text-gray-500 mt-1">
          {roles.length} role{roles.length !== 1 ? "s" : ""} in this organization
        </p>
      </div>

      {/* Role List */}
      <div className="flex-1 overflow-y-auto">
        {isLoading ? (
          <div className="flex items-center justify-center h-32">
            <div className="h-6 w-6 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
          </div>
        ) : roles.length === 0 ? (
          <div className="p-4 text-center">
            <p className="text-sm text-gray-500">No roles created yet</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-200">
            {roles.map((role) => (
              <div
                key={role.id}
                className={cn(
                  "px-4 py-3 cursor-pointer transition-colors hover:bg-gray-50",
                  selectedRole?.id === role.id && "bg-blue-50 border-r-2 border-blue-600"
                )}
                onClick={() => onSelectRole(role)}
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="font-medium text-gray-900 truncate">
                        {role.name}
                      </p>
                      {role.isSystemRole && (
                        <span className="shrink-0 inline-flex items-center rounded-full bg-amber-100 px-2 py-0.5">
                          <span className="text-xs font-medium text-amber-900">
                            System
                          </span>
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-gray-500 truncate mt-0.5">
                      {role.permissions.length} permission
                      {role.permissions.length !== 1 ? "s" : ""}
                    </p>
                  </div>

                  {/* Delete Button - Only for non-system roles */}
                  {!role.isSystemRole && onDeleteRole && (
                    <button
                      onClick={(e) => handleDeleteClick(e, role.id)}
                      className="shrink-0 p-1 hover:bg-red-50 rounded transition-colors text-gray-400 hover:text-red-600"
                      title="Delete role"
                    >
                      <svg
                        className="h-4 w-4"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                        />
                      </svg>
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Create Role Button */}
      <div className="p-4 border-t border-gray-200">
        <button
          onClick={() => setShowCreateDialog(true)}
          className={cn(
            "w-full px-4 py-2 rounded-lg font-medium transition-colors",
            "bg-blue-600 text-white hover:bg-blue-700 active:bg-blue-800",
            isCreating && "opacity-50 cursor-not-allowed"
          )}
          disabled={isCreating}
        >
          {isCreating ? "Creating..." : "Create Role"}
        </button>
      </div>

      {/* Create Role Dialog */}
      <CreateRoleDialog
        open={showCreateDialog}
        onOpenChange={setShowCreateDialog}
        onCreateRole={(roleData) => {
          onCreateRole({
            name: roleData.name,
            description: roleData.description,
            permissionIds: roleData.permissionIds ?? [],
          });
          setShowCreateDialog(false);
        }}
      />

      {/* Delete Confirmation Dialog */}
      {deleteConfirmRoleId !== null && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-lg max-w-sm w-full">
            <div className="p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-2">
                Delete Role?
              </h3>
              <p className="text-sm text-gray-600 mb-6">
                Are you sure you want to delete this role? This action cannot
                be undone.
              </p>
              <div className="flex gap-3 justify-end">
                <button
                  onClick={() => setDeleteConfirmRoleId(null)}
                  className="px-4 py-2 rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={() =>
                    handleConfirmDelete(deleteConfirmRoleId)
                  }
                  className="px-4 py-2 rounded-lg bg-red-600 text-white hover:bg-red-700 transition-colors"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
