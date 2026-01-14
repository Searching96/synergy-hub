import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { userService } from "@/services/user.service";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { Briefcase, CheckSquare, Loader2 } from "lucide-react";
import type { ApiResponse } from "@/types/auth.types";
import type { Task } from "@/types/task.types";
import type { Project } from "@/types/project.types";

import type { PaginatedResponse } from "@/types/project.types";

export default function YourWork() {
  const { data: issuesData, isLoading: loadingIssues } = useQuery<ApiResponse<Task[]>>({
    queryKey: ["my-issues"],
    queryFn: userService.getMyIssues,
  });

  const { data: projectsData, isLoading: loadingProjects } = useQuery<ApiResponse<PaginatedResponse<Project>>>({
    queryKey: ["my-projects"],
    queryFn: userService.getMyProjects,
  });

  const issues = useMemo<Task[]>(() => issuesData?.data || [], [issuesData]);
  const projects = useMemo<Project[]>(() => projectsData?.data?.content || [], [projectsData]);

  const renderIssues = () => {
    if (loadingIssues) {
      return (
        <div className="space-y-2">
          {[1, 2, 3, 4].map((key) => (
            <Skeleton key={key} className="h-10 w-full" />
          ))}
        </div>
      );
    }

    if (!issues || issues.length === 0) {
      return <div className="text-muted-foreground text-sm">No assigned issues yet.</div>;
    }

    return (
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-[80px]">Type</TableHead>
            <TableHead className="w-[120px]">Key</TableHead>
            <TableHead>Summary</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Priority</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {issues.map((issue) => {
            const keyPrefix = issue.projectKey || issue.projectName?.slice(0, 4)?.toUpperCase() || "ISS";
            const key = `${keyPrefix}-${issue.id}`;
            return (
              <TableRow key={issue.id}>
                <TableCell className="font-medium text-xs">
                  <span className="inline-flex items-center gap-1">
                    <CheckSquare className="h-3 w-3" />
                    {issue.type || "TASK"}
                  </span>
                </TableCell>
                <TableCell className="text-xs text-muted-foreground">{key}</TableCell>
                <TableCell className="text-sm">{String(issue.title || issue.summary)}</TableCell>
                <TableCell>
                  <Badge variant="secondary">{issue.status}</Badge>
                </TableCell>
                <TableCell className="text-xs text-muted-foreground">{issue.priority}</TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    );
  };

  const renderProjects = () => {
    if (loadingProjects) {
      return (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((key) => (
            <Skeleton key={key} className="h-36" />
          ))}
        </div>
      );
    }

    if (!projects || projects.length === 0) {
      return (
        <div className="flex flex-col items-center justify-center py-12 border rounded-lg bg-muted/20 border-dashed">
          <Briefcase className="h-10 w-10 text-muted-foreground mb-3 opacity-50" />
          <p className="text-muted-foreground text-sm mb-4">No recent projects found.</p>
          <Button variant="outline" size="sm" asChild>
            <a href="/projects">View All Projects</a>
          </Button>
        </div>
      );
    }

    return (
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {projects.map((project: any) => (
          <Card key={project.id} className="border">
            <CardHeader>
              <CardTitle className="text-lg flex items-center gap-2">
                <Briefcase className="h-4 w-4" />
                {project.name}
              </CardTitle>
              <CardDescription className="line-clamp-2">{project.description || "No description"}</CardDescription>
            </CardHeader>
            <CardContent className="text-sm text-muted-foreground">
              {project.role && <div className="mb-1">Role: {project.role}</div>}
              {project.updatedAt && <div>Last opened: {new Date(project.updatedAt).toLocaleDateString()}</div>}
            </CardContent>
          </Card>
        ))}
      </div>
    );
  };

  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold mb-1">Your Work</h1>
          <p className="text-muted-foreground">
            Issues assigned to you and recent projects.
          </p>
        </div>
        {(loadingIssues || loadingProjects) && (
          <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
        )}
      </div>

      <Tabs defaultValue="assigned" className="space-y-4">
        <TabsList>
          <TabsTrigger value="assigned">Assigned to Me</TabsTrigger>
          <TabsTrigger value="projects">Recent Projects</TabsTrigger>
        </TabsList>

        <TabsContent value="assigned">
          <Card>
            <CardHeader>
              <CardTitle>Assigned to Me</CardTitle>
              <CardDescription>Issues you need to act on.</CardDescription>
            </CardHeader>
            <CardContent>{renderIssues()}</CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="projects">
          <Card>
            <CardHeader>
              <CardTitle>Recent Projects</CardTitle>
              <CardDescription>Quick access to work you touched recently.</CardDescription>
            </CardHeader>
            <CardContent>{renderProjects()}</CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
