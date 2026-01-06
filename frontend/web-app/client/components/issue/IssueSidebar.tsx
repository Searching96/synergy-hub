import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Separator } from "@/components/ui/separator";
import {
  CheckCircle2,
  User,
  AlertCircle,
  Tag,
  Clock,
  Calendar,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { Task } from "@/types/task.types";

interface IssueSidebarProps {
  task: Task;
  members: any[];
  disabled?: boolean;
  onStatusChange: (status: string) => Promise<void>;
  onAssigneeChange: (assigneeId: string) => Promise<void>;
  onPriorityChange: (priority: string) => Promise<void>;
}

const PRIORITY_COLORS = {
  LOW: "bg-gray-100 text-gray-700",
  MEDIUM: "bg-blue-100 text-blue-700",
  HIGH: "bg-orange-100 text-orange-700",
  CRITICAL: "bg-red-100 text-red-700",
};

const STATUS_COLORS = {
  TODO: "bg-gray-100 text-gray-700",
  TO_DO: "bg-gray-100 text-gray-700",
  IN_PROGRESS: "bg-blue-100 text-blue-700",
  IN_REVIEW: "bg-purple-100 text-purple-700",
  DONE: "bg-green-100 text-green-700",
  BLOCKED: "bg-red-100 text-red-700",
};

const getInitials = (name: string) => {
  return name
    .split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);
};

export function IssueSidebar({
  task,
  members,
  disabled = false,
  onStatusChange,
  onAssigneeChange,
  onPriorityChange,
}: IssueSidebarProps) {
  return (
    <div className="w-sidebar-wide border-l bg-gray-50/50 overflow-y-auto">
      <div className="space-y-6 px-6 py-6">
        {/* Status */}
        <div>
          <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4" />
            Status
          </Label>
          <Select value={task.status} onValueChange={onStatusChange} disabled={disabled}>
            <SelectTrigger>
              <SelectValue>
                <Badge className={cn(STATUS_COLORS[task.status as keyof typeof STATUS_COLORS])}>
                  {task.status.replace("_", " ")}
                </Badge>
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="TODO">To Do</SelectItem>
              <SelectItem value="IN_PROGRESS">In Progress</SelectItem>
              <SelectItem value="IN_REVIEW">In Review</SelectItem>
              <SelectItem value="DONE">Done</SelectItem>
              <SelectItem value="BLOCKED">Blocked</SelectItem>
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
            onValueChange={onAssigneeChange}
            disabled={disabled}
          >
            <SelectTrigger>
              <SelectValue>
                {task.assigneeId && task.assigneeName ? (
                  <div className="flex items-center gap-2">
                    <Avatar className="h-6 w-6">
                      <AvatarFallback className="text-xs">
                        {getInitials(task.assigneeName)}
                      </AvatarFallback>
                    </Avatar>
                    <span>{task.assigneeName}</span>
                  </div>
                ) : (
                  "Unassigned"
                )}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="unassigned">Unassigned</SelectItem>
              {members
                .filter((member: any) => member?.user?.id && member?.user?.name)
                .map((member: any) => (
                  <SelectItem key={member.user.id} value={member.user.id.toString()}>
                    <div className="flex items-center gap-2">
                      <Avatar className="h-6 w-6">
                        <AvatarFallback className="text-xs">
                          {getInitials(member.user.name)}
                        </AvatarFallback>
                      </Avatar>
                      <span>{member.user.name}</span>
                    </div>
                  </SelectItem>
                ))}
            </SelectContent>
          </Select>
        </div>

        {/* Reporter */}
        {(task.reporterName || task.createdBy?.name) && (
          <div>
            <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
              <User className="h-4 w-4" />
              Reporter
            </Label>
            <div className="flex items-center gap-2 p-2 bg-white rounded-md border">
              <Avatar className="h-6 w-6">
                <AvatarFallback className="text-xs">
                  {getInitials(task.reporterName || task.createdBy?.name || "U")}
                </AvatarFallback>
              </Avatar>
              <span className="text-sm">
                {task.reporterName || task.createdBy?.name || "Unknown"}
              </span>
            </div>
          </div>
        )}

        {/* Priority */}
        <div>
          <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
            <AlertCircle className="h-4 w-4" />
            Priority
          </Label>
          <Select value={task.priority} onValueChange={onPriorityChange} disabled={disabled}>
            <SelectTrigger>
              <SelectValue>
                <Badge className={cn(PRIORITY_COLORS[task.priority as keyof typeof PRIORITY_COLORS])}>
                  {task.priority}
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

        {/* Labels */}
        <div>
          <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
            <Tag className="h-4 w-4" />
            Labels
          </Label>
          <div className="flex flex-wrap gap-1">
            <Badge variant="outline">{task.type}</Badge>
            {task.sprintName && <Badge variant="outline">{task.sprintName}</Badge>}
          </div>
        </div>

        {/* Due Date */}
        {task.dueDate && (
          <div>
            <Label className="text-xs font-semibold mb-2 flex items-center gap-2">
              <Calendar className="h-4 w-4" />
              Due Date
            </Label>
            <div className="p-2 bg-white rounded-md border text-sm">
              {new Date(task.dueDate).toLocaleDateString()}
            </div>
          </div>
        )}

        {/* Time Tracking */}
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

        <Separator />

        {/* Metadata */}
        <div className="space-y-2 text-xs text-muted-foreground">
          <div>Created {new Date(task.createdAt).toLocaleDateString()}</div>
          <div>Updated {new Date(task.updatedAt).toLocaleDateString()}</div>
        </div>
      </div>
    </div>
  );
}
