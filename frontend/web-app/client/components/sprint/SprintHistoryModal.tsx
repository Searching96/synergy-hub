import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { sprintService } from "@/services/sprint.service";
import type { Sprint } from "@/types/sprint.types";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Calendar, Eye, TrendingUp, CheckCircle2 } from "lucide-react";
import { cn } from "@/lib/utils";
import SprintDetailModal from "./SprintDetailModal";

interface SprintHistoryModalProps {
  projectId: number | null;
  onClose: () => void;
}

export default function SprintHistoryModal({ projectId, onClose }: SprintHistoryModalProps) {
  const [selectedSprintId, setSelectedSprintId] = useState<number | null>(null);

  const { data: sprints = [], isLoading } = useQuery<Sprint[]>({
    queryKey: ["completed-sprints", projectId],
    queryFn: async () => {
      const response = await sprintService.getCompletedSprints(projectId!);
      return response.data;
    },
    enabled: !!projectId,
  });

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  const getDuration = (startDate: string, endDate: string) => {
    const start = new Date(startDate);
    const end = new Date(endDate);
    const diffTime = Math.abs(end.getTime() - start.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return `${diffDays} days`;
  };

  const [isHistoryOpen, setIsHistoryOpen] = useState(true);

  // Reset history open state when projectId changes (new session)
  // useEffect(() => setIsHistoryOpen(true), [projectId]); 

  return (
    <>
      <Dialog
        open={!!projectId && isHistoryOpen && !selectedSprintId}
        onOpenChange={(open) => {
          if (!open) {
            setIsHistoryOpen(false);
            onClose();
          }
        }}
      >
        <DialogContent className="max-w-4xl max-h-[90vh] p-0 gap-0 flex flex-col">
          <DialogHeader className="px-6 py-4 border-b">
            <DialogTitle className="text-2xl font-bold">Sprint History</DialogTitle>
            <p className="text-sm text-muted-foreground">
              View past sprint performance and metrics
            </p>
          </DialogHeader>

          <ScrollArea className="flex-1 p-6">
            {isLoading ? (
              <div className="space-y-4">
                {[1, 2, 3].map((i) => (
                  <Skeleton key={i} className="h-32" />
                ))}
              </div>
            ) : sprints.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
                <Calendar className="h-12 w-12 mb-4 opacity-50" />
                <p>No completed sprints yet</p>
                <p className="text-sm">Completed sprints will appear here</p>
              </div>
            ) : (
              <div className="space-y-4">
                {sprints.map((sprint: any) => {
                  const completionRate =
                    typeof sprint.completedTaskCount === 'number' &&
                      typeof sprint.taskCount === 'number' &&
                      sprint.taskCount > 0
                      ? Math.round((sprint.completedTaskCount / sprint.taskCount) * 100)
                      : 0;

                  return (
                    <Card key={sprint.id} className="p-4 hover:shadow-md transition-shadow">
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <div className="flex items-center gap-3 mb-2">
                            <h3 className="text-lg font-semibold">{sprint.name}</h3>
                            <Badge
                              className={cn(
                                "text-xs",
                                sprint.status === "COMPLETED" && "bg-blue-100 text-blue-700",
                                sprint.status === "CANCELLED" && "bg-gray-100 text-gray-700"
                              )}
                            >
                              {sprint.status}
                            </Badge>
                          </div>

                          {sprint.goal && (
                            <p className="text-sm text-muted-foreground mb-3">{sprint.goal}</p>
                          )}

                          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                            <div>
                              <p className="text-muted-foreground text-xs mb-1">Duration</p>
                              <p className="font-medium">{getDuration(sprint.startDate, sprint.endDate)}</p>
                            </div>
                            <div>
                              <p className="text-muted-foreground text-xs mb-1">Date Range</p>
                              <p className="font-medium">
                                {formatDate(sprint.startDate)} - {formatDate(sprint.endDate)}
                              </p>
                            </div>
                            <div>
                              <p className="text-muted-foreground text-xs mb-1">Tasks</p>
                              <p className="font-medium flex items-center gap-1">
                                <CheckCircle2 className="h-3 w-3 text-green-600" />
                                {sprint.completedTaskCount || 0} / {sprint.taskCount || 0}
                              </p>
                            </div>
                            <div>
                              <p className="text-muted-foreground text-xs mb-1">Completion</p>
                              <p className="font-medium flex items-center gap-1">
                                <TrendingUp className={cn(
                                  "h-3 w-3",
                                  completionRate >= 80 ? "text-green-600" :
                                    completionRate >= 50 ? "text-yellow-600" : "text-red-600"
                                )} />
                                {completionRate}%
                              </p>
                            </div>
                          </div>

                          {/* Progress Bar */}
                          <div className="mt-3">
                            <div className="w-full bg-gray-200 rounded-full h-2">
                              <div
                                className={cn(
                                  "h-2 rounded-full",
                                  completionRate >= 80 ? "bg-green-600" :
                                    completionRate >= 50 ? "bg-yellow-600" : "bg-red-600"
                                )}
                                style={{ width: `${completionRate}%` }}
                              />
                            </div>
                          </div>
                        </div>

                        <Button
                          variant="outline"
                          size="sm"
                          className="ml-4"
                          onClick={() => setSelectedSprintId(sprint.id)}
                        >
                          <Eye className="h-4 w-4 mr-1" />
                          View Details
                        </Button>
                      </div>
                    </Card>
                  );
                })}
              </div>
            )}
          </ScrollArea>
        </DialogContent>
      </Dialog>

      {/* Sprint Detail Modal */}
      {selectedSprintId && (
        <SprintDetailModal
          sprintId={selectedSprintId}
          onClose={() => {
            setSelectedSprintId(null);
            setIsHistoryOpen(true);
          }}
        />
      )}
    </>
  );
}
