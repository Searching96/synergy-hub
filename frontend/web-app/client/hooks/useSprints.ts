import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { sprintService } from "@/services/sprint.service";
import type { Sprint } from "@/types/sprint.types";

export function useProjectSprints(projectId: string | undefined) {
  return useQuery({
    queryKey: ["sprints", projectId],
    queryFn: async () => {
      if (!projectId) throw new Error("Project ID is required");
      return sprintService.getProjectSprints(projectId);
    },
    enabled: !!projectId,
  });
}

export function useCreateSprint(projectId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (sprintData: {
      name: string;
      goal?: string;
      startDate: string;
      endDate: string;
    }) => {
      const response = await sprintService.createSprint({
        projectId: parseInt(projectId!),
        ...sprintData,
      });
      return response;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sprints", projectId] });
      queryClient.invalidateQueries({ queryKey: ["board", projectId] });
    },
  });
}

export function useStartSprint(projectId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (sprintId: number) => {
      return sprintService.startSprint(sprintId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sprints", projectId] });
      queryClient.invalidateQueries({ queryKey: ["board", projectId] });
    },
  });
}

export function useCompleteSprint(projectId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (sprintId: number) => {
      return sprintService.completeSprint(sprintId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sprints", projectId] });
      queryClient.invalidateQueries({ queryKey: ["board", projectId] });
    },
  });
}
