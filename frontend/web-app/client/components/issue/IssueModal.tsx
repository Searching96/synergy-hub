import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { useProjects } from "@/hooks/useProjects";
import { useTask, useCreateTask } from "@/hooks/useTasks";
import { useUpdateTask } from "@/hooks/useUpdateTask";
import { projectService } from "@/services/project.service";
import IssueForm, { IssueFormValues } from "./IssueForm";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

export default function IssueModal() {
  const [searchParams, setSearchParams] = useSearchParams();
  const issueParam = searchParams.get("issue");
  const isCreate = searchParams.get("create") === "true";
  const open = isCreate || !!issueParam;

  const taskId = useMemo(() => {
    if (!issueParam) return null;
    const parts = issueParam.split("-");
    const last = parts[parts.length - 1];
    const parsed = parseInt(last || "", 10);
    return Number.isNaN(parsed) ? null : parsed;
  }, [issueParam]);

  const { data: taskResponse, isLoading: isTaskLoading } = useTask(taskId || 0);
  const { data: projectsResponse } = useProjects();
  const createTask = useCreateTask();
  const updateTask = useUpdateTask();

  const projects = useMemo(() => projectsResponse?.data || [], [projectsResponse]);

  const [formValues, setFormValues] = useState<IssueFormValues>({
    projectId: null,
    title: "",
    description: "",
    priority: "MEDIUM",
    type: "TASK",
    assigneeId: null,
  });

  useEffect(() => {
    if (taskResponse?.data && taskId) {
      const task = taskResponse.data;
      setFormValues({
        projectId: task.projectId || null,
        title: task.title || "",
        description: task.description || "",
        priority: task.priority || "MEDIUM",
        type: (task.type || "TASK") as IssueFormValues["type"],
        assigneeId: task.assignee?.id || null,
      });
    }
  }, [taskId, taskResponse]);

  useEffect(() => {
    if (isCreate && projects.length > 0 && !formValues.projectId) {
      setFormValues((prev) => ({ ...prev, projectId: projects[0].id }));
    }
  }, [isCreate, projects, formValues.projectId]);

  const selectedProjectId = formValues.projectId;

  const { data: membersResponse } = useQuery({
    queryKey: ["issue-modal-members", selectedProjectId],
    queryFn: () => projectService.getProjectMembers(selectedProjectId!),
    enabled: !!selectedProjectId,
  });

  const members = useMemo(() => {
    if (!membersResponse) return [];
    // Handle ApiResponse<ProjectMember[]> structure
    const memberData = membersResponse.data || membersResponse;
    // Filter out any undefined items and ensure we have valid members
    return Array.isArray(memberData) ? memberData.filter((m): m is typeof memberData[0] => m !== undefined && m !== null) : [];
  }, [membersResponse]);

  const handleClose = () => {
    const next = new URLSearchParams(searchParams);
    next.delete("issue");
    next.delete("create");
    setSearchParams(next);
  };

  const handleSubmit = async (values: IssueFormValues) => {
    if (!values.projectId) {
      toast.error("Please choose a project");
      return;
    }

    try {
      if (taskId) {
        await updateTask.mutateAsync({
          taskId,
          data: {
            title: values.title,
            description: values.description || null,
            priority: values.priority,
            type: values.type,
            assigneeId: values.assigneeId,
          },
        });
      } else {
        await createTask.mutateAsync({
          projectId: values.projectId,
          title: values.title,
          description: values.description || null,
          priority: values.priority,
          type: values.type,
          dueDate: null,
          sprintId: null,
          parentTaskId: null,
          assigneeId: values.assigneeId,
        });
      }
      handleClose();
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.response?.data?.error || "Failed to save issue";
      toast.error(message);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(nextOpen) => !nextOpen && handleClose()}>
      <DialogContent className="max-w-3xl">
        <DialogHeader>
          <DialogTitle>{taskId ? "Edit Issue" : "Create Issue"}</DialogTitle>
        </DialogHeader>

        {taskId && isTaskLoading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        ) : (
          <IssueForm
            initialValues={formValues}
            projects={projects}
            members={members}
            isSubmitting={createTask.isPending || updateTask.isPending}
            onSubmit={handleSubmit}
            onCancel={handleClose}
          />
        )}
      </DialogContent>
    </Dialog>
  );
}
