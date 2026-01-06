import { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { useProjects } from "@/hooks/useProjects";
import { useCreateTask } from "@/hooks/useTasks";
import { projectService } from "@/services/project.service";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";

interface CreateIssueDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const INITIAL_FORM_STATE = {
  projectId: "",
  title: "",
  description: "",
  priority: "MEDIUM",
  type: "TASK",
  estimatedHours: "",
  dueDate: "",
  assigneeId: null as number | null,
  parentTaskId: null as number | null,
  epicId: null as number | null,
};

export default function CreateIssueDialog({ open, onOpenChange }: CreateIssueDialogProps) {
  const { toast } = useToast();
  const { data: projectsResponse } = useProjects();
  const { mutate: createTask, isPending } = useCreateTask();

  const [formData, setFormData] = useState(INITIAL_FORM_STATE);

  const projects = projectsResponse?.data || [];
  
  // Load members for selected project
  const { data: membersResponse } = useQuery({
    queryKey: ["project-members", formData.projectId],
    queryFn: () => projectService.getProjectMembers(parseInt(formData.projectId)),
    enabled: !!formData.projectId,
  });
  const members = membersResponse?.data?.filter((m) => m && m.userId) || [];
  
  // Filter out archived projects
  const activeProjects = projects.filter(project => project.status !== "ARCHIVED");

  // Reset form when dialog opens
  useEffect(() => {
    if (open) {
      setFormData(INITIAL_FORM_STATE);
    }
  }, [open]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.projectId) {
      toast({
        title: "Error",
        description: "Please select a project",
        variant: "destructive",
      });
      return;
    }

    if (!formData.title.trim()) {
      toast({
        title: "Error",
        description: "Title is required",
        variant: "destructive",
      });
      return;
    }

    try {
      const payload = {
        projectId: parseInt(formData.projectId),
        title: formData.title,
        description: formData.description || null,
        priority: formData.priority,
        type: formData.type,
        storyPoints: formData.estimatedHours ? parseFloat(formData.estimatedHours) : null,
        // Send date as-is for backend to handle as LocalDate (no timezone conversion)
        dueDate: formData.dueDate || null,
        sprintId: null,
        // Set parentTaskId for subtasks, epicId for stories/tasks/bugs
        parentTaskId: formData.type === "SUBTASK" ? formData.parentTaskId : null,
        assigneeId: formData.assigneeId,
      };

      createTask(payload, {
        onSuccess: () => {
          // Reset form and close dialog
          setFormData(INITIAL_FORM_STATE);
          onOpenChange(false);
          
          // Show success toast
          toast({
            title: "Success",
            description: "Issue created successfully",
          });
        },
      });
    } catch (error) {
      // Error handled by mutation
      console.error("Failed to create task:", error);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-modal-md max-h-[90vh] overflow-y-auto">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>Create New Issue</DialogTitle>
            <DialogDescription>
              Add a new task or issue to your project. Fill in the details below.
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 py-4">
            {/* Project Selection */}
            <div className="grid gap-2">
              <Label htmlFor="project">
                Project <span className="text-red-500">*</span>
              </Label>
              <Select
                value={formData.projectId}
                onValueChange={(value) => setFormData({ ...formData, projectId: value })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select a project" />
                </SelectTrigger>
                <SelectContent>
                  {activeProjects.map((project) => (
                    <SelectItem key={project.id} value={project.id.toString()}>
                      {project.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Title */}
            <div className="grid gap-2">
              <Label htmlFor="title">
                Title <span className="text-red-500">*</span>
              </Label>
              <Input
                id="title"
                placeholder="e.g., Fix login button alignment"
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                required
              />
            </div>

            {/* Description */}
            <div className="grid gap-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                placeholder="Detailed description of the issue..."
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                rows={4}
              />
            </div>

            {/* Type and Priority */}
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="type">Type</Label>
                <Select
                  value={formData.type}
                  onValueChange={(value) => {
                    // Reset parent/epic when type changes
                    setFormData({ 
                      ...formData, 
                      type: value,
                      parentTaskId: null,
                      epicId: null 
                    });
                  }}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="EPIC">Epic</SelectItem>
                    <SelectItem value="STORY">Story</SelectItem>
                    <SelectItem value="TASK">Task</SelectItem>
                    <SelectItem value="BUG">Bug</SelectItem>
                    <SelectItem value="SUBTASK">Subtask</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="grid gap-2">
                <Label htmlFor="priority">Priority</Label>
                <Select
                  value={formData.priority}
                  onValueChange={(value) => setFormData({ ...formData, priority: value })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="LOW">Low</SelectItem>
                    <SelectItem value="MEDIUM">Medium</SelectItem>
                    <SelectItem value="HIGH">High</SelectItem>
                    <SelectItem value="CRITICAL">Critical</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Parent Issue Selection for Subtasks */}
            {formData.projectId && formData.type === "SUBTASK" && (
              <div className="grid gap-2">
                <Label htmlFor="parentTask">
                  Parent Issue <span className="text-red-500">*</span>
                </Label>
                <Select
                  value={formData.parentTaskId?.toString() || ""}
                  onValueChange={(value) =>
                    setFormData({ ...formData, parentTaskId: value ? parseInt(value) : null })
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select parent (Story/Task/Bug)" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">No Parent (will use mock data)</SelectItem>
                    {/* Mock data - in real app, would fetch stories/tasks/bugs */}
                    <SelectItem value="1001">PROJ-1: Implement user authentication</SelectItem>
                    <SelectItem value="1002">PROJ-2: Create dashboard layout</SelectItem>
                    <SelectItem value="1003">PROJ-3: Fix responsive issues</SelectItem>
                  </SelectContent>
                </Select>
                <p className="text-xs text-muted-foreground">
                  Subtasks must belong to a Story, Task, or Bug
                </p>
              </div>
            )}

            {/* Epic Selection for Stories/Tasks/Bugs */}
            {formData.projectId && ["STORY", "TASK", "BUG"].includes(formData.type) && (
              <div className="grid gap-2">
                <Label htmlFor="epic">Epic (Optional)</Label>
                <Select
                  value={formData.epicId?.toString() || ""}
                  onValueChange={(value) =>
                    setFormData({ ...formData, epicId: value ? parseInt(value) : null })
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select epic" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">No Epic</SelectItem>
                    {/* Mock data - in real app, would fetch epics */}
                    <SelectItem value="2001">Epic: User Management System</SelectItem>
                    <SelectItem value="2002">Epic: Reporting Dashboard</SelectItem>
                    <SelectItem value="2003">Epic: Mobile App Development</SelectItem>
                  </SelectContent>
                </Select>
                <p className="text-xs text-muted-foreground">
                  Link this issue to an epic for better organization
                </p>
              </div>
            )}

            {/* Assignee */}
            {formData.projectId && (
              <div className="grid gap-2">
                <Label htmlFor="assignee">Assign To</Label>
                <Select
                  value={formData.assigneeId?.toString() || ""}
                  onValueChange={(value) =>
                    setFormData({ ...formData, assigneeId: value ? parseInt(value) : null })
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select team member" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">Unassigned</SelectItem>
                    {members.map((member) => (
                      <SelectItem key={member.userId} value={member.userId.toString()}>
                        {member.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}

            {/* Story Points */}
            <div className="grid gap-2">
              <Label htmlFor="estimatedHours">Story Points</Label>
              <Input
                id="estimatedHours"
                type="number"
                min="0"
                step="0.5"
                placeholder="e.g., 5"
                value={formData.estimatedHours}
                onChange={(e) =>
                  setFormData({ ...formData, estimatedHours: e.target.value })
                }
              />
            </div>

            {/* Due Date */}
            <div className="grid gap-2">
              <Label htmlFor="dueDate">Due Date</Label>
              <Input
                id="dueDate"
                type="date"
                value={formData.dueDate}
                onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              className="bg-blue-600 hover:bg-blue-700"
              disabled={isPending}
            >
              {isPending ? "Creating..." : "Create Issue"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
