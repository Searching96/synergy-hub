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
  teamId?: number;
  teamName?: string;
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
  teamId?: number;
}

export interface UpdateProjectDto {
  name?: string;
  description?: string;
  startDate?: string;
  endDate?: string;
  teamId?: number;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ProjectFilter {
  search?: string;
  status?: string;
  page?: number;
  size?: number;
}
