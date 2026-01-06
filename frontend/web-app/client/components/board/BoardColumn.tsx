import { Droppable } from "@hello-pangea/dnd";
import { cn } from "@/lib/utils";
import { Skeleton } from "@/components/ui/skeleton";
import IssueCard from "./IssueCard";
import type { BoardTask, TaskStatus } from "@/hooks/useProjectBoard";

interface BoardColumnProps {
  status: TaskStatus;
  title: string;
  tasks: BoardTask[];
  projectKey?: string;
  isProjectArchived?: boolean;
  isLoading?: boolean;
  canMove?: boolean;
}

export default function BoardColumn({ 
  status, 
  title, 
  tasks, 
  projectKey, 
  isProjectArchived,
  isLoading = false,
  canMove = true,
}: BoardColumnProps) {
  return (
    <div className="flex flex-col min-w-board-column md:min-w-board-column-md w-full md:w-board-column-md">
      {/* Column Header */}
      <div className="flex items-center justify-between mb-3 px-2">
        <div className="flex items-center gap-2">
          <h3 className="text-sm font-semibold text-foreground uppercase tracking-wide">
            {title}
          </h3>
          {!canMove && (
            <span className="text-[11px] font-medium text-amber-700 bg-amber-100 rounded-full px-2 py-0.5">
              Move restricted
            </span>
          )}
        </div>
        <span className="text-xs font-medium text-muted-foreground bg-muted rounded-full px-2 py-0.5">
          {tasks.length}
        </span>
      </div>

      {/* Droppable Area */}
      <Droppable droppableId={status} isDropDisabled={!canMove}>
        {(provided, snapshot) => (
          <div
            ref={provided.innerRef}
            {...provided.droppableProps}
            className={cn(
              "flex-1 min-h-64 bg-gray-50 rounded-lg p-2",
              "transition-all duration-300 ease-in-out", // Smooth height transitions\n              snapshot.isDraggingOver && "bg-blue-50 ring-2 ring-blue-300 scale-[1.01]",
              isLoading && "opacity-80 pointer-events-none" // Show network call in progress
            )}
            style={{
              minHeight: tasks.length > 0 ? `${tasks.length * 88 + 64}px` : '256px',
              transition: 'min-height 0.3s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.2s, transform 0.2s',
            }}
          >
            {isLoading ? (
              // Show skeleton overlays on existing cards for smooth transitions
              tasks.length > 0 ? (
                tasks.map((task, index) => (
                  <div key={task.id} className="relative mb-2">
                    <IssueCard
                      task={task}
                      index={index}
                      projectKey={projectKey}
                      isProjectArchived={isProjectArchived}
                      canMove={false}
                    />
                    <div className="absolute inset-0 bg-white/70 backdrop-blur-[2px] rounded-lg animate-pulse pointer-events-none" />
                  </div>
                ))
              ) : (
                <div className="space-y-2">
                  <Skeleton className="h-20 w-full rounded-lg" />
                  <Skeleton className="h-24 w-full rounded-lg" />
                  <Skeleton className="h-20 w-full rounded-lg" />
                </div>
              )
            ) : tasks.length === 0 ? (
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
                    canMove={canMove}
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
