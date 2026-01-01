import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { sprintService } from "@/services/sprint.service";

export interface Sprint {
  id: number;
  name: string;
  goal?: string;
  status: "PLANNING" | "PLANNED" | "ACTIVE" | "COMPLETED" | "CANCELLED";
  startDate: string;
  endDate: string;
  taskCount?: number;
  completedTaskCount?: number;
  projectId: number;
}

export function useProjectSprints(projectId: string | undefined) {
  return useQuery({
    queryKey: ["sprints", projectId],
    queryFn: async () => {
      if (!projectId) throw new Error("Project ID is required");
      const response = await sprintService.getProjectSprints(projectId);
      return response.data as Sprint[];
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
      return response.data;
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
      const response = await sprintService.startSprint(sprintId);
      return response.data;
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
      const response = await sprintService.completeSprint(sprintId);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sprints", projectId] });
      queryClient.invalidateQueries({ queryKey: ["board", projectId] });
    },
  });
}
