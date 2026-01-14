import { useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { useQuery } from "@tanstack/react-query";
import { useProjects } from "@/hooks/useProjects";
import { projectService } from "@/services/project.service";
import { taskService } from "@/services/task.service";
import { cn } from "@/lib/utils";
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
  Bug,
  ListChecks,
  X
} from "lucide-react";
import { AttachmentDropzone } from "@/components/issue/AttachmentDropzone";

const issueSchema = z.object({
  projectId: z.string().min(1, "Project is required"),
  title: z.string().min(3, "Title must be at least 3 characters").max(200, "Title is too long"),
  description: z.string().optional(),
  type: z.enum(["BUG", "STORY", "TASK", "EPIC", "SUBTASK"]),
  status: z.enum(["TO_DO", "IN_PROGRESS", "IN_REVIEW", "DONE", "BLOCKED"]).optional(),
  priority: z.enum(["LOW", "MEDIUM", "HIGH", "CRITICAL"]).optional(),
  estimatedHours: z.string().optional().refine(val => !val || !isNaN(parseFloat(val)), "Must be a number"),
  startDate: z.string().optional(),
  dueDate: z.string().optional(),
  assigneeId: z.number().nullable().optional(),
  reporterId: z.number().nullable().optional(),
  parentTaskId: z.number().nullable().optional(),
  labels: z.array(z.string()).optional(),
  issueColor: z.string().optional(),
  linkedIssues: z.array(z.string()).optional(),
  restrictedRoles: z.array(z.string()).optional(),
  attachments: z.array(z.instanceof(File)).optional(),
  createAnother: z.boolean().optional(),
}).refine((data) => {
  if (data.type === "SUBTASK" && !data.parentTaskId) {
    return false;
  }
  return true;
}, {
  message: "Subtasks must have a parent issue",
  path: ["parentTaskId"],
});

export type IssueFormValues = z.infer<typeof issueSchema>;

interface IssueFormProps {
  initialValues?: Partial<IssueFormValues>;
  onSubmit: (values: IssueFormValues) => void;
  isSubmitting?: boolean;
  onCancel?: () => void;
}

const TYPE_ICONS = {
  EPIC: { icon: Zap, color: "text-purple-600" },
  STORY: { icon: Lightbulb, color: "text-green-600" },
  TASK: { icon: CheckSquare, color: "text-blue-600" },
  BUG: { icon: Bug, color: "text-red-600" },
  SUBTASK: { icon: ListChecks, color: "text-gray-600" },
};

const AVAILABLE_COLORS = [
  { name: "Red", value: "red" },
  { name: "Orange", value: "orange" },
  { name: "Yellow", value: "yellow" },
  { name: "Green", value: "green" },
  { name: "Blue", value: "blue" },
  { name: "Purple", value: "purple" },
  { name: "Pink", value: "pink" },
  { name: "Gray", value: "gray" },
];

const AVAILABLE_ROLES = [
  { label: "Developer", value: "DEVELOPER" },
  { label: "Manager", value: "MANAGER" },
  { label: "Viewer", value: "VIEWER" },
  { label: "Editor", value: "EDITOR" },
];

const DEFAULT_VALUES: Partial<IssueFormValues> = {
  projectId: "",
  title: "",
  description: "",
  type: "TASK",
  status: "TO_DO",
  priority: "MEDIUM",
  labels: [],
  restrictedRoles: [],
  attachments: [],
  createAnother: false,
};

export default function IssueForm({
  initialValues,
  onSubmit,
  isSubmitting = false,
  onCancel,
}: IssueFormProps) {
  const { toast } = useToast();
  const { data: projectsResponse } = useProjects();
  const projects = projectsResponse?.data?.content || [];

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    formState: { errors },
    reset,
  } = useForm<IssueFormValues>({
    resolver: zodResolver(issueSchema),
    defaultValues: initialValues ? { ...DEFAULT_VALUES, ...initialValues } : DEFAULT_VALUES,
  });

  const selectedProjectId = watch("projectId");
  const selectedType = watch("type");
  const labels = watch("labels") || [];
  const linkedIssues = watch("linkedIssues") || [];
  const restrictedRoles = watch("restrictedRoles") || [];

  const [labelInput, setLabelInput] = useState("");
  const [linkedIssueInput, setLinkedIssueInput] = useState("");

  // Load members for selected project
  const { data: membersResponse } = useQuery({
    queryKey: ["project-members", selectedProjectId],
    queryFn: () => projectService.getProjectMembers(parseInt(selectedProjectId)),
    enabled: !!selectedProjectId,
  });
  const members = membersResponse?.data?.filter((m) => m && m.userId) || [];

  // Load potential parent tasks for the project
  const { data: tasksResponse } = useQuery({
    queryKey: ["project-tasks", selectedProjectId],
    queryFn: () => taskService.getProjectTasks(selectedProjectId),
    enabled: !!selectedProjectId,
  });

  const potentialParents = (tasksResponse?.data
    ? (Array.isArray(tasksResponse.data) ? tasksResponse.data : tasksResponse.data.content)
    : []
  ).filter(t => ["STORY", "TASK", "BUG", "EPIC"].includes(t.type));

  const activeProjects = projects.filter(project => project.status !== "ARCHIVED");

  const onFormSubmit = (data: IssueFormValues) => {
    onSubmit(data);
  };

  const addLabel = () => {
    if (labelInput.trim() && !labels.includes(labelInput.trim())) {
      setValue("labels", [...labels, labelInput.trim()]);
      setLabelInput("");
    }
  };

  const removeLabel = (label: string) => {
    setValue("labels", labels.filter(l => l !== label));
  };

  const addLinkedIssue = () => {
    if (linkedIssueInput.trim() && !linkedIssues.includes(linkedIssueInput.trim())) {
      setValue("linkedIssues", [...linkedIssues, linkedIssueInput.trim()]);
      setLinkedIssueInput("");
    }
  };

  const removeLinkedIssue = (issue: string) => {
    setValue("linkedIssues", linkedIssues.filter(i => i !== issue));
  };

  const toggleRole = (role: string) => {
    const newRoles = restrictedRoles.includes(role)
      ? restrictedRoles.filter(r => r !== role)
      : [...restrictedRoles, role];
    setValue("restrictedRoles", newRoles);
  };

  return (
    <form onSubmit={handleSubmit(onFormSubmit)} className="flex flex-col flex-1 w-full overflow-hidden">
      {/* Scrollable Form Body */}
      <div className="flex-1 min-h-0 overflow-y-auto px-6 py-4 space-y-5">
        {/* Meta Row */}
        <div className="flex items-center justify-between text-sm">
          <span className="text-muted-foreground">
            Required fields are marked with an asterisk <span className="text-red-500">*</span>
          </span>
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="sm" type="button">
              <Eye className="h-4 w-4 mr-1" />
              Watch
            </Button>
            <Button variant="ghost" size="icon" type="button">
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </div>
        </div>

        {/* Project Selection */}
        <div className="grid gap-2">
          <Label htmlFor="project" className="text-sm font-medium">
            Project <span className="text-red-500">*</span>
          </Label>
          <Controller
            name="projectId"
            control={control}
            render={({ field }) => (
              <Select onValueChange={field.onChange} value={field.value}>
                <SelectTrigger id="project" className={cn("h-10", errors.projectId && "border-red-500")}>
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
            )}
          />
          {errors.projectId && (
            <p className="text-xs text-red-500">{errors.projectId.message}</p>
          )}
        </div>

        {/* Issue Type */}
        <div className="grid gap-2">
          <Label htmlFor="type" className="text-sm font-medium">
            Issue type <span className="text-red-500">*</span>
          </Label>
          <Controller
            name="type"
            control={control}
            render={({ field }) => (
              <Select
                onValueChange={(val) => {
                  field.onChange(val);
                  setValue("parentTaskId", null);
                }}
                value={field.value}
              >
                <SelectTrigger id="type" className="h-10">
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
            )}
          />
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
          <Controller
            name="status"
            control={control}
            render={({ field }) => (
              <Select onValueChange={field.onChange} value={field.value}>
                <SelectTrigger id="status" className="h-10">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="TO_DO">
                    <span className="px-2 py-0.5 rounded bg-gray-100 text-gray-700 text-sm">To Do</span>
                  </SelectItem>
                  <SelectItem value="IN_PROGRESS">
                    <span className="px-2 py-0.5 rounded bg-blue-100 text-blue-700 text-sm">In Progress</span>
                  </SelectItem>
                  <SelectItem value="DONE">
                    <span className="px-2 py-0.5 rounded bg-green-100 text-green-700 text-sm">Done</span>
                  </SelectItem>
                </SelectContent>
              </Select>
            )}
          />
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
            {...register("title")}
            className={cn("h-10", errors.title && "border-red-500")}
          />
          {errors.title && (
            <p className="text-xs text-red-500">{errors.title.message}</p>
          )}
        </div>

        {/* Description */}
        <div className="grid gap-2">
          <Label htmlFor="description" className="text-sm font-medium">
            Description
          </Label>
          <div className="border rounded-lg overflow-hidden">
            {/* Toolbar (Simplified for now) */}
            <div className="flex items-center justify-between px-3 py-2 border-b bg-gray-50">
              <div className="flex items-center gap-1">
                <Button variant="ghost" size="icon" className="h-8 w-8" type="button"><Bold className="h-4 w-4" /></Button>
                <Button variant="ghost" size="icon" className="h-8 w-8" type="button"><Italic className="h-4 w-4" /></Button>
                <Button variant="ghost" size="icon" className="h-8 w-8" type="button"><List className="h-4 w-4" /></Button>
              </div>
            </div>
            <Textarea
              id="description"
              placeholder="Describe the issue..."
              {...register("description")}
              rows={6}
              className="border-none focus-visible:ring-0 resize-none"
            />
          </div>
        </div>

        {/* Assignee and Reporter */}
        {selectedProjectId && (
          <div className="grid grid-cols-2 gap-4">
            <div className="grid gap-2">
              <Label htmlFor="assignee" className="text-sm font-medium">Assignee</Label>
              <Controller
                name="assigneeId"
                control={control}
                render={({ field }) => (
                  <Select onValueChange={(val) => field.onChange(val === "unassigned" ? null : parseInt(val))} value={field.value?.toString() || "unassigned"}>
                    <SelectTrigger id="assignee" className="h-10">
                      <SelectValue placeholder="Unassigned" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="unassigned">Unassigned</SelectItem>
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
                )}
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="reporter" className="text-sm font-medium">Reporter</Label>
              <Controller
                name="reporterId"
                control={control}
                render={({ field }) => (
                  <Select onValueChange={(val) => field.onChange(val === "current" ? null : parseInt(val))} value={field.value?.toString() || "current"}>
                    <SelectTrigger id="reporter" className="h-10">
                      <SelectValue placeholder="Current User" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="current">Current User</SelectItem>
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
                )}
              />
            </div>
          </div>
        )}

        {/* Labels */}
        <div className="grid gap-2">
          <Label htmlFor="labels" className="text-sm font-medium">Labels</Label>
          <div className="flex gap-2">
            <Input
              id="labels"
              placeholder="Add label..."
              value={labelInput}
              onChange={(e) => setLabelInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  addLabel();
                }
              }}
              className="h-10"
            />
            <Button type="button" variant="outline" onClick={addLabel} className="px-3">Add</Button>
          </div>
          {labels.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {labels.map((label) => (
                <div key={label} className="bg-blue-100 text-blue-700 px-3 py-1 rounded-full text-sm flex items-center gap-2">
                  {label}
                  <button type="button" onClick={() => removeLabel(label)} className="hover:text-blue-900"><X className="h-3 w-3" /></button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Parent Issue */}
        {selectedProjectId && selectedType === "SUBTASK" && (
          <div className="grid gap-2">
            <Label htmlFor="parentTask" className="text-sm font-medium">
              Parent Issue <span className="text-red-500">*</span>
            </Label>
            <Controller
              name="parentTaskId"
              control={control}
              render={({ field }) => (
                <Select onValueChange={(val) => field.onChange(val === "none" ? null : parseInt(val))} value={field.value?.toString() || "none"}>
                  <SelectTrigger className={cn("h-10", errors.parentTaskId && "border-red-500")}>
                    <SelectValue placeholder="Select parent (Story/Task/Bug)" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">No Parent</SelectItem>
                    {potentialParents.map((task) => (
                      <SelectItem key={task.id} value={task.id.toString()}>
                        {task.projectKey ? `${task.projectKey}-${task.id}: ` : ""}{task.title}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.parentTaskId && (
              <p className="text-xs text-red-500">{errors.parentTaskId.message}</p>
            )}
          </div>
        )}

        {/* Start Date and Due Date */}
        <div className="grid grid-cols-2 gap-4">
          <div className="grid gap-2">
            <Label htmlFor="startDate" className="text-sm font-medium">Start Date</Label>
            <Input id="startDate" type="date" {...register("startDate")} className="h-10" />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="dueDate" className="text-sm font-medium">Due Date</Label>
            <Input id="dueDate" type="date" {...register("dueDate")} className="h-10" />
          </div>
        </div>

        {/* Issue Color */}
        <div className="grid gap-2">
          <Label htmlFor="issueColor" className="text-sm font-medium">Issue Color</Label>
          <Controller
            name="issueColor"
            control={control}
            render={({ field }) => (
              <div className="flex gap-2">
                {AVAILABLE_COLORS.map((color) => (
                  <button
                    key={color.value}
                    type="button"
                    onClick={() => field.onChange(color.value)}
                    className={cn(
                      "h-10 w-10 rounded border-2 transition",
                      field.value === color.value ? "border-gray-900" : "border-gray-300",
                      `bg-${color.value}-500 hover:border-gray-900`
                    )}
                    title={color.name}
                  />
                ))}
              </div>
            )}
          />
        </div>

        {/* Linked Issues */}
        <div className="grid gap-2">
          <Label htmlFor="linkedIssues" className="text-sm font-medium">Linked Issues</Label>
          <div className="flex gap-2">
            <Input
              id="linkedIssues"
              placeholder="Link issue (e.g., PROJ-123)..."
              value={linkedIssueInput}
              onChange={(e) => setLinkedIssueInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  addLinkedIssue();
                }
              }}
              className="h-10"
            />
            <Button type="button" variant="outline" onClick={addLinkedIssue} className="px-3">Link</Button>
          </div>
          {linkedIssues.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {linkedIssues.map((issue) => (
                <div key={issue} className="bg-purple-100 text-purple-700 px-3 py-1 rounded text-sm flex items-center gap-2">
                  {issue}
                  <button type="button" onClick={() => removeLinkedIssue(issue)} className="hover:text-purple-900"><X className="h-3 w-3" /></button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* File Attachments */}
        <div className="grid gap-2">
          <Label htmlFor="attachments" className="text-sm font-medium">File Attachments</Label>
          <Controller
            name="attachments"
            control={control}
            render={({ field }) => (
              <AttachmentDropzone
                onFilesSelected={field.onChange}
                maxFiles={5}
                maxFileSize={10}
                showPreview={true}
                acceptedTypes={["image/*", "application/pdf", "text/*", ".zip", ".rar"]}
              />
            )}
          />
        </div>

        {/* Restrict to Selected Roles */}
        <div className="grid gap-2">
          <Label className="text-sm font-medium">Restrict Access to Roles</Label>
          <div className="space-y-2">
            {AVAILABLE_ROLES.map((role) => (
              <div key={role.value} className="flex items-center gap-2">
                <Checkbox
                  id={`role-${role.value}`}
                  checked={restrictedRoles.includes(role.value)}
                  onCheckedChange={() => toggleRole(role.value)}
                />
                <Label htmlFor={`role-${role.value}`} className="text-sm font-normal cursor-pointer">
                  {role.label}
                </Label>
              </div>
            ))}
          </div>
          <p className="text-xs text-muted-foreground">
            Leave empty to allow all roles to view this issue
          </p>
        </div>
      </div>

      {/* Footer */}
      <div className="flex-shrink-0 px-6 py-4 border-t bg-gray-50 flex items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <Controller
            name="createAnother"
            control={control}
            render={({ field }) => (
              <Checkbox
                id="create-another"
                checked={field.value}
                onCheckedChange={field.onChange}
              />
            )}
          />
          <Label htmlFor="create-another" className="text-sm font-normal cursor-pointer">
            Create another issue
          </Label>
        </div>
        <div className="flex items-center gap-2">
          {onCancel && (
            <Button
              type="button"
              variant="ghost"
              onClick={onCancel}
            >
              Cancel
            </Button>
          )}
          <Button
            type="submit"
            className="bg-blue-600 hover:bg-blue-700"
            disabled={isSubmitting}
          >
            {isSubmitting ? "Saving..." : initialValues ? "Update" : "Create"}
          </Button>
        </div>
      </div>
    </form>
  );
}