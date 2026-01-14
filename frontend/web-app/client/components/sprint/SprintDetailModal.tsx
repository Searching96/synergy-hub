import { useQuery } from "@tanstack/react-query";
import { sprintService } from "@/services/sprint.service";
import type { SprintDetails } from "@/types/sprint.types";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Calendar, Target, TrendingDown, CheckCircle2 } from "lucide-react";
import { cn } from "@/lib/utils";

interface SprintDetailModalProps {
  sprintId: number | null;
  onClose: () => void;
}

const STATUS_COLORS = {
  TODO: "bg-gray-100 text-gray-700",
  IN_PROGRESS: "bg-blue-100 text-blue-700",
  IN_REVIEW: "bg-purple-100 text-purple-700",
  DONE: "bg-green-100 text-green-700",
  BLOCKED: "bg-red-100 text-red-700",
};

const PRIORITY_COLORS = {
  LOW: "bg-gray-100 text-gray-700",
  MEDIUM: "bg-blue-100 text-blue-700",
  HIGH: "bg-orange-100 text-orange-700",
  CRITICAL: "bg-red-100 text-red-700",
};

export default function SprintDetailModal({ sprintId, onClose }: SprintDetailModalProps) {
  const { data: sprint, isLoading } = useQuery<SprintDetails | undefined>({
    queryKey: ["sprint-details", sprintId],
    queryFn: async () => {
      const response = await sprintService.getSprintDetails(sprintId!);
      return response.data;
    },
    enabled: !!sprintId,
  });

  const tasks = sprint?.tasks || [];
  const burndown = sprint?.burndownData;

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  const getDaysRemaining = (endDate: string) => {
    const end = new Date(endDate);
    const now = new Date();
    const diffTime = end.getTime() - now.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays < 0) {
      return `${Math.abs(diffDays)} days overdue`;
    }
    return `${diffDays} days remaining`;
  };

  return (
    <Dialog open={!!sprintId} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-5xl max-h-[90vh] p-0 gap-0 flex flex-col">
        {isLoading ? (
          <div className="p-6 space-y-4">
            <Skeleton className="h-8 w-64" />
            <Skeleton className="h-4 w-96" />
            <div className="grid grid-cols-3 gap-4 mt-6">
              <Skeleton className="h-24" />
              <Skeleton className="h-24" />
              <Skeleton className="h-24" />
            </div>
          </div>
        ) : !sprint ? (
          <div className="p-6">
            <p className="text-muted-foreground">Sprint not found</p>
          </div>
        ) : (
          <>
            {/* Header */}
            <DialogHeader className="px-6 py-4 border-b">
              <div className="flex items-start justify-between">
                <div>
                  <DialogTitle className="text-2xl font-bold mb-2">{sprint.name}</DialogTitle>
                  {sprint.goal && (
                    <p className="text-sm text-muted-foreground flex items-center gap-2">
                      <Target className="h-4 w-4" />
                      {sprint.goal}
                    </p>
                  )}
                </div>
                <Badge
                  className={cn(
                    "text-sm",
                    sprint.status === "ACTIVE" && "bg-green-100 text-green-700",
                    sprint.status === "COMPLETED" && "bg-blue-100 text-blue-700",
                    sprint.status === "PLANNED" && "bg-gray-100 text-gray-700"
                  )}
                >
                  {sprint.status}
                </Badge>
              </div>
            </DialogHeader>

            <ScrollArea className="flex-1">
              <div className="p-6 space-y-6">
                {/* Sprint Stats */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <Card>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-sm font-medium flex items-center gap-2">
                        <Calendar className="h-4 w-4" />
                        Timeline
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-1">
                        <p className="text-xs text-muted-foreground">Start</p>
                        <p className="text-sm font-medium">{formatDate(sprint.startDate)}</p>
                        <p className="text-xs text-muted-foreground mt-2">End</p>
                        <p className="text-sm font-medium">{formatDate(sprint.endDate)}</p>
                        {sprint.status === "ACTIVE" && (
                          <p className="text-xs text-blue-600 mt-2">
                            {getDaysRemaining(sprint.endDate)}
                          </p>
                        )}
                      </div>
                    </CardContent>
                  </Card>

                  <Card>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-sm font-medium flex items-center gap-2">
                        <CheckCircle2 className="h-4 w-4" />
                        Progress
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-2">
                        <div className="flex justify-between text-sm">
                          <span className="text-muted-foreground">Completed</span>
                          <span className="font-medium">{sprint.completedTaskCount || 0}</span>
                        </div>
                        <div className="flex justify-between text-sm">
                          <span className="text-muted-foreground">Total</span>
                          <span className="font-medium">{sprint.taskCount || 0}</span>
                        </div>
                        <div className="mt-3">
                          <div className="flex justify-between text-xs mb-1">
                            <span className="text-muted-foreground">Completion</span>
                            <span className="font-medium">{sprint.progress || 0}%</span>
                          </div>
                          <div className="w-full bg-gray-200 rounded-full h-2">
                            <div
                              className="bg-green-600 h-2 rounded-full"
                              style={{ width: `${sprint.progress || 0}%` }}
                            />
                          </div>
                        </div>
                      </div>
                    </CardContent>
                  </Card>

                  {burndown && (
                    <Card>
                      <CardHeader className="pb-3">
                        <CardTitle className="text-sm font-medium flex items-center gap-2">
                          <TrendingDown className="h-4 w-4" />
                          Burndown
                        </CardTitle>
                      </CardHeader>
                      <CardContent>
                        <div className="space-y-2">
                          <div className="flex justify-between text-sm">
                            <span className="text-muted-foreground">Total Points</span>
                            <span className="font-medium">{burndown.totalPoints}</span>
                          </div>
                          <div className="flex justify-between text-sm">
                            <span className="text-muted-foreground">Remaining</span>
                            <span className="font-medium">{burndown.remainingPoints}</span>
                          </div>
                          <div className="flex justify-between text-sm">
                            <span className="text-muted-foreground">Burned</span>
                            <span className="font-medium text-green-600">
                              {burndown.totalPoints - burndown.remainingPoints}
                            </span>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  )}
                </div>

                {/* Tasks List */}
                <Card>
                  <CardHeader>
                    <CardTitle>Tasks ({tasks.length})</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {tasks.length === 0 ? (
                      <p className="text-sm text-muted-foreground">No tasks in this sprint</p>
                    ) : (
                      <div className="space-y-2">
                        {tasks.map((task: any) => (
                          <div
                            key={task.id}
                            className="flex items-center justify-between p-3 border rounded-lg hover:bg-gray-50"
                          >
                            <div className="flex items-center gap-3 flex-1">
                              <CheckCircle2
                                className={cn(
                                  "h-4 w-4 flex-shrink-0",
                                  task.status === "DONE" ? "text-green-600" : "text-gray-400"
                                )}
                              />
                              <div className="flex-1 min-w-0">
                                <p className={cn(
                                  "text-sm font-medium",
                                  task.status === "DONE" && "line-through text-muted-foreground"
                                )}>
                                  {task.title}
                                </p>
                                {task.assignee && (
                                  <p className="text-xs text-muted-foreground mt-1">
                                    Assigned to {task.assignee.name}
                                  </p>
                                )}
                              </div>
                            </div>
                            <div className="flex items-center gap-2">
                              {task.priority && (
                                <Badge
                                  variant="outline"
                                  className={cn(
                                    "text-xs",
                                    PRIORITY_COLORS[task.priority as keyof typeof PRIORITY_COLORS]
                                  )}
                                >
                                  {task.priority}
                                </Badge>
                              )}
                              <Badge
                                variant="secondary"
                                className={cn(
                                  "text-xs",
                                  STATUS_COLORS[task.status as keyof typeof STATUS_COLORS] || STATUS_COLORS.TODO
                                )}
                              >
                                {task.status}
                              </Badge>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </CardContent>
                </Card>
              </div>
            </ScrollArea>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
