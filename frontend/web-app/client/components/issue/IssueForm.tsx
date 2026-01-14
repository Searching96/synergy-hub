import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useProjects } from "@/hooks/useProjects";
import { projectService } from "@/services/project.service";
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

export interface IssueFormValues {
  projectId: string;
  title: string;
  description: string;
  type: "BUG" | "STORY" | "TASK" | "EPIC" | "SUBTASK";
  status?: "TO_DO" | "IN_PROGRESS" | "IN_REVIEW" | "DONE" | "BLOCKED";
  startDate?: string;
  dueDate?: string;
  assigneeId?: number | null;
  reporterId?: number | null;
  parentTaskId?: number | null;
  labels?: string[];
  issueColor?: string;
  linkedIssues?: string[];
  restrictedRoles?: string[];
  attachments?: File[];
  createAnother?: boolean;
}

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

const INITIAL_FORM_STATE: IssueFormValues = {
  projectId: "",
  title: "",
  description: "",
  type: "TASK",
  status: "TO_DO",
  startDate: "",
  dueDate: "",
  assigneeId: null,
  reporterId: null,
  parentTaskId: null,
  labels: [],
  issueColor: "",
  linkedIssues: [],
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
  const [formData, setFormData] = useState<IssueFormValues>(
    initialValues ? { ...INITIAL_FORM_STATE, ...initialValues } : INITIAL_FORM_STATE
  );
  const [createAnother, setCreateAnother] = useState(false);
  const [labelInput, setLabelInput] = useState("");
  const [linkedIssueInput, setLinkedIssueInput] = useState("");

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
      onSubmit({
        ...formData,
        createAnother,
      });
    } catch (error) {
      console.error("Failed to submit form:", error);
    }
  };

  const addLabel = () => {
    if (labelInput.trim() && formData.labels && !formData.labels.includes(labelInput.trim())) {
      setFormData(prev => ({
        ...prev,
        labels: [...(prev.labels || []), labelInput.trim()]
      }));
      setLabelInput("");
    }
  };

  const removeLabel = (label: string) => {
    setFormData(prev => ({
      ...prev,
      labels: (prev.labels || []).filter(l => l !== label)
    }));
  };

  const addLinkedIssue = () => {
    if (linkedIssueInput.trim() && formData.linkedIssues && !formData.linkedIssues.includes(linkedIssueInput.trim())) {
      setFormData(prev => ({
        ...prev,
        linkedIssues: [...(prev.linkedIssues || []), linkedIssueInput.trim()]
      }));
      setLinkedIssueInput("");
    }
  };

  const removeLinkedIssue = (issue: string) => {
    setFormData(prev => ({
      ...prev,
      linkedIssues: (prev.linkedIssues || []).filter(i => i !== issue)
    }));
  };

  const toggleRole = (role: string) => {
    setFormData(prev => ({
      ...prev,
      restrictedRoles: (prev.restrictedRoles || []).includes(role)
        ? (prev.restrictedRoles || []).filter(r => r !== role)
        : [...(prev.restrictedRoles || []), role]
    }));
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-col flex-1 w-full overflow-hidden">
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
                type: value as IssueFormValues["type"],
                parentTaskId: null,
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
          <a href="#" className="text-xs text-blue-600 hover:underline">
            Learn about issue types
          </a>
        </div>

        {/* Status */}
        <div className="grid gap-2">
          <Label htmlFor="status" className="text-sm font-medium flex items-center gap-1">
            Status
            <Info className="h-3.5 w-3.5 text-muted-foreground" />
          </Label>
          <Select
            value={formData.status || "TO_DO"}
            onValueChange={(value) => setFormData({ ...formData, status: value as IssueFormValues["status"] })}
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

        {/* Assignee and Reporter */}
        {formData.projectId && (
          <div className="grid grid-cols-2 gap-4">
            <div className="grid gap-2">
              <Label htmlFor="assignee" className="text-sm font-medium">
                Assignee
              </Label>
              <Select
                value={formData.assigneeId?.toString() || "unassigned"}
                onValueChange={(value) =>
                  setFormData({ ...formData, assigneeId: value === "unassigned" ? null : parseInt(value) })
                }
              >
                <SelectTrigger className="h-10">
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
            </div>

            <div className="grid gap-2">
              <Label htmlFor="reporter" className="text-sm font-medium">
                Reporter
              </Label>
              <Select
                value={formData.reporterId?.toString() || "current"}
                onValueChange={(value) =>
                  setFormData({ ...formData, reporterId: value === "current" ? null : parseInt(value) })
                }
              >
                <SelectTrigger className="h-10">
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
            </div>
          </div>
        )}

        {/* Labels */}
        <div className="grid gap-2">
          <Label htmlFor="labels" className="text-sm font-medium">
            Labels
          </Label>
          <div className="flex gap-2">
            <Input
              id="labels"
              placeholder="Add label..."
              value={labelInput}
              onChange={(e) => setLabelInput(e.target.value)}
              onKeyPress={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  addLabel();
                }
              }}
              className="h-10"
            />
            <Button
              type="button"
              variant="outline"
              onClick={addLabel}
              className="px-3"
            >
              Add
            </Button>
          </div>
          {formData.labels && formData.labels.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {formData.labels.map((label) => (
                <div
                  key={label}
                  className="bg-blue-100 text-blue-700 px-3 py-1 rounded-full text-sm flex items-center gap-2"
                >
                  {label}
                  <button
                    type="button"
                    onClick={() => removeLabel(label)}
                    className="hover:text-blue-900"
                  >
                    <X className="h-3 w-3" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Parent Issue */}
        {formData.projectId && formData.type === "SUBTASK" && (
          <div className="grid gap-2">
            <Label htmlFor="parentTask" className="text-sm font-medium">
              Parent Issue <span className="text-red-500">*</span>
            </Label>
            <Select
              value={formData.parentTaskId?.toString() || "none"}
              onValueChange={(value) =>
                setFormData({ ...formData, parentTaskId: value === "none" ? null : parseInt(value) })
              }
            >
              <SelectTrigger className="h-10">
                <SelectValue placeholder="Select parent (Story/Task/Bug)" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="none">No Parent</SelectItem>
                <SelectItem value="1001">PROJ-1: Implement user authentication</SelectItem>
                <SelectItem value="1002">PROJ-2: Create dashboard layout</SelectItem>
                <SelectItem value="1003">PROJ-3: Fix responsive issues</SelectItem>
              </SelectContent>
            </Select>
          </div>
        )}

        {/* Start Date and Due Date */}
        <div className="grid grid-cols-2 gap-4">
          <div className="grid gap-2">
            <Label htmlFor="startDate" className="text-sm font-medium">Start Date</Label>
            <Input
              id="startDate"
              type="date"
              value={formData.startDate}
              onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
              className="h-10"
            />
          </div>
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
        </div>

        {/* Issue Color */}
        <div className="grid gap-2">
          <Label htmlFor="issueColor" className="text-sm font-medium">
            Issue Color
          </Label>
          <div className="flex gap-2">
            {AVAILABLE_COLORS.map((color) => (
              <button
                key={color.value}
                type="button"
                onClick={() => setFormData({ ...formData, issueColor: color.value })}
                className={`h-10 w-10 rounded border-2 ${
                  formData.issueColor === color.value ? "border-gray-900" : "border-gray-300"
                } bg-${color.value}-500 hover:border-gray-900 transition`}
                title={color.name}
              />
            ))}
          </div>
        </div>

        {/* Linked Issues */}
        <div className="grid gap-2">
          <Label htmlFor="linkedIssues" className="text-sm font-medium">
            Linked Issues
          </Label>
          <div className="flex gap-2">
            <Input
              id="linkedIssues"
              placeholder="Link issue (e.g., PROJ-123)..."
              value={linkedIssueInput}
              onChange={(e) => setLinkedIssueInput(e.target.value)}
              onKeyPress={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  addLinkedIssue();
                }
              }}
              className="h-10"
            />
            <Button
              type="button"
              variant="outline"
              onClick={addLinkedIssue}
              className="px-3"
            >
              Link
            </Button>
          </div>
          {formData.linkedIssues && formData.linkedIssues.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {formData.linkedIssues.map((issue) => (
                <div
                  key={issue}
                  className="bg-purple-100 text-purple-700 px-3 py-1 rounded text-sm flex items-center gap-2"
                >
                  {issue}
                  <button
                    type="button"
                    onClick={() => removeLinkedIssue(issue)}
                    className="hover:text-purple-900"
                  >
                    <X className="h-3 w-3" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* File Attachments - REPLACED WITH ATTACHMENTDROPZONE */}
        <div className="grid gap-2">
          <Label htmlFor="attachments" className="text-sm font-medium">
            File Attachments
          </Label>
          <AttachmentDropzone
            onFilesSelected={(files) => {
              setFormData(prev => ({
                ...prev,
                attachments: files
              }));
            }}
            maxFiles={5}
            maxFileSize={10}
            showPreview={true}
            acceptedTypes={[
              "image/*",
              "application/pdf",
              "application/msword",
              "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
              "application/vnd.ms-excel",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              "text/*",
              ".zip",
              ".rar"
            ]}
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
                  checked={(formData.restrictedRoles || []).includes(role.value)}
                  onCheckedChange={() => toggleRole(role.value)}
                />
                <Label
                  htmlFor={`role-${role.value}`}
                  className="text-sm font-normal cursor-pointer"
                >
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
          <Checkbox
            id="create-another"
            checked={createAnother}
            onCheckedChange={(checked) => setCreateAnother(checked as boolean)}
          />
          <Label
            htmlFor="create-another"
            className="text-sm font-normal cursor-pointer"
          >
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