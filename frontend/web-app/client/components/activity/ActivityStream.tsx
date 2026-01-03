import { useQuery } from "@tanstack/react-query";
import { activityService } from "@/services/activity.service";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  Activity,
  CheckCircle2,
  FileText,
  GitCommit,
  Layers,
  MessageSquare,
  Users,
  Calendar,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { ActivityEvent } from "@/types/activity.types";
import type { ApiResponse } from "@/types/auth.types";

interface ActivityStreamProps {
  projectId: number;
  showTitle?: boolean;
  height?: string;
}

const EVENT_ICONS: Record<string, any> = {
  TASK_CREATED: FileText,
  TASK_UPDATED: FileText,
  TASK_DELETED: FileText,
  TASK_ASSIGNED: Users,
  TASK_STATUS_CHANGED: CheckCircle2,
  SPRINT_CREATED: Layers,
  SPRINT_STARTED: Layers,
  SPRINT_COMPLETED: Layers,
  MEMBER_ADDED: Users,
  MEMBER_REMOVED: Users,
  ROLE_CHANGED: Users,
  COMMENT_ADDED: MessageSquare,
  COMMENT_EDITED: MessageSquare,
  COMMENT_DELETED: MessageSquare,
  PROJECT_UPDATED: GitCommit,
  PROJECT_DELETED: GitCommit,
};

const EVENT_COLORS: Record<string, string> = {
  TASK_CREATED: "text-blue-600 bg-blue-50",
  TASK_UPDATED: "text-gray-600 bg-gray-50",
  TASK_DELETED: "text-red-600 bg-red-50",
  TASK_ASSIGNED: "text-purple-600 bg-purple-50",
  TASK_STATUS_CHANGED: "text-green-600 bg-green-50",
  SPRINT_CREATED: "text-indigo-600 bg-indigo-50",
  SPRINT_STARTED: "text-green-600 bg-green-50",
  SPRINT_COMPLETED: "text-blue-600 bg-blue-50",
  MEMBER_ADDED: "text-purple-600 bg-purple-50",
  MEMBER_REMOVED: "text-orange-600 bg-orange-50",
  ROLE_CHANGED: "text-yellow-600 bg-yellow-50",
  COMMENT_ADDED: "text-blue-600 bg-blue-50",
  PROJECT_UPDATED: "text-gray-600 bg-gray-50",
};

const getInitials = (name: string) => {
  return name
    .split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);
};

const formatTimestamp = (timestamp: string) => {
  const date = new Date(timestamp);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return "just now";
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays < 7) return `${diffDays}d ago`;
  return date.toLocaleDateString();
};

export default function ActivityStream({
  projectId,
  showTitle = true,
  height = "600px",
}: ActivityStreamProps) {
  const { data: activityResponse, isLoading } = useQuery<ApiResponse<ActivityEvent[]>>({
    queryKey: ["activity", projectId],
    queryFn: () => activityService.getProjectActivity(projectId, 0, 50),
    enabled: !!projectId,
    refetchInterval: 30000, // Refetch every 30 seconds
  });

  const activities = activityResponse?.data || [];

  if (isLoading) {
    return (
      <Card>
        {showTitle && (
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Activity className="h-5 w-5" />
              Activity Stream
            </CardTitle>
          </CardHeader>
        )}
        <CardContent>
          <div className="space-y-4">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="flex gap-3">
                <Skeleton className="h-10 w-10 rounded-full" />
                <div className="flex-1 space-y-2">
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="h-3 w-1/4" />
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  if (!activities || activities.length === 0) {
    return (
      <Card>
        {showTitle && (
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Activity className="h-5 w-5" />
              Activity Stream
            </CardTitle>
          </CardHeader>
        )}
        <CardContent>
          <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
            <Activity className="h-12 w-12 mb-4 opacity-50" />
            <p>No activity yet</p>
            <p className="text-sm">Project activity will appear here</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      {showTitle && (
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Activity className="h-5 w-5" />
            Activity Stream
          </CardTitle>
        </CardHeader>
      )}
      <CardContent className="p-0">
        <ScrollArea style={{ height }}>
          <div className="p-6 space-y-4">
            {activities.map((activity: ActivityEvent) => {
              const Icon = EVENT_ICONS[activity.eventType] || Activity;
              const colorClass = EVENT_COLORS[activity.eventType] || "text-gray-600 bg-gray-50";
              const actorName = activity.actorName || activity.user?.name || "System";
              const isSystemEvent = activity.systemEvent || !activity.actorId;

              return (
                <div key={activity.id} className="flex gap-3 group">
                  {isSystemEvent ? (
                    <div className={cn(
                      "h-10 w-10 rounded-full flex items-center justify-center flex-shrink-0",
                      colorClass
                    )}>
                      <Icon className="h-5 w-5" />
                    </div>
                  ) : (
                    <Avatar className="h-10 w-10 flex-shrink-0">
                      <AvatarFallback className="text-sm">
                        {getInitials(actorName)}
                      </AvatarFallback>
                    </Avatar>
                  )}

                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex-1 min-w-0">
                        <p className="text-sm">
                          {!isSystemEvent && (
                            <span className="font-medium">{actorName}</span>
                          )}
                          {!isSystemEvent && " "}
                          <span className="text-muted-foreground">
                            {activity.eventDetails || activity.description || formatEventType(activity.eventType)}
                          </span>
                        </p>
                        {activity.metadata && (
                          <div className="mt-1 flex flex-wrap gap-2">
                            {activity.metadata.taskTitle && (
                              <Badge variant="outline" className="text-xs">
                                {activity.metadata.taskTitle}
                              </Badge>
                            )}
                            {activity.metadata.taskStatus && (
                              <Badge variant="secondary" className="text-xs">
                                {activity.metadata.taskStatus}
                              </Badge>
                            )}
                          </div>
                        )}
                      </div>
                      <span className="text-xs text-muted-foreground whitespace-nowrap">
                        {formatTimestamp(activity.timestamp)}
                      </span>
                    </div>
                    {activity.ipAddress && (
                      <p className="text-xs text-muted-foreground mt-1">
                        {activity.ipAddress}
                      </p>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </ScrollArea>
      </CardContent>
    </Card>
  );
}

function formatEventType(eventType: string): string {
  return eventType
    .split("_")
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(" ");
}
