export interface Project {
  id: number;
  name: string;
  description: string;
  role: string;
  memberCount: number;
  taskCount: number;
  completedTaskCount: number;
  startDate: string;
  endDate: string;
  status?: string;
  owner?: {
    id: number;
    name: string;
    email: string;
  };
}

export interface ProjectDetails extends Project {
  members: ProjectMember[];
  stats: ProjectStats;
}

export interface ProjectMemberUser {
  id: number;
  name: string;
  email: string;
}

export interface ProjectMember {
  userId?: number;
  name?: string;
  email?: string;
  role: string;
  joinedAt?: string;
  user?: ProjectMemberUser;
}

export interface ProjectStats {
  totalTasks: number;
  completedTasks: number;
  inProgressTasks: number;
  todoTasks: number;
  activeSprints: number;
  completedSprints: number;
}

export interface CreateProjectDto {
  name: string;
  description?: string;
  startDate?: string;
  endDate?: string;
}

export interface UpdateProjectDto {
  name?: string;
  description?: string;
  endDate?: string;
}
