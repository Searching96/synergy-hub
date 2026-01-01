import { useState } from "react";
import { Droppable } from "@hello-pangea/dnd";
import { cn } from "@/lib/utils";
import { ChevronDown, ChevronRight, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import BacklogTaskRow from "./BacklogTaskRow";
import type { BacklogTask } from "@/hooks/useBacklog";

interface BacklogSectionProps {
  title: string;
  droppableId: string;
  tasks: BacklogTask[];
  projectKey?: string;
  collapsible?: boolean;
  showCompleteButton?: boolean;
  onComplete?: () => void;
  members?: Array<{ id: number; name: string; email: string }>;
  onUpdateStoryPoints: (taskId: number, storyPoints: number | null) => void;
  onUpdateAssignee: (taskId: number, assigneeId: number | null) => void;
  isProjectArchived?: boolean;
}

export default function BacklogSection({
  title,
  droppableId,
  tasks,
  projectKey,
  collapsible = false,
  showCompleteButton = false,
  onComplete,
  members,
  onUpdateStoryPoints,
  onUpdateAssignee,
  isProjectArchived,
}: BacklogSectionProps) {
  const [isCollapsed, setIsCollapsed] = useState(false);

  return (
    <div className="border rounded-lg bg-white">
      {/* Header */}
      <div
        className={cn(
          "flex items-center justify-between p-4 border-b",
          collapsible && "cursor-pointer hover:bg-gray-50"
        )}
        onClick={() => collapsible && setIsCollapsed(!isCollapsed)}
      >
        <div className="flex items-center gap-2">
          {collapsible && (
            isCollapsed ? (
              <ChevronRight className="h-4 w-4" />
            ) : (
              <ChevronDown className="h-4 w-4" />
            )
          )}
          <h2 className="text-lg font-semibold">{title}</h2>
          <span className="text-sm text-muted-foreground">
            ({tasks.length} {tasks.length === 1 ? "issue" : "issues"})
          </span>
        </div>

        {showCompleteButton && !isCollapsed && (
          <Button
            size="sm"
            variant="outline"
            onClick={(e) => {
              e.stopPropagation();
              onComplete?.();
            }}
          >
            <CheckCircle2 className="h-4 w-4 mr-2" />
            Complete Sprint
          </Button>
        )}
      </div>

      {/* Task List */}
      {!isCollapsed && (
        <Droppable droppableId={droppableId}>
          {(provided, snapshot) => (
            <div
              ref={provided.innerRef}
              {...provided.droppableProps}
              className={cn(
                "p-4 min-h-[200px]",
                snapshot.isDraggingOver && "bg-blue-50"
              )}
            >
              {tasks.length === 0 ? (
                <div className="flex items-center justify-center h-32 text-sm text-muted-foreground">
                  No tasks
                </div>
              ) : (
                tasks.map((task, index) => (
                  <BacklogTaskRow
                    key={task.id}
                    task={task}
                    index={index}
                    projectKey={projectKey}
                    members={members}
                    onUpdateStoryPoints={onUpdateStoryPoints}
                    onUpdateAssignee={onUpdateAssignee}
                    isProjectArchived={isProjectArchived}
                  />
                ))
              )}
              {provided.placeholder}
            </div>
          )}
        </Droppable>
      )}
    </div>
  );
}
