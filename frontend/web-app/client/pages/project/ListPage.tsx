import { useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { useDebounce } from "@/hooks/useDebounce";
import { useQuery } from "@tanstack/react-query";
import { taskService } from "@/services/task.service";
import { useProject } from "@/context/ProjectContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ProjectBreadcrumb } from "@/components/project/ProjectBreadcrumb";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuLabel,
  DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu";
import { Filter, Share2, Group, MoreHorizontal, Search, Check } from "lucide-react";
import type { Task } from "@/types/task.types";
import { toast } from "sonner";

export default function ListPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { project } = useProject();
  const [query, setQuery] = useState("");
  const debouncedQuery = useDebounce(query, 300);
  const [selected, setSelected] = useState<Record<number, boolean>>({});
  const [selectAll, setSelectAll] = useState(false);

  const { data, isLoading, error } = useQuery({
    queryKey: ["tasks", projectId, { q: debouncedQuery }],
    queryFn: () => taskService.getProjectTasks(projectId!, debouncedQuery ? { q: debouncedQuery } : {}),
    enabled: !!projectId,
  });

  const tasks = (data?.data || []) as Task[];

  const [filterStatus, setFilterStatus] = useState<string | null>(null);
  const [sortBy, setSortBy] = useState<string>('id');

  const filtered = useMemo(() => {
    let result = tasks;

    // Filter by query
    if (query) {
      const q = query.toLowerCase();
      result = result.filter(
        (t) =>
          t.title?.toLowerCase().includes(q) ||
          String(t.id).includes(q) ||
          t.assigneeName?.toLowerCase().includes(q) ||
          t.sprintName?.toLowerCase().includes(q)
      );
    }

    // Filter by status
    if (filterStatus) {
      result = result.filter((t) => t.status === filterStatus);
    }

    // Sort
    result = [...result].sort((a, b) => {
      if (sortBy === 'id') return a.id - b.id;
      if (sortBy === 'title') return a.title.localeCompare(b.title);
      if (sortBy === 'status') return (a.status || '').localeCompare(b.status || '');
      return 0;
    });

    return result;
  }, [tasks, query, filterStatus, sortBy]);

  const toggleSelectAll = (checked: boolean) => {
    setSelectAll(checked);
    if (checked) {
      const next: Record<number, boolean> = {};
      filtered.forEach((t) => (next[t.id] = true));
      setSelected(next);
    } else {
      setSelected({});
    }
  };

  const toggleRow = (id: number, checked: boolean) => {
    setSelected((prev) => ({ ...prev, [id]: checked }));
  };

  return (
    <div className="p-6 space-y-6">
      <div className="mb-4">
        <ProjectBreadcrumb current="List" />
      </div>

      <div className="space-y-6">
        {/* Header */}
        <div>
          <h1 className="text-3xl font-bold">List</h1>
          <p className="text-muted-foreground mt-1">
            Browse and manage issues in {project?.name || "this project"}
          </p>
        </div>

        {/* Toolbar */}
        <div className="flex items-center gap-2">
          <div className="relative max-w-md flex-1">
            <Search className="absolute left-2 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search issues..."
              className="pl-8"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </div>
          <Button variant="outline" onClick={() => {
            navigator.clipboard.writeText(window.location.href);
            toast.success("Link copied to clipboard");
          }}>
            <Share2 className="h-4 w-4 mr-2" />
            Share
          </Button>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline">
                <Filter className="h-4 w-4 mr-2" />
                Filter
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent>
              <DropdownMenuLabel>Filter by Status</DropdownMenuLabel>
              <DropdownMenuSeparator />
              {['TO_DO', 'IN_PROGRESS', 'DONE'].map(status => (
                <DropdownMenuItem key={status} onClick={() => setFilterStatus(prev => prev === status ? null : status)}>
                  {filterStatus === status && <Check className="mr-2 h-4 w-4" />}
                  {status.replace('_', ' ')}
                </DropdownMenuItem>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline">
                <Group className="h-4 w-4 mr-2" />
                Sort: {sortBy === 'id' ? 'Key' : sortBy === 'title' ? 'Summary' : sortBy === 'status' ? 'Status' : 'None'}
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent>
              <DropdownMenuLabel>Sort by</DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => setSortBy('id')}>Key</DropdownMenuItem>
              <DropdownMenuItem onClick={() => setSortBy('title')}>Summary</DropdownMenuItem>
              <DropdownMenuItem onClick={() => setSortBy('status')}>Status</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>

          <Button variant="ghost" size="icon">
            <MoreHorizontal className="h-5 w-5" />
          </Button>
        </div>

        {/* Table */}
        <div className="bg-white border rounded-lg">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-10">
                  <Checkbox
                    checked={selectAll}
                    onCheckedChange={(v) => toggleSelectAll(Boolean(v))}
                    aria-label="Select all"
                  />
                </TableHead>
                <TableHead className="w-28">Type</TableHead>
                <TableHead className="w-28">Key</TableHead>
                <TableHead>Summary</TableHead>
                <TableHead className="w-40">Status</TableHead>
                <TableHead className="w-40">Sprint</TableHead>
                <TableHead className="w-40">Assignee</TableHead>
                <TableHead className="w-36">Due date</TableHead>
                <TableHead className="w-40">Labels</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading && (
                <TableRow>
                  <TableCell colSpan={9} className="text-center py-10 text-muted-foreground">
                    Loading issues...
                  </TableCell>
                </TableRow>
              )}
              {error && !isLoading && (
                <TableRow>
                  <TableCell colSpan={9} className="text-center py-10 text-destructive">
                    Failed to load issues
                  </TableCell>
                </TableRow>
              )}
              {!isLoading && !error && filtered.length === 0 && (
                <TableRow>
                  <TableCell colSpan={9} className="text-center py-10 text-muted-foreground">
                    No issues found
                  </TableCell>
                </TableRow>
              )}
              {filtered.map((t) => (
                <TableRow key={t.id} data-state={selected[t.id] ? "selected" : undefined}>
                  <TableCell>
                    <Checkbox
                      checked={!!selected[t.id]}
                      onCheckedChange={(v) => toggleRow(t.id, Boolean(v))}
                      aria-label={`Select issue ${t.id}`}
                    />
                  </TableCell>
                  <TableCell className="uppercase text-xs font-medium text-muted-foreground">{t.type}</TableCell>
                  <TableCell className="font-mono">{t.id}</TableCell>
                  <TableCell className="font-medium">{t.title}</TableCell>
                  <TableCell>{t.status}</TableCell>
                  <TableCell>{t.sprintName || "—"}</TableCell>
                  <TableCell>{t.assigneeName || "Unassigned"}</TableCell>
                  <TableCell>{t.dueDate ? new Date(t.dueDate).toLocaleDateString() : "—"}</TableCell>
                  <TableCell>{(t as any).labels?.join(", ") || "—"}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </div>
    </div>
  );
}
