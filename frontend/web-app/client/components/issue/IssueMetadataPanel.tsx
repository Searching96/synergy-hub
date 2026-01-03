/**
 * IssueMetadataPanel Component
 * Displays and manages issue metadata (status, priority, assignee, dates, etc.)
 */

import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Calendar, User, Tag, Clock } from "lucide-react";
import { formatDate, isPastDate } from "@/lib/date";
import { cn } from "@/lib/utils";
import { getTaskStatus, getTaskPriority } from "@/lib/validation";
import type { Task, TaskStatus, TaskPriority } from "@/types/task.types";
import type { ProjectMember } from "@/types/project.types";

interface IssueMetadataPanelProps {
  task: Task;
  members: ProjectMember[];
  isProjectArchived: boolean;
  onStatusChange: (status: TaskStatus) => void;
  onPriorityChange: (priority: TaskPriority) => void;
  onAssigneeChange: (assigneeId: number | null) => void;
}

const PRIORITY_COLORS: Record<TaskPriority, string> = {
  LOW: "bg-gray-100 text-gray-700",
  MEDIUM: "bg-blue-100 text-blue-700",
  HIGH: "bg-orange-100 text-orange-700",
  CRITICAL: "bg-red-100 text-red-700",
};

const STATUS_COLORS: Record<TaskStatus, string> = {
  TO_DO: "bg-gray-100 text-gray-700",
  IN_PROGRESS: "bg-blue-100 text-blue-700",
  IN_REVIEW: "bg-purple-100 text-purple-700",
  DONE: "bg-green-100 text-green-700",
  BLOCKED: "bg-red-100 text-red-700",
};

export function IssueMetadataPanel({
  task,
  members,
  isProjectArchived,
  onStatusChange,
  onPriorityChange,
  onAssigneeChange,
}: IssueMetadataPanelProps) {
  const isOverdue = task.dueDate && isPastDate(task.dueDate) && task.status !== "DONE";
  
  // Use safe enum getters with fallbacks
  const safeStatus = getTaskStatus(task.status);
  const safePriority = getTaskPriority(task.priority);

  return (
    <div className="space-y-6">
      {/* Status */}
      <div>
        <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
          <Tag className="h-4 w-4" />
          Status
        </Label>
        <Select
          value={safeStatus}
          onValueChange={onStatusChange}
          disabled={isProjectArchived}
        >
          <SelectTrigger>
            <SelectValue>
              <Badge className={cn("text-xs", STATUS_COLORS[safeStatus])}>
                {safeStatus.replace(/_/g, " ")}
              </Badge>
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="TO_DO">To Do</SelectItem>
            <SelectItem value="IN_PROGRESS">In Progress</SelectItem>
            <SelectItem value="IN_REVIEW">In Review</SelectItem>
            <SelectItem value="DONE">Done</SelectItem>
            <SelectItem value="BLOCKED">Blocked</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Priority */}
      <div>
        <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
          <Tag className="h-4 w-4" />
          Priority
        </Label>
        <Select
          value={safePriority}
          onValueChange={onPriorityChange}
          disabled={isProjectArchived}
        >
          <SelectTrigger>
            <SelectValue>
              <Badge className={cn("text-xs", PRIORITY_COLORS[safePriority])}>
                {safePriority}
              </Badge>
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="LOW">Low</SelectItem>
            <SelectItem value="MEDIUM">Medium</SelectItem>
            <SelectItem value="HIGH">High</SelectItem>
            <SelectItem value="CRITICAL">Critical</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Assignee */}
      <div>
        <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
          <User className="h-4 w-4" />
          Assignee
        </Label>
        <Select
          value={task.assigneeId?.toString() || "unassigned"}
          onValueChange={(value) =>
            onAssigneeChange(value === "unassigned" ? null : parseInt(value))
          }
          disabled={isProjectArchived}
        >
          <SelectTrigger>
            <SelectValue>
              {task.assignee ? (
                <div className="flex items-center gap-2">
                  <div className="h-6 w-6 rounded-full bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center text-xs font-semibold text-white">
                    {task.assignee.name.charAt(0).toUpperCase()}
                  </div>
                  <span>{task.assignee.name}</span>
                </div>
              ) : (
                <span className="text-muted-foreground">Unassigned</span>
              )}
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="unassigned">
              <span className="text-muted-foreground">Unassigned</span>
            </SelectItem>
            {members.map((member) => {
              const memberUser = member.user || { id: member.userId, name: member.name || "Unknown" };
              return (
                <SelectItem key={memberUser.id} value={memberUser.id.toString()}>
                  <div className="flex items-center gap-2">
                    <div className="h-6 w-6 rounded-full bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center text-xs font-semibold text-white">
                      {memberUser.name.charAt(0).toUpperCase()}
                    </div>
                    <span>{memberUser.name}</span>
                  </div>
                </SelectItem>
              );
            })}
          </SelectContent>
        </Select>
      </div>

      {/* Reporter */}
      {task.reporter && (
        <div>
          <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
            <User className="h-4 w-4" />
            Reporter
          </Label>
          <div className="flex items-center gap-2 text-sm">
            <div className="h-6 w-6 rounded-full bg-gradient-to-br from-green-500 to-teal-500 flex items-center justify-center text-xs font-semibold text-white">
              {task.reporter.name.charAt(0).toUpperCase()}
            </div>
            <span>{task.reporter.name}</span>
          </div>
        </div>
      )}

      {/* Due Date */}
      {task.dueDate && (
        <div>
          <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
            <Calendar className="h-4 w-4" />
            Due Date
          </Label>
          <div
            className={cn(
              "text-sm",
              isOverdue ? "text-red-600 font-semibold" : "text-foreground"
            )}
          >
            {formatDate(task.dueDate)}
            {isOverdue && " (Overdue)"}
          </div>
        </div>
      )}

      {/* Story Points */}
      {task.storyPoints !== null && task.storyPoints !== undefined && (
        <div>
          <Label className="text-xs font-semibold mb-2">Story Points</Label>
          <div className="text-sm font-medium">{task.storyPoints}</div>
        </div>
      )}

      {/* Time Tracking */}
      {(task.estimatedHours || task.actualHours) && (
        <div>
          <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
            <Clock className="h-4 w-4" />
            Time Tracking
          </Label>
          <div className="space-y-1 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Estimated:</span>
              <span>{task.estimatedHours || 0}h</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Actual:</span>
              <span>{task.actualHours || 0}h</span>
            </div>
          </div>
        </div>
      )}

      {/* Metadata */}
      <div className="space-y-2 text-xs text-muted-foreground pt-4 border-t">
        <div>Created {formatDate(task.createdAt)}</div>
        <div>Updated {formatDate(task.updatedAt)}</div>
      </div>
    </div>
  );
}
