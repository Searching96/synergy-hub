import { useState } from "react";
import { useParams } from "react-router-dom";
import { useProjectSprints, useStartSprint } from "@/hooks/useSprints";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { toast } from "sonner";
import { Calendar, Target, Play, Loader2 } from "lucide-react";

interface SprintListDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export default function SprintListDialog({ open, onOpenChange }: SprintListDialogProps) {
  const { projectId } = useParams<{ projectId: string }>();
  const { data: sprints, isLoading } = useProjectSprints(projectId);
  const startSprint = useStartSprint(projectId);

  const [startingSprintId, setStartingSprintId] = useState<number | null>(null);

  const handleStartSprint = async (sprintId: number, sprintName: string) => {
    try {
      setStartingSprintId(sprintId);
      await startSprint.mutateAsync(sprintId);
      toast.success(`${sprintName} started successfully`);
      onOpenChange(false);
    } catch (error: any) {
      const errorMessage = error?.response?.data?.error || error?.response?.data?.message || "Failed to start sprint";
      toast.error(errorMessage);
      console.error(error);
    } finally {
      setStartingSprintId(null);
    }
  };

  const statusColors: Record<string, string> = {
    PLANNING: "bg-gray-100 text-gray-700",
    PLANNED: "bg-gray-100 text-gray-700",
    ACTIVE: "bg-green-100 text-green-700",
    COMPLETED: "bg-blue-100 text-blue-700",
    CANCELLED: "bg-red-100 text-red-700",
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px] max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>All Sprints</DialogTitle>
          <DialogDescription>
            View and manage all sprints in this project
          </DialogDescription>
        </DialogHeader>

        {isLoading ? (
          <div className="flex justify-center py-8">
            <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
          </div>
        ) : !sprints || sprints.length === 0 ? (
          <div className="text-center py-8 text-muted-foreground">
            No sprints found. Create a sprint to get started.
          </div>
        ) : (
          <div className="space-y-3">
            {sprints.map((sprint) => {
              const normalizedStatus = sprint.status === "PLANNING" ? "PLANNED" : sprint.status;
              const badgeClass = statusColors[normalizedStatus] || statusColors.PLANNING;

              const isValidDate = (dateString?: string) => {
                if (!dateString) return false;
                const d = new Date(dateString);
                return d instanceof Date && !isNaN(d.getTime());
              };

              return (
                <div
                  key={sprint.id}
                  className="border rounded-lg p-4 space-y-3"
                >
                  <div className="flex items-start justify-between">
                    <div className="space-y-1 flex-1">
                      <div className="flex items-center gap-2">
                        <h3 className="font-semibold">{sprint.name}</h3>
                        <Badge className={badgeClass}>
                          {normalizedStatus}
                        </Badge>
                      </div>
                      {sprint.goal && (
                        <p className="text-sm text-muted-foreground flex items-start gap-2">
                          <Target className="h-4 w-4 mt-0.5 flex-shrink-0" />
                          {sprint.goal}
                        </p>
                      )}
                    </div>

                    {(normalizedStatus === "PLANNED") && (
                      <Button
                        size="sm"
                        onClick={() => handleStartSprint(sprint.id, sprint.name)}
                        disabled={startSprint.isPending || startingSprintId !== null}
                      >
                        {startSprint.isPending ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                          <>
                            <Play className="h-4 w-4 mr-1" />
                            Start
                          </>
                        )}
                      </Button>
                    )}
                  </div>

                  <div className="flex items-center gap-4 text-sm text-muted-foreground">
                    <div className="flex items-center gap-1">
                      <Calendar className="h-4 w-4" />
                      {isValidDate(sprint.startDate) && isValidDate(sprint.endDate) ? (
                        `${new Date(sprint.startDate!).toLocaleDateString()} - ${new Date(sprint.endDate!).toLocaleDateString()}`
                      ) : (
                        "No dates set"
                      )}
                    </div>
                    {sprint.taskCount !== undefined && (
                      <div>
                        {sprint.completedTaskCount || 0} / {sprint.taskCount} tasks completed
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
