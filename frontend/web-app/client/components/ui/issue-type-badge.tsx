/**
 * IssueTypeBadge Component
 * Displays issue type with icon in a consistent format across the app
 */

import { Zap, Lightbulb, CheckSquare, Bug, ListChecks } from "lucide-react";
import { cn } from "@/lib/utils";
import type { TaskType } from "@/types/task.types";

interface IssueTypeBadgeProps {
  type: TaskType;
  showLabel?: boolean;
  size?: "sm" | "md" | "lg";
  className?: string;
}

const typeConfig: Record<TaskType, { icon: any; color: string; bg: string; label: string }> = {
  EPIC: { 
    icon: Zap, 
    color: "text-purple-600", 
    bg: "bg-purple-50 border-purple-200", 
    label: "Epic" 
  },
  STORY: { 
    icon: Lightbulb, 
    color: "text-green-600", 
    bg: "bg-green-50 border-green-200", 
    label: "Story" 
  },
  TASK: { 
    icon: CheckSquare, 
    color: "text-blue-600", 
    bg: "bg-blue-50 border-blue-200", 
    label: "Task" 
  },
  BUG: { 
    icon: Bug, 
    color: "text-red-600", 
    bg: "bg-red-50 border-red-200", 
    label: "Bug" 
  },
  SUBTASK: { 
    icon: ListChecks, 
    color: "text-gray-600", 
    bg: "bg-gray-50 border-gray-200", 
    label: "Subtask" 
  },
};

const sizeConfig = {
  sm: {
    icon: "h-3 w-3",
    text: "text-xs",
    padding: "px-1.5 py-0.5",
    gap: "gap-1",
  },
  md: {
    icon: "h-4 w-4",
    text: "text-sm",
    padding: "px-2 py-1",
    gap: "gap-1.5",
  },
  lg: {
    icon: "h-5 w-5",
    text: "text-base",
    padding: "px-3 py-1.5",
    gap: "gap-2",
  },
};

export function IssueTypeBadge({ 
  type, 
  showLabel = false, 
  size = "sm",
  className 
}: IssueTypeBadgeProps) {
  const config = typeConfig[type] || typeConfig.TASK;
  const sizes = sizeConfig[size];
  const Icon = config.icon;

  return (
    <div
      className={cn(
        "inline-flex items-center rounded-md border font-medium",
        config.color,
        config.bg,
        sizes.padding,
        sizes.gap,
        className
      )}
    >
      <Icon className={sizes.icon} />
      {showLabel && <span className={sizes.text}>{config.label}</span>}
    </div>
  );
}

/**
 * Get type configuration for use in other components
 */
export function getTypeConfig(type: TaskType) {
  return typeConfig[type] || typeConfig.TASK;
}
