import { useMemo, useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuLabel,
  DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Separator } from "@/components/ui/separator";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";
import { useProject } from "@/context/ProjectContext";
import {
  useProjectTasks,
} from "@/hooks/useTasks";
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
import { taskService } from "@/services/task.service";
import IssueDetailPanel from "@/components/backlog/IssueDetailPanel";

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
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedIssues, setSelectedIssues] = useState<string[]>([]);

  const { data: allIssues = [], isLoading, error } = useProjectTasks(projectId!);

  const filteredIssues = useMemo(() => {
    return allIssues.filter((issue) => {
      const matchesSearch =
        issue.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        issue.id.toString().includes(searchQuery) ||
        (issue.description && issue.description.toLowerCase().includes(searchQuery.toLowerCase()));

      return matchesSearch;
    });
  }, [allIssues, searchQuery]);

  const activeIssue = useMemo(() => {
    if (activeId) {
      return filteredIssues.find((i) => i.id === activeId);
    }
    return filteredIssues.length > 0 ? filteredIssues[0] : null;
  }, [filteredIssues, activeId]);

  useEffect(() => {
    if (!activeId && filteredIssues.length > 0) {
      setActiveId(filteredIssues[0].id);
    } else if (activeId && !filteredIssues.find((i) => i.id === activeId)) {
      // Active issue no longer exists, reset to first
      setActiveId(filteredIssues.length > 0 ? filteredIssues[0].id : null);
    }
  }, [filteredIssues, activeId]);

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
            <Button variant="outline" size="sm" onClick={() => {
              const csvContent = "data:text/csv;charset=utf-8,"
                + "ID,Title,Status,Assignee,Priority\n"
                + filteredIssues.map(i => `${i.id},"${i.title}",${i.status},"${i.assigneeName || 'Unassigned'}",${i.priority}`).join("\n");
              const encodedUri = encodeURI(csvContent);
              const link = document.createElement("a");
              link.setAttribute("href", encodedUri);
              link.setAttribute("download", `${project?.name || 'Project'}_Issues.csv`);
              document.body.appendChild(link);
              link.click();
              document.body.removeChild(link);
              toast.success("Issues exported successfully");
            }}>
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
              <Input
                placeholder="Search issues"
                className="pl-3 pr-10"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              <Search className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            </div>
          </div>
          {/* ... */}
        </div>
      </div>

      {/* Bulk Actions Bar */}
      {selectedIssues.length > 0 && (
        <div className="flex items-center justify-between bg-zinc-900 text-white px-4 py-3 rounded-lg shadow-lg animate-in fade-in slide-in-from-bottom-4">
          <div className="flex items-center gap-3">
            <div className="font-semibold text-sm">{selectedIssues.length} selected</div>
            <Separator orientation="vertical" className="h-4 bg-white/20" />
            <Button
              variant="ghost"
              size="sm"
              className="text-white/80 hover:bg-white/10 hover:text-white h-auto py-1 px-2"
              onClick={() => setSelectedIssues([])}
            >
              Clear
            </Button>
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="destructive"
              size="sm"
              onClick={async () => {
                if (!confirm(`Are you sure you want to delete ${selectedIssues.length} issues?`)) return;

                const toastId = toast.loading("Deleting issues...");
                try {
                  await Promise.all(selectedIssues.map(id => taskService.deleteTask(parseInt(id))));
                  toast.success(`Deleted ${selectedIssues.length} issues`);
                  setSelectedIssues([]);
                  // Invalidate queries would happen automatically if keys match, or we force it:
                  // queryClient.invalidateQueries({ queryKey: ["tasks", projectId] });
                  // For now, simple page or list refresh relies on query invalidation or optimistic updates (not implemented here fully)
                  window.location.reload(); // Simple brute force update for now until QueryClient is accessible
                } catch (err) {
                  console.error(err);
                  toast.error("Failed to delete some issues");
                } finally {
                  toast.dismiss(toastId);
                }
              }}
            >
              Delete Selected
            </Button>
          </div>
        </div>
      )}

      {/* Main Content Grid */}
      <div className="grid gap-6 grid-cols-1 lg:grid-cols-12">
        {/* Left Column: Issue List */}
        <div className={viewMode === 'DETAIL' ? "lg:col-span-3 space-y-3" : "lg:col-span-12 space-y-3"}>
          <div className="flex items-center justify-between text-sm text-muted-foreground">
            <div className="flex items-center gap-1">
              <span>Created</span>
              <ChevronsUpDown className="h-4 w-4" />
            </div>
            <span>{filteredIssues.length} issues</span>
          </div>

          <div className="space-y-2">
            {isLoading && <div>Loading issues...</div>}
            {!isLoading && filteredIssues.length === 0 && <div>No issues found.</div>}
            {filteredIssues.map((issue) => {
              const isActive = issue.id === activeId;
              return (
                <div
                  key={issue.id}
                  onClick={() => setActiveId(issue.id)}
                  className={`w-full text-left border rounded-lg p-3 flex items-center gap-3 transition-colors cursor-pointer group ${isActive && viewMode === 'DETAIL' ? "bg-blue-50 border-blue-200" : "hover:bg-muted"
                    }`}
                >
                  <div onClick={(e) => e.stopPropagation()}>
                    <Checkbox
                      checked={selectedIssues.includes(issue.id)}
                      onCheckedChange={(checked) => {
                        setSelectedIssues(prev =>
                          checked
                            ? [...prev, issue.id]
                            : prev.filter(id => id !== issue.id)
                        );
                      }}
                    />
                  </div>
                  <div className="mt-0.5">
                    {/* Simplified icon logic */}
                    <CircleDot className="h-4 w-4 text-blue-500" />
                  </div>
                  <div className="flex-1 space-y-1 min-w-0">
                    <div className="text-sm font-semibold leading-tight truncate">{issue.title}</div>
                    <div className="text-xs text-muted-foreground flex items-center gap-2">
                      <span>#{issue.id}</span>
                      {issue.status && <Badge variant="outline" className="text-[10px] h-4 px-1">{issue.status}</Badge>}
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <Avatar className="h-6 w-6">
                      <AvatarFallback className="text-[10px]">
                        {issue.assigneeName
                          ? issue.assigneeName.substring(0, 2).toUpperCase()
                          : (issue.assignee?.name
                            ? issue.assignee.name.substring(0, 2).toUpperCase()
                            : "UN")}
                      </AvatarFallback>
                    </Avatar>

                    <DropdownMenu>
                      <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
                        <Button variant="ghost" size="icon" className="h-8 w-8 opacity-0 group-hover:opacity-100 transition-opacity">
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuLabel>Actions</DropdownMenuLabel>
                        <DropdownMenuItem onClick={(e) => { e.stopPropagation(); /* Implement Edit */ }}>
                          Edit Issue
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem className="text-destructive" onClick={(e) => { e.stopPropagation(); /* Implement Delete */ }}>
                          Delete Issue
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Center Column: Issue Detail (Only show in DETAIL mode) */}
        {viewMode === 'DETAIL' && activeIssue && (
          <div className="lg:col-span-9 h-[calc(100vh-12rem)]">
            <IssueDetailPanel
              taskId={parseInt(activeIssue.id)}
              projectId={parseInt(projectId!)}
              issueKey={`${projectLabel.substring(0, 4).toUpperCase()}-${activeIssue.id}`}
              issueType={activeIssue.type}
              title={activeIssue.title}
              status={activeIssue.status}
              description={activeIssue.description || ""}
              onClose={() => setViewMode("LIST")}
              className="w-full border-none shadow-none h-full"
            />
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
