import { Droppable } from "@hello-pangea/dnd";
import { cn } from "@/lib/utils";
import IssueCard from "./IssueCard";
import type { BoardTask, TaskStatus } from "@/hooks/useProjectBoard";

interface BoardColumnProps {
  status: TaskStatus;
  title: string;
  tasks: BoardTask[];
  projectKey?: string;
  isProjectArchived?: boolean;
}

export default function BoardColumn({ status, title, tasks, projectKey, isProjectArchived }: BoardColumnProps) {
  return (
    <div className="flex flex-col min-w-[280px] w-[280px] h-full">
      {/* Column Header */}
      <div className="flex items-center justify-between mb-3 px-2">
        <h3 className="text-sm font-semibold text-foreground uppercase tracking-wide">
          {title}
        </h3>
        <span className="text-xs font-medium text-muted-foreground bg-muted rounded-full px-2 py-0.5">
          {tasks.length}
        </span>
      </div>

      {/* Droppable Area */}
      <Droppable droppableId={status}>
        {(provided, snapshot) => (
          <div
            ref={provided.innerRef}
            {...provided.droppableProps}
            className={cn(
              "flex-1 bg-gray-50 rounded-lg p-2 transition-colors min-h-[200px]",
              snapshot.isDraggingOver && "bg-blue-50 ring-2 ring-blue-300"
            )}
          >
            {tasks.length === 0 ? (
              <div className="flex items-center justify-center h-32 text-sm text-muted-foreground">
                No tasks
              </div>
            ) : (
              tasks.map((task, index) => (
                <IssueCard
                  key={task.id}
                  task={task}
                  index={index}
                  projectKey={projectKey}
                  isProjectArchived={isProjectArchived}
                />
              ))
            )}
            {provided.placeholder}
          </div>
        )}
      </Droppable>
    </div>
  );
}
