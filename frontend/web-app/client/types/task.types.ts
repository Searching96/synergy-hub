import type { User } from "./auth.types";
import type { Attachment } from "./attachment.types";

// Task Status Enum (matches backend)
export type TaskStatus = "TO_DO" | "IN_PROGRESS" | "IN_REVIEW" | "DONE" | "BLOCKED";

// Task Priority Enum (matches backend)
export type TaskPriority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

// Task Type Enum (matches backend)
export type TaskType = "TASK" | "BUG" | "STORY" | "EPIC" | "SUBTASK";

export interface TaskUser {
  id: number;
  name: string;
  email?: string;
}

export interface Task {
  id: number;
  projectId: number;
  projectName?: string;
  title: string;
  description?: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  type: TaskType;
  sprintId?: number | null;
  sprintName?: string | null;
  assigneeId?: number | null;
  assigneeName?: string | null;
  assignee?: TaskUser | null;
  reporterName?: string;
  reporter?: TaskUser | null;
  createdBy?: TaskUser | null;
  storyPoints?: number | null;
  dueDate?: string | null;
  createdAt: string;
  updatedAt: string;
  overdue?: boolean;
  archived?: boolean;
  commentCount?: number;
  estimatedHours?: number;
  actualHours?: number;
  goal?: string;
  // Issue hierarchy fields (Jira-style)
  parentTaskId?: number | null;
  epicId?: number | null;
  subtasks?: Task[];
  parentTask?: { id: number; title: string; type: TaskType } | null;
  epic?: { id: number; title: string } | null;
  attachments?: Attachment[];
  [key: string]: unknown;
}

export interface CreateTaskRequest {
  projectId: number;
  title: string;
  description?: string | null;
  priority?: TaskPriority;
  type?: TaskType;
  storyPoints?: number | null;
  dueDate?: string | null;
  sprintId?: number | null;
  parentTaskId?: number | null;
  assigneeId?: number | null;
  reporterId?: number | null;
  status?: string;
  labels?: string[];
  startDate?: string | null;
  estimatedHours?: string | number | null;
}

export interface UpdateTaskRequest extends Partial<CreateTaskRequest> {
  status?: string;
  position?: number;
  estimatedHours?: string | number | null;
  actualHours?: number | null;
}

export interface TaskFilters {
  [key: string]: string | number | boolean | null | undefined;
}

export interface BoardTask {
  id: number;
  title: string;
  status: string;
  priority: string;
  assignee?: TaskUser | null;
  completedAt?: string;
  storyPoints?: number | null;
  archived: boolean;
}

export interface ActiveSprintSummary {
  id: number;
  name: string;
  status: string;
  startDate: string;
  endDate: string;
  tasks: BoardTask[];
  metrics?: {
    completionPercentage: number;
  };
}

export interface BoardData {
  activeSprints: ActiveSprintSummary[];
  backlogTasks: BoardTask[];
}

export interface BacklogTask extends Task {
  sprintName?: string | null;
  epicName?: string | null;
}
