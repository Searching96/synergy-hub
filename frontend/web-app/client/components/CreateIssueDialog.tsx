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
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";
import {
  Info,
  Eye,
  MoreHorizontal,
  Bold,
  Italic,
  List,
  Link as LinkIcon,
  Sparkles,
  Zap,
  Lightbulb,
  CheckSquare,
  Bug
} from "lucide-react";

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
  status: "TO_DO",
  estimatedHours: "",
  dueDate: "",
  assigneeId: null as number | null,
  parentTaskId: null as number | null,
  epicId: null as number | null,
};

const TYPE_ICONS = {
  EPIC: { icon: Zap, color: "text-purple-600" },
  STORY: { icon: Lightbulb, color: "text-green-600" },
  TASK: { icon: CheckSquare, color: "text-blue-600" },
  BUG: { icon: Bug, color: "text-red-600" },
  SUBTASK: { icon: CheckSquare, color: "text-gray-600" },
};

export default function CreateIssueDialog({ open, onOpenChange }: CreateIssueDialogProps) {
  const { toast } = useToast();
  const { data: projectsResponse } = useProjects();
  const { mutate: createTask, isPending } = useCreateTask();

  const [formData, setFormData] = useState(INITIAL_FORM_STATE);
  const [createAnother, setCreateAnother] = useState(false);

  const projects = projectsResponse?.data?.content || [];

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
          // Show success toast
          toast({
            title: "Success",
            description: "Issue created successfully",
          });

          // Either close or reset for another creation
          if (createAnother) {
            setFormData({ ...INITIAL_FORM_STATE, projectId: formData.projectId });
          } else {
            setFormData(INITIAL_FORM_STATE);
            onOpenChange(false);
          }
        },
      });
    } catch (error) {
      // Error handled by mutation
      console.error("Failed to create task:", error);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[800px] max-h-[90vh] p-0 gap-0">
        <form onSubmit={handleSubmit} className="flex flex-col h-full">
          {/* Header */}
          <DialogHeader className="px-6 py-4 border-b">
            <DialogTitle className="text-2xl font-semibold">Create issue</DialogTitle>
            <DialogDescription className="sr-only">
              Fill out the form below to create a new issue in your project
            </DialogDescription>
          </DialogHeader>

          {/* Scrollable Form Body */}
          <div className="flex-1 overflow-y-auto px-6 py-4 space-y-5">
            {/* Meta Row */}
            <div className="flex items-center justify-between text-sm">
              <span className="text-muted-foreground">
                Required fields are marked with an asterisk <span className="text-red-500">*</span>
              </span>
              <div className="flex items-center gap-2">
                <Button
                  variant="ghost"
                  size="sm"
                  type="button"
                  onClick={() => toast({ title: "Coming Soon", description: "Issue watching will be available in the next update." })}
                >
                  <Eye className="h-4 w-4 mr-1" />
                  Watch
                </Button>
              </div>
            </div>

            {/* Project Selection */}
            <div className="grid gap-2">
              <Label htmlFor="project" className="text-sm font-medium">
                Project <span className="text-red-500">*</span>
              </Label>
              <Select
                value={formData.projectId}
                onValueChange={(value) => setFormData({ ...formData, projectId: value })}
              >
                <SelectTrigger className="h-10">
                  <SelectValue placeholder="Select a project" />
                </SelectTrigger>
                <SelectContent>
                  {activeProjects.map((project) => (
                    <SelectItem key={project.id} value={project.id.toString()}>
                      <div className="flex items-center gap-2">
                        <div className="h-5 w-5 rounded bg-blue-600 flex items-center justify-center text-white text-xs font-bold">
                          {project.name.substring(0, 1)}
                        </div>
                        {project.name}
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Issue Type */}
            <div className="grid gap-2">
              <Label htmlFor="type" className="text-sm font-medium">
                Issue type <span className="text-red-500">*</span>
              </Label>
              <Select
                value={formData.type}
                onValueChange={(value) => {
                  setFormData({
                    ...formData,
                    type: value,
                    parentTaskId: null,
                    epicId: null
                  });
                }}
              >
                <SelectTrigger className="h-10">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(TYPE_ICONS).map(([type, { icon: Icon, color }]) => (
                    <SelectItem key={type} value={type}>
                      <div className="flex items-center gap-2">
                        <Icon className={`h-4 w-4 ${color}`} />
                        {type.charAt(0) + type.slice(1).toLowerCase()}
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Button
                variant="link"
                size="sm"
                className="h-auto p-0 text-xs text-blue-600 justify-start"
                onClick={() => toast({
                  title: "Issue Types Help",
                  description: "EPIC: Large goals. STORY: User-centric features. TASK: General work. BUG: Defects. SUBTASK: Child work.",
                })}
              >
                Learn about issue types
              </Button>
            </div>

            {/* Status */}
            <div className="grid gap-2">
              <Label htmlFor="status" className="text-sm font-medium flex items-center gap-1">
                Status
                <Info className="h-3.5 w-3.5 text-muted-foreground" />
              </Label>
              <Select
                value={formData.status}
                onValueChange={(value) => setFormData({ ...formData, status: value })}
              >
                <SelectTrigger className="h-10">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="TO_DO">
                    <span className="px-2 py-0.5 rounded bg-gray-100 text-gray-700 text-sm">
                      To Do
                    </span>
                  </SelectItem>
                  <SelectItem value="IN_PROGRESS">
                    <span className="px-2 py-0.5 rounded bg-blue-100 text-blue-700 text-sm">
                      In Progress
                    </span>
                  </SelectItem>
                  <SelectItem value="DONE">
                    <span className="px-2 py-0.5 rounded bg-green-100 text-green-700 text-sm">
                      Done
                    </span>
                  </SelectItem>
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">
                This is the issue's initial status upon creation
              </p>
            </div>

            {/* Summary */}
            <div className="grid gap-2">
              <Label htmlFor="title" className="text-sm font-medium">
                Summary <span className="text-red-500">*</span>
              </Label>
              <Input
                id="title"
                placeholder="Enter a concise summary"
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                required
                className="h-10"
              />
            </div>

            {/* Description - Rich Text Editor Style */}
            <div className="grid gap-2">
              <Label htmlFor="description" className="text-sm font-medium">
                Description
              </Label>
              <div className="border rounded-lg overflow-hidden">
                {/* Toolbar */}
                <div className="flex items-center justify-between px-3 py-2 border-b bg-gray-50">
                  <div className="flex items-center gap-1">
                    <Select defaultValue="normal">
                      <SelectTrigger className="h-8 w-32 text-xs border-none">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="normal">Normal text</SelectItem>
                        <SelectItem value="h1">Heading 1</SelectItem>
                        <SelectItem value="h2">Heading 2</SelectItem>
                        <SelectItem value="h3">Heading 3</SelectItem>
                      </SelectContent>
                    </Select>
                    <div className="h-4 w-px bg-gray-300 mx-1" />
                    <Button variant="ghost" size="icon" className="h-8 w-8" type="button">
                      <Bold className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" className="h-8 w-8" type="button">
                      <Italic className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" className="h-8 w-8" type="button">
                      <List className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" className="h-8 w-8" type="button">
                      <LinkIcon className="h-4 w-4" />
                    </Button>
                  </div>
                  <Button variant="ghost" size="icon" className="h-8 w-8" type="button">
                    <Sparkles className="h-4 w-4 text-purple-600" />
                  </Button>
                </div>
                {/* Text Area */}
                <Textarea
                  id="description"
                  placeholder="We support markdown! Try **bold**, `inline code`..."
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  rows={6}
                  className="border-none focus-visible:ring-0 resize-none"
                />
              </div>
            </div>
            {/* Assignee */}
            {formData.projectId && (
              <div className="grid gap-2">
                <Label htmlFor="assignee" className="text-sm font-medium">
                  Assignee
                </Label>
                <Select
                  value={formData.assigneeId?.toString() || ""}
                  onValueChange={(value) =>
                    setFormData({ ...formData, assigneeId: value ? parseInt(value) : null })
                  }
                >
                  <SelectTrigger className="h-10">
                    <SelectValue placeholder="Unassigned" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">Unassigned</SelectItem>
                    {members.map((member) => (
                      <SelectItem key={member.userId} value={member.userId.toString()}>
                        <div className="flex items-center gap-2">
                          <div className="h-6 w-6 rounded-full bg-blue-600 flex items-center justify-center text-white text-xs font-bold">
                            {member.name.substring(0, 1)}
                          </div>
                          {member.name}
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}

            {/* Priority and Story Points Row */}
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="priority" className="text-sm font-medium">Priority</Label>
                <Select
                  value={formData.priority}
                  onValueChange={(value) => setFormData({ ...formData, priority: value })}
                >
                  <SelectTrigger className="h-10">
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

              <div className="grid gap-2">
                <Label htmlFor="estimatedHours" className="text-sm font-medium">Story Points</Label>
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
                  className="h-10"
                />
              </div>
            </div>

            {/* Due Date */}
            <div className="grid gap-2">
              <Label htmlFor="dueDate" className="text-sm font-medium">Due Date</Label>
              <Input
                id="dueDate"
                type="date"
                value={formData.dueDate}
                onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
                className="h-10"
              />
            </div>

            {/* Parent Issue Selection for Subtasks */}
            {formData.projectId && formData.type === "SUBTASK" && (
              <div className="grid gap-2">
                <Label htmlFor="parentTask" className="text-sm font-medium">
                  Parent Issue <span className="text-red-500">*</span>
                </Label>
                <Select
                  value={formData.parentTaskId?.toString() || ""}
                  onValueChange={(value) =>
                    setFormData({ ...formData, parentTaskId: value ? parseInt(value) : null })
                  }
                >
                  <SelectTrigger className="h-10">
                    <SelectValue placeholder="Select parent (Story/Task/Bug)" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">No Parent</SelectItem>
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
                <Label htmlFor="epic" className="text-sm font-medium">Epic (Optional)</Label>
                <Select
                  value={formData.epicId?.toString() || ""}
                  onValueChange={(value) =>
                    setFormData({ ...formData, epicId: value ? parseInt(value) : null })
                  }
                >
                  <SelectTrigger className="h-10">
                    <SelectValue placeholder="Select epic" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">No Epic</SelectItem>
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
          </div>

          {/* Footer */}
          <DialogFooter className="px-6 py-4 border-t bg-gray-50 flex-row items-center justify-between">
            <div className="flex items-center gap-2">
              <Checkbox
                id="create-another"
                checked={createAnother}
                onCheckedChange={(checked) => setCreateAnother(checked as boolean)}
              />
              <Label
                htmlFor="create-another"
                className="text-sm font-normal cursor-pointer"
              >
                Create another
              </Label>
            </div>
            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="ghost"
                onClick={() => onOpenChange(false)}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                className="bg-blue-600 hover:bg-blue-700"
                disabled={isPending}
              >
                {isPending ? "Creating..." : "Create"}
              </Button>
            </div>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
