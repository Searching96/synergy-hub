import { Draggable } from "@hello-pangea/dnd";
import { useSearchParams } from "react-router-dom";
import { cn } from "@/lib/utils";
import {
  CheckSquare,
  Bug,
  Lightbulb,
  AlertCircle,
  ArrowUp,
  ArrowDown,
  Minus,
  UserRound,
  MoreHorizontal,
  Plus
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuSub,
  DropdownMenuSubTrigger,
  DropdownMenuSubContent,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import type { BacklogTask } from "@/types/task.types";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface BacklogTaskRowProps {
  task: BacklogTask;
  index: number;
  projectKey?: string;
  members?: Array<{ id: number; name: string; email: string }>;
  onUpdateStoryPoints: (taskId: number, storyPoints: number | null) => void;
  onUpdateAssignee: (taskId: number, assigneeId: number | null) => void;
  isProjectArchived?: boolean;
  onClick?: () => void;
  onAddEpic?: (taskId: number) => void;
}

const typeIcons = {
  TASK: CheckSquare,
  BUG: Bug,
  STORY: Lightbulb,
  EPIC: AlertCircle,
};

const priorityConfig = {
  CRITICAL: { icon: AlertCircle, color: "text-red-600" },
  HIGH: { icon: ArrowUp, color: "text-orange-600" },
  MEDIUM: { icon: Minus, color: "text-yellow-600" },
  LOW: { icon: ArrowDown, color: "text-blue-600" },
};

export default function BacklogTaskRow({
  task,
  index,
  projectKey = "PROJ",
  members = [],
  onUpdateStoryPoints,
  onUpdateAssignee,
  isProjectArchived,
  onAddEpic,
  ...props
}: BacklogTaskRowProps) {
  const [searchParams, setSearchParams] = useSearchParams();
  const TypeIcon = typeIcons[task.type as keyof typeof typeIcons] || CheckSquare;
  const priority = priorityConfig[task.priority as keyof typeof priorityConfig] || priorityConfig.MEDIUM;
  const PriorityIcon = priority.icon;

  const assigneeInitials = task.assignee?.name
    ? task.assignee.name.split(" ").map((n) => n[0]).join("").slice(0, 2).toUpperCase()
    : "?";

  const handleClick = (e: React.MouseEvent) => {
    // Don't open modal if clicking on interactive elements
    const target = e.target as HTMLElement;
    if (target.tagName === 'INPUT' || target.closest('button') || target.closest('[role="combobox"]')) {
      return;
    }

    // If external onClick is provided, use it and skip default behavior
    if (props.onClick) {
      props.onClick();
      return;
    }

    const next = new URLSearchParams(searchParams);
    next.delete("create");
    next.set("issue", `${projectKey}-${task.id}`);
    setSearchParams(next);
  };

  return (
    <Draggable draggableId={`task-${task.id}`} index={index} isDragDisabled={task.archived}>
      {(provided, snapshot) => (
        <div
          ref={provided.innerRef}
          {...provided.draggableProps}
          {...provided.dragHandleProps}
          onClick={handleClick}
          className={cn(
            "relative group bg-white border rounded-lg p-3 mb-2 grid grid-cols-[auto_auto_1fr_auto_auto] gap-3 items-center hover:shadow-md transition-all cursor-pointer",
            snapshot.isDragging && "shadow-lg ring-2 ring-blue-500",
            (task.archived || isProjectArchived) && "opacity-60"
          )}
        >
          {/* Hover actions for missing epic - moved left to avoid overlapping assignee */}
          {!(task.epicTitle as string || task.epic?.title || task.epicName) && (
            <div className="absolute right-40 top-1/2 -translate-y-1/2 flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity bg-white/95 p-1 rounded-md shadow-sm z-10 border">
              <Button
                size="sm"
                variant="ghost"
                className="h-7 text-xs px-2 hover:bg-gray-100"
                onClick={(e) => {
                  e.stopPropagation();
                  onAddEpic?.(task.id);
                }}
              >
                <Plus className="h-3.5 w-3.5 mr-1" />
                Add epic
              </Button>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button size="icon" variant="ghost" className="h-7 w-7 hover:bg-gray-100">
                    <MoreHorizontal className="h-4 w-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="w-48">
                  <DropdownMenuItem>Move to</DropdownMenuItem>
                  <DropdownMenuItem>Copy issue link</DropdownMenuItem>
                  <DropdownMenuItem>Copy issue key</DropdownMenuItem>
                  <DropdownMenuItem>Add flag</DropdownMenuItem>
                  <DropdownMenuItem>Assignee</DropdownMenuItem>
                  <DropdownMenuItem>Parent</DropdownMenuItem>
                  <DropdownMenuSub>
                    <DropdownMenuSubTrigger>Story point estimate</DropdownMenuSubTrigger>
                    <DropdownMenuSubContent>
                      {[1, 2, 3, 5, 8, 13].map((sp) => (
                        <DropdownMenuItem key={sp}>{sp}</DropdownMenuItem>
                      ))}
                    </DropdownMenuSubContent>
                  </DropdownMenuSub>
                  <DropdownMenuItem>Split issue</DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem className="text-destructive">Delete</DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          )}

          {/* Type Icon */}
          <TypeIcon className="h-4 w-4 text-muted-foreground" />

          {/* Task Key */}
          <span className="text-sm font-medium text-muted-foreground">
            {projectKey}-{task.id}
          </span>

          {/* Title, Priority & Tags */}
          <div className="flex items-center gap-2 min-w-0">
            <PriorityIcon className={cn("h-4 w-4 flex-shrink-0", priority.color)} />
            <span className="text-sm font-medium truncate">{task.title}</span>
            <div className="flex items-center gap-1 flex-wrap">
              <span className="text-[11px] px-2 py-0.5 rounded-full bg-gray-100 text-gray-700 border">
                {(task.epicTitle as string) || task.epic?.title || task.epicName || "No epic"}
              </span>
              <span className={cn(
                "text-[11px] px-2 py-0.5 rounded-full border",
                task.status === "DONE" && "bg-green-100 text-green-700 border-green-200",
                task.status === "IN_PROGRESS" && "bg-blue-100 text-blue-700 border-blue-200",
                task.status === "TO_DO" && "bg-gray-100 text-gray-700 border-gray-200",
                task.status === "IN_REVIEW" && "bg-purple-100 text-purple-700 border-purple-200",
                task.status === "BLOCKED" && "bg-red-100 text-red-700 border-red-200"
              )}>
                {task.status.replace("_", " ")}
              </span>
            </div>
          </div>

          {/* Assignee - Editable */}
          <Select
            value={task.assignee?.id?.toString() || "unassigned"}
            onValueChange={(value) => {
              const assigneeId = value === "unassigned" ? null : parseInt(value);
              onUpdateAssignee(task.id, assigneeId);
            }}
            disabled={task.archived || isProjectArchived}
          >
            <SelectTrigger className="h-8 text-xs" onClick={(e) => e.stopPropagation()}>
              <SelectValue placeholder="Unassigned" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="unassigned">Unassigned</SelectItem>
              {members
                .filter((member) => member && member.id != null)
                .map((member) => (
                  <SelectItem key={member.id} value={member.id.toString()}>
                    {member.name}
                  </SelectItem>
                ))}
            </SelectContent>
          </Select>

          {/* Assignee avatar */}
          <div className="w-8 h-8 rounded-full bg-gray-100 border flex items-center justify-center text-xs text-gray-700">
            {task.assignee ? assigneeInitials : <UserRound className="h-4 w-4 text-gray-400" />}
          </div>
        </div>
      )}
    </Draggable>
  );
}
