/**
 * Sprint domain types
 */

export type SprintStatus = "PLANNING" | "PLANNED" | "ACTIVE" | "COMPLETED" | "CANCELLED";

export interface SprintTaskAssignee {
  id: number;
  name: string;
}

export interface SprintTask {
  id: number;
  title: string;
  status: string;
  priority?: string;
  assignee?: SprintTaskAssignee;
  completedAt?: string;
  storyPoints?: number;
  archived?: boolean;
  sprintId?: number | null;
}

export interface SprintMetrics {
  completionPercentage?: number;
  velocity?: number;
}

export interface Sprint {
  id: number;
  name: string;
  goal?: string;
  status: SprintStatus;
  startDate: string;
  endDate: string;
  taskCount?: number;
  completedTaskCount?: number;
  progress?: number;
  projectId: number;
  metrics?: SprintMetrics;
  tasks?: SprintTask[];
}

export interface SprintDetails extends Sprint {
  burndownData?: {
    totalPoints: number;
    remainingPoints: number;
  };
}

export interface CreateSprintRequest {
  projectId: number;
  name: string;
  goal?: string;
  startDate: string;
  endDate: string;
}

export interface UpdateSprintRequest {
  name?: string;
  goal?: string;
  startDate?: string;
  endDate?: string;
  status?: SprintStatus;
}
