/**
 * PermissionToggle Component
 * A single row in the permission matrix showing a permission with a switch
 * Handles accessibility, loading states, and system role tooltips
 */

import { Switch } from "@radix-ui/react-switch";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@radix-ui/react-tooltip";
import { cn } from "@/lib/utils";
import type { Permission, Role } from "@/types/rbac.types";

interface PermissionToggleProps {
  permission: Permission;
  role: Role;
  isChecked: boolean;
  isDisabled?: boolean;
  isLoading?: boolean;
  onToggle: () => void;
}

export function PermissionToggle({
  permission,
  role,
  isChecked,
  isDisabled = false,
  isLoading = false,
  onToggle,
}: PermissionToggleProps) {
  // System roles cannot be modified
  const cannotModify = role.isSystemRole;
  const isDisabledState = isDisabled || isLoading || cannotModify;

  const tooltipContent = cannotModify
    ? `System roles cannot be modified. "${role.name}" is protected.`
    : undefined;

  return (
    <TooltipProvider>
      <div className="flex items-center gap-3 px-4 py-2 hover:bg-gray-50 rounded transition-colors">
        {/* Permission Info */}
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-gray-900">{permission.name}</p>
          {permission.description && (
            <p className="text-xs text-gray-500 truncate">
              {permission.description}
            </p>
          )}
        </div>

        {/* Toggle Switch */}
        <Tooltip>
          <TooltipTrigger asChild>
            <div className={cn(isDisabledState && "opacity-50 cursor-not-allowed")}>
              <Switch
                checked={isChecked}
                onCheckedChange={onToggle}
                disabled={isDisabledState}
                aria-label={`Toggle ${permission.name} for ${role.name}`}
                className={cn(
                  "relative inline-flex h-6 w-11 items-center rounded-full transition-colors",
                  isChecked
                    ? "bg-blue-600"
                    : "bg-gray-300",
                  isDisabledState && "opacity-50 cursor-not-allowed"
                )}
              >
                <span
                  className={cn(
                    "inline-block h-4 w-4 transform rounded-full bg-white transition-transform",
                    isChecked ? "translate-x-5" : "translate-x-1"
                  )}
                />
              </Switch>
            </div>
          </TooltipTrigger>
          {tooltipContent && (
            <TooltipContent>
              <p className="text-sm">{tooltipContent}</p>
            </TooltipContent>
          )}
        </Tooltip>

        {/* Loading Indicator */}
        {isLoading && (
          <div className="h-4 w-4 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
        )}
      </div>
    </TooltipProvider>
  );
}
