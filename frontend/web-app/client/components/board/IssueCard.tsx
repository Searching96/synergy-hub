import { Draggable } from "@hello-pangea/dnd";
import { useSearchParams } from "react-router-dom";
import { cn } from "@/lib/utils";
import { AlertCircle, ArrowUp, ArrowDown, Minus } from "lucide-react";
import type { BoardTask } from "@/hooks/useProjectBoard";

interface IssueCardProps {
  task: BoardTask;
  index: number;
  projectKey?: string;
  isProjectArchived?: boolean;
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

export default function IssueCard({ task, index, projectKey = "PROJ", isProjectArchived }: IssueCardProps) {
  const [searchParams, setSearchParams] = useSearchParams();
  const priority = priorityConfig[task.priority as keyof typeof priorityConfig] || priorityConfig.MEDIUM;
  const PriorityIcon = priority.icon;

  const handleClick = () => {
    setSearchParams({ selectedIssue: task.id.toString() });
  };

  return (
    <Draggable draggableId={`task-${task.id}`} index={index} isDragDisabled={task.archived || isProjectArchived}>
      {(provided, snapshot) => (
        <div
          ref={provided.innerRef}
          {...provided.draggableProps}
          {...provided.dragHandleProps}
          onClick={handleClick}
          className={cn(
            "bg-white rounded-lg border shadow-sm p-3 mb-2 cursor-pointer transition-all hover:shadow-md",
            snapshot.isDragging && "shadow-lg ring-2 ring-blue-500 rotate-2",
            task.archived && "opacity-60"
          )}
        >
          {/* Issue Key */}
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-medium text-muted-foreground">
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
