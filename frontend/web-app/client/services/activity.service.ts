import api from "./api";
import type { ApiResponse } from "@/types/auth.types";
import type { ActivityEvent } from "@/types/activity.types";

export const activityService = {
  // Get project activity stream
  async getProjectActivity(
    projectId: number | string,
    page = 0,
    size = 20
  ): Promise<ApiResponse<ActivityEvent[]>> {
    const response = await api.get<ApiResponse<ActivityEvent[]>>(`/projects/${projectId}/activity`, {
      params: { page, size },
    });
    return response.data;
  },
};
