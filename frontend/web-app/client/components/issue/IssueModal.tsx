import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter
} from "@/components/ui/dialog";
import { useProjects } from "@/hooks/useProjects";
import { useTask, useCreateTask } from "@/hooks/useTasks";
import { useUpdateTask } from "@/hooks/useUpdateTask";
import { projectService } from "@/services/project.service";
import IssueForm, { IssueFormValues } from "./IssueForm";
import { Loader2 } from "lucide-react";
import { useToast } from "@/hooks/use-toast";

export default function IssueModal() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { toast } = useToast();

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
  const { mutateAsync: createTask, isPending: isCreating } = useCreateTask();
  const { mutateAsync: updateTask, isPending: isUpdating } = useUpdateTask();

  const projects = useMemo(() => projectsResponse?.data?.content || [], [projectsResponse]);

  const [formValues, setFormValues] = useState<IssueFormValues>({
    projectId: "",
    title: "",
    description: "",
    priority: "MEDIUM",
    type: "TASK",
    status: "TO_DO",
    estimatedHours: "",
    dueDate: "",
    assigneeId: null,
  });

  useEffect(() => {
    if (taskResponse?.data && taskId) {
      const task = taskResponse.data;
      setFormValues({
        projectId: task.projectId?.toString() || "",
        title: task.title || "",
        description: task.description || "",
        priority: task.priority || "MEDIUM",
        type: (task.type || "TASK") as IssueFormValues["type"],
        status: (task.status || "TO_DO") as IssueFormValues["status"],
        assigneeId: task.assignee?.id || null,
      });
    }
  }, [taskId, taskResponse]);

  useEffect(() => {
    if (isCreate && projects.length > 0 && !formValues.projectId) {
      setFormValues((prev) => ({ ...prev, projectId: projects[0].id.toString() }));
    }
  }, [isCreate, projects, formValues.projectId]);

  const selectedProjectId = formValues.projectId;

  const { data: membersResponse } = useQuery({
    queryKey: ["issue-modal-members", selectedProjectId],
    queryFn: () => projectService.getProjectMembers(parseInt(selectedProjectId)),
    enabled: !!selectedProjectId,
  });

  const members = useMemo(() => {
    if (!membersResponse) return [];
    const memberData = membersResponse.data || membersResponse;
    return Array.isArray(memberData)
      ? memberData.filter((m): m is typeof memberData[0] => m !== undefined && m !== null)
      : [];
  }, [membersResponse]);

  const handleClose = () => {
    const next = new URLSearchParams(searchParams);
    next.delete("issue");
    next.delete("create");
    setSearchParams(next);
  };

  const handleSubmit = async (values: IssueFormValues) => {
    if (!values.projectId) {
      toast({
        title: "Error",
        description: "Please choose a project",
        variant: "destructive",
      });
      return;
    }

    try {
      if (taskId) {
        // Update existing task
        await updateTask({
          taskId,
          data: {
            title: values.title,
            description: values.description || null,
            priority: values.priority,
            type: values.type,
            status: values.status || "TO_DO",
            assigneeId: values.assigneeId,
            storyPoints: values.estimatedHours ? parseFloat(values.estimatedHours) : null,
            dueDate: values.dueDate || null,
          },
        });
        toast({
          title: "Success",
          description: "Issue updated successfully",
        });
      } else {
        // Create new task
        await createTask({
          projectId: parseInt(values.projectId),
          title: values.title,
          description: values.description || null,
          priority: values.priority,
          type: values.type,
          status: values.status || "TO_DO",
          dueDate: values.dueDate || null,
          sprintId: null,
          parentTaskId: values.parentTaskId || null,
          assigneeId: values.assigneeId || null,
          storyPoints: values.estimatedHours ? parseFloat(values.estimatedHours) : null,
        });
        toast({
          title: "Success",
          description: "Issue created successfully",
        });

        // If "create another" is checked, reset form but keep project
        if (values.createAnother) {
          setFormValues({
            projectId: values.projectId,
            title: "",
            description: "",
            priority: "MEDIUM",
            type: "TASK",
            status: "TO_DO",
            estimatedHours: "",
            dueDate: "",
            assigneeId: null,
          });
          return; // Don't close dialog
        }
      }

      handleClose();
    } catch (err: any) {
      const message =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        "Failed to save issue";
      toast({
        title: "Error",
        description: message,
        variant: "destructive",
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={(nextOpen) => !nextOpen && handleClose()}>
      <DialogContent className="sm:max-w-[800px] h-[90vh] p-0 gap-0 flex flex-col">
        {/* Header */}
        <DialogHeader className="flex-shrink-0 px-6 py-4 border-b">
          <DialogTitle className="text-2xl font-semibold">
            {taskId ? "Edit issue" : "Create issue"}
          </DialogTitle>
          <DialogDescription className="sr-only">
            {taskId
              ? "Edit the issue details below"
              : "Fill out the form below to create a new issue in your project"
            }
          </DialogDescription>
        </DialogHeader>

        {taskId && isTaskLoading ? (
          <div className="flex-1 flex items-center justify-center py-12">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        ) : (
          <IssueForm
            initialValues={formValues}
            isSubmitting={isCreating || isUpdating}
            onSubmit={handleSubmit}
            onCancel={handleClose}
          />
        )}
      </DialogContent>
    </Dialog>
  );
}
