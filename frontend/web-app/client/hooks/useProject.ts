import { useQuery } from "@tanstack/react-query";
import { projectService } from "@/services/project.service";
import type { ProjectDetails } from "@/types/project.types";

export function useProjectById(projectId: string | undefined) {
  return useQuery<ProjectDetails>({
    queryKey: ["project", projectId],
    queryFn: async () => {
      if (!projectId) throw new Error("Project ID is required");
      const response = await projectService.getProjectById(projectId);
      return response.data;
    },
    enabled: !!projectId,
    staleTime: 5 * 60 * 1000, // Cache for 5 minutes
  });
}
