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
  Minus 
} from "lucide-react";
import type { BacklogTask } from "@/hooks/useBacklog";
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
}: BacklogTaskRowProps) {
  const [searchParams, setSearchParams] = useSearchParams();
  const TypeIcon = typeIcons[task.type as keyof typeof typeIcons] || CheckSquare;
  const priority = priorityConfig[task.priority as keyof typeof priorityConfig] || priorityConfig.MEDIUM;
  const PriorityIcon = priority.icon;

  const handleClick = (e: React.MouseEvent) => {
    // Don't open modal if clicking on interactive elements
    const target = e.target as HTMLElement;
    if (target.tagName === 'INPUT' || target.closest('button') || target.closest('[role="combobox"]')) {
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
            "bg-white border rounded-lg p-3 mb-2 grid grid-cols-[auto_auto_1fr_auto_150px_100px] gap-4 items-center hover:shadow-md transition-all cursor-pointer",
            snapshot.isDragging && "shadow-lg ring-2 ring-blue-500",
            (task.archived || isProjectArchived) && "opacity-60"
          )}
        >
          {/* Type Icon */}
          <TypeIcon className="h-4 w-4 text-muted-foreground" />

          {/* Task Key */}
          <span className="text-sm font-medium text-muted-foreground">
            {projectKey}-{task.id}
          </span>

          {/* Title & Priority */}
          <div className="flex items-center gap-2 min-w-0">
            <PriorityIcon className={cn("h-4 w-4 flex-shrink-0", priority.color)} />
            <span className="text-sm font-medium truncate">{task.title}</span>
          </div>

          {/* Story Points - Editable */}
          <Input
            type="number"
            min="0"
            max="100"
            value={task.storyPoints || ""}
            onChange={(e) => {
              const value = e.target.value ? parseInt(e.target.value) : null;
              onUpdateStoryPoints(task.id, value);
            }}
            onClick={(e) => e.stopPropagation()}
            className="w-16 h-8 text-xs text-center"
            placeholder="SP"
            disabled={task.archived || isProjectArchived}
          />

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

          {/* Priority Badge */}
          <div className="text-xs text-muted-foreground">
            {task.priority}
          </div>
        </div>
      )}
    </Draggable>
  );
}
