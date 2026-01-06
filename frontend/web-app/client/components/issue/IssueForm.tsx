import { useEffect, useRef, useState } from "react";
import { Bug, CheckSquare, Lightbulb, Zap, ListChecks } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export interface IssueFormValues {
  projectId: number | null;
  title: string;
  description: string;
  priority: string;
  type: "BUG" | "STORY" | "TASK" | "EPIC" | "SUBTASK";
  assigneeId: number | null;
}

interface IssueFormProps {
  initialValues: IssueFormValues;
  projects?: Array<{ id: number; name: string }>;
  members?: Array<{ id: number; name: string }>;
  isSubmitting?: boolean;
  onSubmit: (values: IssueFormValues) => void;
  onCancel?: () => void;
}

const typeOptions = [
  { value: "EPIC" as const, label: "Epic", icon: Zap, color: "text-purple-600" },
  { value: "STORY" as const, label: "Story", icon: Lightbulb, color: "text-green-600" },
  { value: "TASK" as const, label: "Task", icon: CheckSquare, color: "text-blue-600" },
  { value: "BUG" as const, label: "Bug", icon: Bug, color: "text-red-600" },
  { value: "SUBTASK" as const, label: "Subtask", icon: ListChecks, color: "text-gray-600" },
];

export default function IssueForm({
  initialValues,
  projects = [],
  members = [],
  isSubmitting = false,
  onSubmit,
  onCancel,
}: IssueFormProps) {
  const [values, setValues] = useState<IssueFormValues>(initialValues);
  const descriptionRef = useRef<HTMLTextAreaElement | null>(null);

  useEffect(() => {
    setValues(initialValues);
  }, [initialValues]);

  useEffect(() => {
    if (descriptionRef.current) {
      descriptionRef.current.style.height = "auto";
      descriptionRef.current.style.height = `${descriptionRef.current.scrollHeight}px`;
    }
  }, [values.description]);

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    onSubmit(values);
  };

  return (
    <form className="space-y-4" onSubmit={handleSubmit}>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="project">Project</Label>
          <Select
            value={values.projectId ? values.projectId.toString() : ""}
            onValueChange={(value) =>
              setValues((prev) => ({ ...prev, projectId: value ? parseInt(value, 10) : null }))
            }
          >
            <SelectTrigger id="project">
              <SelectValue placeholder="Select project" />
            </SelectTrigger>
            <SelectContent>
              {projects.map((project) => (
                <SelectItem key={project.id} value={project.id.toString()}>
                  {project.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-2">
          <Label htmlFor="type">Type</Label>
          <Select
            value={values.type}
            onValueChange={(value) =>
              setValues((prev) => ({ ...prev, type: value as IssueFormValues["type"] }))
            }
          >
            <SelectTrigger id="type">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {typeOptions.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  <span className="flex items-center gap-2">
                    <option.icon className={`h-4 w-4 ${option.color}`} />
                    {option.label}
                  </span>
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="title">Title</Label>
        <Input
          id="title"
          value={values.title}
          onChange={(event) => setValues((prev) => ({ ...prev, title: event.target.value }))}
          placeholder="e.g. Fix login button alignment"
          required
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="description">Description</Label>
        <Textarea
          id="description"
          ref={descriptionRef}
          value={values.description}
          onChange={(event) => setValues((prev) => ({ ...prev, description: event.target.value }))}
          rows={4}
          placeholder="Use markdown to describe the work..."
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="space-y-2">
          <Label htmlFor="priority">Priority</Label>
          <Select
            value={values.priority}
            onValueChange={(value) => setValues((prev) => ({ ...prev, priority: value }))}
          >
            <SelectTrigger id="priority">
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

        <div className="space-y-2">
          <Label htmlFor="assignee">Assignee</Label>
          <Select
            value={values.assigneeId ? values.assigneeId.toString() : "unassigned"}
            onValueChange={(value) =>
              setValues((prev) => ({
                ...prev,
                assigneeId: value === "unassigned" ? null : parseInt(value, 10),
              }))
            }
          >
            <SelectTrigger id="assignee">
              <SelectValue placeholder="Unassigned" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="unassigned">Unassigned</SelectItem>
              {members.filter((member) => member && member.id).map((member) => (
                <SelectItem key={member.id} value={member.id.toString()}>
                  {member.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="flex items-center justify-end gap-2">
        {onCancel && (
          <Button type="button" variant="outline" onClick={onCancel}>
            Cancel
          </Button>
        )}
        <Button type="submit" disabled={isSubmitting || !values.projectId}>
          {isSubmitting ? "Saving..." : "Save"}
        </Button>
      </div>
    </form>
  );
}
