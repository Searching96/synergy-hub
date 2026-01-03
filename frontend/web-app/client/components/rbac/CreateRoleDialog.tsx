/**
 * CreateRoleDialog Component
 * Modal dialog for creating a new role with name, description, and initial permissions
 * Uses React Hook Form + Zod for validation
 */

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
  Dialog,
  DialogContent,
} from "@radix-ui/react-dialog";
import { useRBAC } from "@/hooks/useRBAC";
import { cn } from "@/lib/utils";
import { useState } from "react";

/**
 * Validation schema matching backend CreateRoleRequest DTO
 */
const createRoleSchema = z.object({
  name: z
    .string()
    .min(3, "Role name must be at least 3 characters")
    .max(50, "Role name must be at most 50 characters"),
  description: z
    .string()
    .min(5, "Description must be at least 5 characters")
    .max(200, "Description must be at most 200 characters"),
  permissionIds: z.array(z.number()).optional().default([]),
});

type CreateRoleFormData = z.infer<typeof createRoleSchema>;

interface CreateRoleDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreateRole: (data: CreateRoleFormData) => void;
}

export function CreateRoleDialog({
  open,
  onOpenChange,
  onCreateRole,
}: CreateRoleDialogProps) {
  const { permissions, isLoadingPermissions } = useRBAC();
  const [selectedPermissions, setSelectedPermissions] = useState<Set<number>>(
    new Set()
  );

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
  } = useForm<CreateRoleFormData>({
    resolver: zodResolver(createRoleSchema),
    defaultValues: {
      name: "",
      description: "",
      permissionIds: [],
    },
  });

  const handleOpenChange = (newOpen: boolean) => {
    onOpenChange(newOpen);
    if (!newOpen) {
      reset();
      setSelectedPermissions(new Set());
    }
  };

  const onSubmit = (data: CreateRoleFormData) => {
    onCreateRole({
      ...data,
      permissionIds: Array.from(selectedPermissions),
    });
    reset();
    setSelectedPermissions(new Set());
  };

  const togglePermission = (permissionId: number) => {
    const newSet = new Set(selectedPermissions);
    if (newSet.has(permissionId)) {
      newSet.delete(permissionId);
    } else {
      newSet.add(permissionId);
    }
    setSelectedPermissions(newSet);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="fixed left-[50%] top-[50%] translate-x-[-50%] translate-y-[-50%] z-50 w-full max-w-md p-6 bg-white rounded-lg shadow-lg border border-gray-200">
        <div className="mb-4">
          <h2 className="text-lg font-semibold text-gray-900">
            Create New Role
          </h2>
          <p className="text-sm text-gray-600 mt-1">
            Define a new role with permissions for your organization
          </p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* Role Name */}
          <div>
            <label
              htmlFor="name"
              className="block text-sm font-medium text-gray-900 mb-1"
            >
              Role Name
            </label>
            <input
              {...register("name")}
              id="name"
              type="text"
              placeholder="e.g., Content Manager"
              className={cn(
                "w-full px-3 py-2 border rounded-lg text-sm transition-colors",
                "focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent",
                errors.name ? "border-red-500" : "border-gray-300"
              )}
            />
            {errors.name && (
              <p className="mt-1 text-xs text-red-600">{errors.name.message}</p>
            )}
          </div>

          {/* Description */}
          <div>
            <label
              htmlFor="description"
              className="block text-sm font-medium text-gray-900 mb-1"
            >
              Description
            </label>
            <textarea
              {...register("description")}
              id="description"
              placeholder="Describe the purpose of this role"
              rows={3}
              className={cn(
                "w-full px-3 py-2 border rounded-lg text-sm transition-colors resize-none",
                "focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent",
                errors.description ? "border-red-500" : "border-gray-300"
              )}
            />
            {errors.description && (
              <p className="mt-1 text-xs text-red-600">
                {errors.description.message}
              </p>
            )}
          </div>

          {/* Permissions Section */}
          <div>
            <label className="block text-sm font-medium text-gray-900 mb-3">
              Assign Permissions (Optional)
            </label>
            {isLoadingPermissions ? (
              <div className="flex items-center justify-center h-24">
                <div className="h-5 w-5 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
              </div>
            ) : permissions.length === 0 ? (
              <p className="text-sm text-gray-500 py-4">
                No permissions available
              </p>
            ) : (
              <div className="space-y-2 max-h-48 overflow-y-auto border border-gray-200 rounded-lg p-3">
                {permissions.map((permission) => (
                  <label
                    key={permission.id}
                    className="flex items-center gap-2 cursor-pointer hover:bg-gray-50 p-2 rounded transition-colors"
                  >
                    <input
                      type="checkbox"
                      checked={selectedPermissions.has(permission.id)}
                      onChange={() => togglePermission(permission.id)}
                      className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900">
                        {permission.name}
                      </p>
                      {permission.description && (
                        <p className="text-xs text-gray-500 truncate">
                          {permission.description}
                        </p>
                      )}
                    </div>
                  </label>
                ))}
              </div>
            )}
          </div>

          {/* Actions */}
          <div className="flex gap-3 justify-end pt-4">
            <button
              type="button"
              onClick={() => handleOpenChange(false)}
              className="px-4 py-2 rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors disabled:opacity-50"
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className={cn(
                "px-4 py-2 rounded-lg bg-blue-600 text-white font-medium transition-colors",
                "hover:bg-blue-700 active:bg-blue-800 disabled:opacity-50 disabled:cursor-not-allowed"
              )}
              disabled={isSubmitting}
            >
              {isSubmitting ? "Creating..." : "Create Role"}
            </button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
