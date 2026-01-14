import { useState, useMemo } from "react";
import { useParams, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useProject } from "@/context/ProjectContext";
import { taskService } from "@/services/task.service";
import { sprintService } from "@/services/sprint.service";
import { ProjectBreadcrumb } from "@/components/project/ProjectBreadcrumb";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import {
  MoreHorizontal,
  Share2,
  Download,
  MessageSquare,
  SlidersHorizontal,
  Search,
  ChevronDown,
  ChevronRight,
  BookOpen,
  CheckSquare,
  Loader2,
} from "lucide-react";
import { format, differenceInDays, addMonths, startOfMonth } from "date-fns";
import { toast } from "@/hooks/use-toast";
import IssueModal from "@/components/issue/IssueModal";

export default function TimelinePage() {
  const { projectId } = useParams<{ projectId: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const { project } = useProject();
  const [collapsedEpics, setCollapsedEpics] = useState<Record<string, boolean>>({});

  // Fetch Epics
  const { data: epics = [], isLoading: isLoadingEpics } = useQuery({
    queryKey: ["epics", projectId],
    queryFn: async () => {
      if (!projectId) return [];
      const res = await taskService.getProjectEpics(projectId);
      return res.data || [];
    },
    enabled: !!projectId,
  });

  // Fetch all tasks to find children
  const { data: allTasks = [], isLoading: isLoadingTasks } = useQuery({
    queryKey: ["tasks", projectId],
    queryFn: async () => {
      if (!projectId) return [];
      const res = await taskService.getProjectTasks(projectId);
      const data = res.data;
      if (Array.isArray(data)) return data;
      return data && 'content' in data ? (data as any).content : [];
    },
    enabled: !!projectId,
  });

  // Fetch Sprints
  const { data: sprints = [] } = useQuery({
    queryKey: ["sprints", projectId],
    queryFn: async () => {
      if (!projectId) return [];
      const res = await sprintService.getProjectSprints(projectId);
      return res.data || [];
    },
    enabled: !!projectId,
  });

  // Search State
  const [searchQuery, setSearchQuery] = useState("");

  const toggleEpic = (epicId: number) => {
    setCollapsedEpics((prev) => ({
      ...prev,
      [epicId]: !prev[epicId],
    }));
  };

  // Group tasks by Epic and Filter
  const timelineData = useMemo(() => {
    if (!epics) return [];

    let filteredEpics = epics;

    return filteredEpics.map((epic) => {
      const children = allTasks.filter((task) => task.epicId === epic.id);

      // Filter logic
      const query = searchQuery.toLowerCase();
      const epicMatches = epic.title.toLowerCase().includes(query);
      const matchingChildren = children.filter(child => child.title.toLowerCase().includes(query));

      const hasMatchingChildren = matchingChildren.length > 0;

      if (searchQuery && !epicMatches && !hasMatchingChildren) {
        return null; // Hide epic entirely
      }

      const childrenToShow = (searchQuery && !epicMatches) ? matchingChildren : children;

      return {
        ...epic,
        children: childrenToShow,
        collapsed: !!collapsedEpics[epic.id],
      };
    }).filter(Boolean) as any[]; // Remove nulls
  }, [epics, allTasks, collapsedEpics, searchQuery]);

  // Determine timeline range
  const { startDate, endDate, totalDays } = useMemo(() => {
    const today = new Date();
    const start = startOfMonth(addMonths(today, -1)); // Start 1 month ago
    const end = addMonths(start, 4); // Show 4 months window
    const days = differenceInDays(end, start);
    return { startDate: start, endDate: end, totalDays: days };
  }, []);

  const getPosition = (dateStr: string | undefined | null, fallbackDate?: Date) => {
    if (!dateStr && !fallbackDate) return 0;
    const date = dateStr ? new Date(dateStr) : fallbackDate!;
    const diff = differenceInDays(date, startDate);
    const pos = (diff / totalDays) * 100;
    return Math.max(0, Math.min(100, pos));
  };

  const getWidth = (startStr: string | undefined | null, endStr: string | undefined | null, defaultDurationDays = 14) => {
    const start = startStr ? new Date(startStr) : new Date(); // Default to today if missing
    let end = endStr ? new Date(endStr) : undefined;

    if (!end) {
      end = new Date(start);
      end.setDate(end.getDate() + defaultDurationDays);
    }

    const startPos = getPosition(start.toISOString());
    const endPos = getPosition(end.toISOString());
    return Math.max(1, endPos - startPos); // Min width 1%
  };

  const months = useMemo(() => {
    const result = [];
    let current = new Date(startDate);
    while (current < endDate) {
      result.push(format(current, "MMM"));
      current = addMonths(current, 1);
    }
    return result;
  }, [startDate, endDate]);

  if (isLoadingEpics || isLoadingTasks) {
    return (
      <div className="flex justify-center items-center h-96">
        <Loader2 data-testid="timeline-loading" className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      <div className="mb-2">
        <ProjectBreadcrumb current="Timeline" />
      </div>

      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="space-y-1">
          <h1 className="text-3xl font-bold">Timeline</h1>
        </div>
        <div className="flex items-center gap-2 flex-wrap justify-end">
          <Button variant="ghost" size="sm" className="gap-2" onClick={() => toast({ title: "Feedback", description: "Coming soon!" })}>
            <MessageSquare className="h-4 w-4" />
            Give feedback
          </Button>
          <Button variant="outline" size="sm" className="gap-2" onClick={() => {
            navigator.clipboard.writeText(window.location.href);
            toast({ title: "Success", description: "Link copied to clipboard" });
          }}>
            <Share2 className="h-4 w-4" />
            Share
          </Button>
          <Button variant="outline" size="sm" className="gap-2" onClick={() => {
            const exportData = JSON.stringify(timelineData, null, 2);
            const blob = new Blob([exportData], { type: "application/json" });
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = `timeline-export-${new Date().toISOString().split('T')[0]}.json`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
            toast({ title: "Exported", description: "Timeline data exported to JSON" });
          }}>
            <Download className="h-4 w-4" />
            Export
          </Button>
          <Button variant="ghost" size="icon">
            <MoreHorizontal className="h-5 w-5" />
          </Button>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="flex flex-wrap items-center gap-3 bg-muted/60 border rounded-lg p-3">
        <div className="flex items-center gap-2 flex-1 min-w-[280px]">
          <div className="relative w-full">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search timeline"
              className="pl-9"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          {/* Mock Avatars */}
          <Avatar className="h-8 w-8 border-2 border-background">
            <AvatarFallback className="bg-orange-500 text-white text-xs">SG</AvatarFallback>
          </Avatar>
        </div>
        <div className="ml-auto flex items-center gap-2">
          <Button variant="outline" size="sm" className="gap-2" onClick={() => toast({ title: "Settings", description: "Coming soon!" })}>
            <SlidersHorizontal className="h-4 w-4" />
            View settings
          </Button>
        </div>
      </div>

      {/* Split Pane */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left: Task Tree */}
        <div className="lg:col-span-4 border rounded-lg bg-white overflow-hidden flex flex-col">
          <div className="flex items-center gap-2 px-4 py-3 border-b text-sm font-semibold bg-gray-50">
            <ChevronDown className="h-4 w-4" />
            Epics & Tasks
          </div>
          <div className="divide-y overflow-y-auto max-h-[600px]">
            {timelineData.length === 0 ? (
              <div className="p-8 text-center text-muted-foreground">No epics found</div>
            ) : timelineData.map((epic) => (
              <div key={epic.id}>
                {/* Epic Row */}
                <div
                  className="flex items-center gap-3 px-4 py-3 hover:bg-gray-50 cursor-pointer"
                  onClick={() => toggleEpic(epic.id)}
                >
                  {epic.collapsed ? (
                    <ChevronRight className="h-4 w-4 text-muted-foreground" />
                  ) : (
                    <ChevronDown className="h-4 w-4 text-muted-foreground" />
                  )}
                  <div className="h-2 w-2 rounded-full bg-purple-500 shrink-0" />
                  <div className="space-y-0.5 truncate">
                    <div className="text-sm font-semibold truncate">{epic.title}</div>
                  </div>
                </div>
                {/* Children Rows */}
                {!epic.collapsed && (
                  <div className="space-y-0 pb-1">
                    {epic.children.map((child: any) => (
                      <div key={child.id} className="flex items-center gap-3 pl-10 pr-4 py-2 hover:bg-gray-50 border-t border-dashed border-gray-100">
                        {child.type === "STORY" ? (
                          <BookOpen className="h-3 w-3 text-emerald-600 shrink-0" />
                        ) : (
                          <CheckSquare className="h-3 w-3 text-blue-600 shrink-0" />
                        )}
                        <div className="flex-1 truncate">
                          <div className="text-xs font-medium truncate">{child.title}</div>
                        </div>
                        <Badge variant="outline" className="text-[10px] h-5 px-1">{child.status}</Badge>
                      </div>
                    ))}
                    {epic.children.length === 0 && (
                      <div className="pl-10 pr-4 py-2 text-xs text-muted-foreground italic">No tasks</div>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
          <div className="px-4 py-3 border-t mt-auto bg-gray-50">
            <Button
              variant="ghost"
              size="sm"
              className="gap-2"
              onClick={() => setSearchParams({ create: "true", type: "EPIC" })}
            >
              + Create Epic
            </Button>
          </div>
        </div>

        {/* Right: Timeline Grid */}
        <div className="lg:col-span-8 border rounded-lg bg-white p-4 relative overflow-hidden flex flex-col h-[600px]">
          {/* Time Axis */}
          <div className="grid grid-cols-4 text-xs font-semibold text-muted-foreground mb-4 border-b pb-2">
            {months.map((m, i) => (
              <div key={i} className="text-center border-l first:border-l-0">{m}</div>
            ))}
          </div>

          <div className="relative flex-1 overflow-y-auto overflow-x-hidden">
            {/* Current time marker */}
            <div
              className="absolute top-0 bottom-0 w-px bg-orange-500 z-10"
              style={{ left: `${getPosition(new Date().toISOString())}%` }}
            />

            {/* Sprints (Background or Top) */}
            {sprints.map((sprint: any, idx: number) => (
              <div
                key={sprint.id}
                className="absolute h-6 bg-blue-100 rounded-sm text-[10px] text-blue-700 px-1 truncate border border-blue-200 opacity-50 z-0"
                style={{
                  top: "0px",
                  left: `${getPosition(sprint.startDate)}%`,
                  width: `${getWidth(sprint.startDate, sprint.endDate)}%`
                }}
              >
                {sprint.name}
              </div>
            ))}

            {/* Bars */}
            <div className="mt-8 space-y-1">
              {timelineData.map((epic) => (
                <div key={epic.id}>
                  {/* Epic Bar */}
                  <div className="h-[44px] relative mb-1"> {/* Matches Epic Row height approx */}
                    <Bar
                      left={`${getPosition(epic.createdAt || undefined, new Date())}%`} // Fallback to now if no date
                      width={`${getWidth(epic.createdAt, epic.dueDate)}%`} // Use due date if avail
                      color="bg-purple-200 border-purple-300"
                      label={epic.title}
                    />
                  </div>

                  {/* Children Bars */}
                  {!epic.collapsed && epic.children.map((child: any) => (
                    <div key={child.id} className="h-[37px] relative mb-0"> {/* Matches Child Row height approx */}
                      <Bar
                        left={`${getPosition(child.createdAt || undefined, new Date())}%`}
                        width={`${getWidth(child.createdAt, child.dueDate)}%`}
                        color={child.type === 'STORY' ? "bg-emerald-100 border-emerald-200" : "bg-blue-100 border-blue-200"}
                        label={child.title}
                        opacity={0.8}
                      />
                    </div>
                  ))}
                  {/* Spacer for no tasks */}
                  {!epic.collapsed && epic.children.length === 0 && <div className="h-[37px]" />}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
      <IssueModal />
    </div>
  );
}

function Bar({ left, width, color, label, opacity = 1 }: { left: string; width: string; color: string; label: string, opacity?: number }) {
  return (
    <div
      className={`absolute h-6 top-1/2 -translate-y-1/2 rounded-md ${color} shadow-sm border px-2 text-[10px] font-semibold flex items-center overflow-hidden`}
      style={{ left, width, opacity }}
      title={label}
    >
      <span className="truncate">{label}</span>
    </div>
  );
}
