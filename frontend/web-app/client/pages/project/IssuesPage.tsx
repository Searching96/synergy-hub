import { useMemo, useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { taskService } from "@/services/task.service";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuLabel,
  DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Separator } from "@/components/ui/separator";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";
import { useProject } from "@/context/ProjectContext";
import {
  MoreHorizontal,
  Share2,
  Download,
  Sparkles,
  Search,
  Filter,
  Users,
  Layers,
  ChevronDown,
  ChevronsUpDown,
  MessageSquare,
  CircleDot,
  Circle,
  Laugh,
  Frown,
} from "lucide-react";
import { toast } from "sonner";

interface IssueListItem {
  id: string;
  title: string;
  type: "BUG" | "TASK" | "STORY";
  assignee: string;
}


export default function IssuesPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { project } = useProject();
  const [viewMode, setViewMode] = useState<"LIST" | "DETAIL">("LIST");
  const [activeId, setActiveId] = useState<number | null>(null);

  const { data: tasksResponse, isLoading, error } = useQuery({
    queryKey: ["tasks", projectId],
    queryFn: () => taskService.getProjectTasks(projectId!),
    enabled: !!projectId,
  });

  const issues = (tasksResponse?.data || []) as any[]; // Using any primarily due to type mismatches with existing UI expectations, plan to refining types later

  const activeIssue = useMemo(() => {
    if (activeId) {
      return issues.find((i) => i.id === activeId);
    }
    return issues.length > 0 ? issues[0] : null;
  }, [issues, activeId]);

  useEffect(() => {
    if (!activeId && issues.length > 0) {
      setActiveId(issues[0].id);
    }
  }, [issues, activeId]);

  const projectLabel = project?.name || "Project";

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="space-y-4">
        <div className="flex items-center justify-between gap-3">
          <div className="space-y-1">
            <div className="text-sm text-muted-foreground">Projects / {projectLabel}</div>
            <h1 className="text-3xl font-bold">Issues</h1>
          </div>
          {/* ... existing header buttons ... */}
          <div className="flex items-center gap-2 flex-wrap justify-end">
            {/* Keep existing buttons for layout, can be functionalized later */}
            <Button variant="outline" size="sm" onClick={() => {
              navigator.clipboard.writeText(window.location.href);
              toast.success("Link copied to clipboard");
            }}>
              <Share2 className="h-4 w-4 mr-2" />
              Share
            </Button>
            <Button variant="outline" size="sm">
              <Download className="h-4 w-4 mr-2" />
              Export issues
            </Button>
            {/* View Mode Toggle */}
            <div className="flex rounded-md border overflow-hidden">
              <Button
                variant={viewMode === "LIST" ? "secondary" : "ghost"}
                size="sm"
                className="rounded-none"
                onClick={() => setViewMode("LIST")}
              >
                LIST VIEW
              </Button>
              <Separator orientation="vertical" />
              <Button
                variant={viewMode === "DETAIL" ? "secondary" : "ghost"}
                size="sm"
                className="rounded-none"
                onClick={() => setViewMode("DETAIL")}
              >
                DETAIL VIEW
              </Button>
            </div>
          </div>
        </div>

        {/* Filter Bar (Placeholder for now, keeping as is or slightly modifying) */}
        <div className="flex flex-wrap items-center gap-2 bg-muted/50 border rounded-lg p-3">
          {/* ... keep existing filter bar structure ... */}
          <div className="flex items-center gap-2 flex-1 min-w-[240px]">
            <Button variant="secondary" size="icon">
              <Sparkles className="h-4 w-4" />
            </Button>
            <div className="relative w-full">
              <Input placeholder="Search issues" className="pl-3 pr-10" />
              <Search className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            </div>
          </div>
          {/* ... */}
        </div>
      </div>

      {/* Main Content Grid */}
      <div className="grid gap-6 grid-cols-1 lg:grid-cols-12">
        {/* Left Column: Issue List */}
        <div className={viewMode === 'DETAIL' ? "lg:col-span-3 space-y-3" : "lg:col-span-12 space-y-3"}>
          <div className="flex items-center justify-between text-sm text-muted-foreground">
            <div className="flex items-center gap-1">
              <span>Created</span>
              <ChevronsUpDown className="h-4 w-4" />
            </div>
            <span>{issues.length} issues</span>
          </div>

          <div className="space-y-2">
            {isLoading && <div>Loading issues...</div>}
            {!isLoading && issues.length === 0 && <div>No issues found.</div>}
            {issues.map((issue) => {
              const isActive = issue.id === activeId;
              return (
                <button
                  key={issue.id}
                  onClick={() => setActiveId(issue.id)}
                  className={`w-full text-left border rounded-lg p-3 flex items-start gap-3 transition-colors ${isActive && viewMode === 'DETAIL' ? "bg-blue-50 border-blue-200" : "hover:bg-muted"
                    }`}
                >
                  <div className="mt-0.5">
                    {/* Simplified icon logic */}
                    <CircleDot className="h-4 w-4 text-blue-500" />
                  </div>
                  <div className="flex-1 space-y-1">
                    <div className="text-sm font-semibold leading-tight">{issue.title}</div>
                    <div className="text-xs text-muted-foreground">#{issue.id}</div>
                  </div>
                  <Avatar className="h-8 w-8">
                    <AvatarFallback>{issue.assigneeName ? issue.assigneeName.substring(0, 2).toUpperCase() : "UN"}</AvatarFallback>
                  </Avatar>
                </button>
              );
            })}
          </div>
        </div>

        {/* Center Column: Issue Detail (Only show in DETAIL mode) */}
        {viewMode === 'DETAIL' && activeIssue && (
          <div className="lg:col-span-9 space-y-4">
            {/* Detail View Content */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
              <div className="lg:col-span-8 space-y-4">
                {/* Meta Header */}
                <div className="flex items-center justify-between gap-3">
                  <div className="text-sm text-muted-foreground">
                    {project?.name} / #{activeIssue.id}
                  </div>
                </div>

                <div className="space-y-3">
                  <h2 className="text-2xl font-semibold">{activeIssue.title}</h2>
                  {/* Action Buttons */}
                  <div className="flex flex-wrap items-center gap-2">
                    {/* ... keep generic action buttons for visual completeness ... */}
                  </div>
                </div>

                {/* Description */}
                <div className="space-y-2">
                  <div className="text-sm font-semibold">Description</div>
                  <div className="border rounded-lg p-3 text-sm text-muted-foreground bg-background whitespace-pre-wrap">
                    {activeIssue.description || "No description provided."}
                  </div>
                </div>
              </div>

              {/* Right Meta Sidebar (within detail view) */}
              <div className="lg:col-span-4 space-y-3">
                <div className="border rounded-lg p-4 space-y-4">
                  <div className="space-y-1">
                    <span className="text-xs font-semibold text-muted-foreground">Status</span>
                    <Badge>{activeIssue.status}</Badge>
                  </div>
                  <div className="space-y-1">
                    <span className="text-xs font-semibold text-muted-foreground">Assignee</span>
                    <div className="flex items-center gap-2">
                      <Avatar className="h-6 w-6">
                        <AvatarFallback>{activeIssue.assigneeName ? activeIssue.assigneeName.substring(0, 2) : "UN"}</AvatarFallback>
                      </Avatar>
                      <span className="text-sm">{activeIssue.assigneeName || "Unassigned"}</span>
                    </div>
                  </div>
                  <div className="space-y-1">
                    <span className="text-xs font-semibold text-muted-foreground">Priority</span>
                    <Badge variant="outline">{activeIssue.priority}</Badge>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Floating Footer */}
      <div className="fixed bottom-4 right-4 flex items-center gap-2 bg-white border shadow-sm rounded-full px-3 py-2 text-sm">
        <Button variant="ghost" size="sm" className="gap-2">
          <Laugh className="h-4 w-4" /> Like
        </Button>
        <Separator orientation="vertical" className="h-5" />
        <Button variant="ghost" size="sm" className="gap-2">
          <Frown className="h-4 w-4" /> Dislike
        </Button>
        <Separator orientation="vertical" className="h-5" />
        <Button variant="ghost" size="sm" className="gap-2">
          <MessageSquare className="h-4 w-4" /> Comment
        </Button>
      </div>
    </div>
  );
}
