import { Draggable } from "@hello-pangea/dnd";
import { useSearchParams } from "react-router-dom";
import { cn } from "@/lib/utils";
import { AlertCircle, ArrowUp, ArrowDown, Minus } from "lucide-react";
import { canMoveTask } from "@/lib/rbac";
import { useAuth } from "@/context/AuthContext";
import { useProject } from "@/context/ProjectContext";
import type { BoardTask, TaskStatus } from "@/hooks/useProjectBoard";

interface IssueCardProps {
  task: BoardTask;
  index: number;
  projectKey?: string;
  isProjectArchived?: boolean;
  canMove?: boolean;
}

const priorityConfig = {
  CRITICAL: {
    icon: AlertCircle,
    color: "text-red-600",
    bg: "bg-red-50",
    label: "Critical",
  },
  HIGH: {
    icon: ArrowUp,
    color: "text-orange-600",
    bg: "bg-orange-50",
    label: "High",
  },
  MEDIUM: {
    icon: Minus,
    color: "text-yellow-600",
    bg: "bg-yellow-50",
    label: "Medium",
  },
  LOW: {
    icon: ArrowDown,
    color: "text-blue-600",
    bg: "bg-blue-50",
    label: "Low",
  },
};

export default function IssueCard({ task, index, projectKey = "PROJ", isProjectArchived, canMove = true }: IssueCardProps) {
  const [searchParams, setSearchParams] = useSearchParams();
  const { user } = useAuth();
  const { project, isLoading: isLoadingProject } = useProject();
  const priority = priorityConfig[task.priority as keyof typeof priorityConfig] || priorityConfig.MEDIUM;
  const PriorityIcon = priority.icon;

  // Get user role from project context - eliminates prop drilling
  // Respect board-level move restriction and avoid flicker while project roles load
  const userRole = project?.members?.find(m => m.userId === user?.id)?.role || "VIEWER";
  const roleAllowsMove = isLoadingProject || canMoveTask(userRole as any);
  const canDrag = canMove && roleAllowsMove && !task.archived && !isProjectArchived;

  const handleClick = () => {
    const next = new URLSearchParams(searchParams);
    next.delete("create");
    next.set("selectedIssue", task.id.toString());
    setSearchParams(next);
  };

  return (
    <Draggable draggableId={`task-${task.id}`} index={index} isDragDisabled={!canDrag}>
      {(provided, snapshot) => (
        <div
          ref={provided.innerRef}
          {...provided.draggableProps}
          {...provided.dragHandleProps}
          onClick={handleClick}
          className={cn(
            "bg-white rounded-lg border shadow-sm p-3 mb-2 cursor-pointer",
            "transition-all duration-200 ease-out",
            "hover:shadow-lg hover:scale-[1.02] hover:border-blue-300",
            "will-change-transform", // GPU acceleration for smoother animations
            snapshot.isDragging && "scale-105 opacity-90 shadow-xl ring-2 ring-blue-400 rotate-2 transition-none",
            task.archived && "opacity-60",
            !canDrag && "cursor-not-allowed opacity-50"
          )}
        >
          {/* Issue Key */}
          <div className="flex items-center justify-between mb-2">
            <span
              className={cn(
                "text-xs font-medium text-muted-foreground",
                (task.status as TaskStatus) === "DONE" && "line-through"
              )}
            >
              {projectKey}-{task.id}
            </span>
            <div className={cn("flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium", priority.bg, priority.color)}>
              <PriorityIcon className="h-3 w-3" />
              <span className="hidden sm:inline">{priority.label}</span>
            </div>
          </div>

          {/* Task Title */}
          <h4 className="text-sm font-medium text-foreground mb-2 line-clamp-2">
            {task.title}
          </h4>

          {/* Footer */}
          <div className="flex items-center justify-between mt-3">
            {task.storyPoints && (
              <span className="text-xs text-muted-foreground">
                {task.storyPoints}h
              </span>
            )}
            
            {task.assignee ? (
              <div className="flex items-center gap-2 ml-auto">
                <div className="h-6 w-6 rounded-full bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center text-xs font-semibold text-white">
                  {task.assignee.name.charAt(0).toUpperCase()}
                </div>
              </div>
            ) : (
              <div className="h-6 w-6 rounded-full bg-gray-200 flex items-center justify-center ml-auto">
                <span className="text-xs text-gray-400">?</span>
              </div>
            )}
          </div>
        </div>
      )}
    </Draggable>
  );
}
