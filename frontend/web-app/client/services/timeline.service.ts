import api from "./api";
import { ApiResponse } from '@/types/auth.types';

export interface TimelineTask {
  id: number;
  title: string;
  status: string;
  type: string;
  priority: string;
  storyPoints?: number;
  startDate?: string;
  dueDate?: string;
  createdAt?: string;
  sprintId?: number;
  sprintName?: string;
  assigneeId?: number;
  assigneeName?: string;
}

export interface TimelineSprint {
  id: number;
  name: string;
  status: string;
  startDate: string;
  endDate: string;
  totalTasks: number;
  completedTasks: number;
  completionPercentage: number;
}

export interface TimelineView {
  projectId: number;
  projectName: string;
  viewStartDate: string;
  viewEndDate: string;
  sprints: TimelineSprint[];
  tasks: TimelineTask[];
}

class TimelineService {
  async getProjectTimeline(
    projectId: number | string,
    monthsAhead?: number
  ): Promise<ApiResponse<TimelineView>> {
    const params = new URLSearchParams();
    if (monthsAhead) {
      params.append('months', monthsAhead.toString());
    }
    const queryString = params.toString();
    const response = await api.get<ApiResponse<TimelineView>>(
      `/projects/${projectId}/timeline${queryString ? '?' + queryString : ''}`
    );
    return response.data;
  }
}

export default new TimelineService();
