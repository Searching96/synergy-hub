import { createContext, useContext, ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { projectService } from "@/services/project.service";

interface ProjectOwner {
  id: number;
  name: string;
  email: string;
}

interface ProjectMember {
  userId: number;
  name: string;
  email: string;
  role: string;
  joinedAt: string;
}

interface ProjectStats {
  totalTasks: number;
  completedTasks: number;
  inProgressTasks: number;
  todoTasks: number;
  activeSprints: number;
  completedSprints: number;
}

export interface Project {
  id: number;
  name: string;
  description: string;
  startDate: string;
  endDate: string;
  status: string;
  owner: ProjectOwner;
  members: ProjectMember[];
  stats: ProjectStats;
}

interface ProjectContextType {
  project: Project | undefined;
  isLoading: boolean;
  error: Error | null;
  refetch: () => void;
}

const ProjectContext = createContext<ProjectContextType | undefined>(undefined);

export function ProjectProvider({ children }: { children: ReactNode }) {
  const { projectId } = useParams<{ projectId: string }>();

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["project", projectId],
    queryFn: async () => {
      if (!projectId) throw new Error("Project ID is required");
      const response = await projectService.getProjectById(projectId);
      return response.data as Project;
    },
    enabled: !!projectId,
    staleTime: 5 * 60 * 1000, // Cache for 5 minutes
  });

  return (
    <ProjectContext.Provider
      value={{
        project: data,
        isLoading,
        error: error as Error | null,
        refetch,
      }}
    >
      {children}
    </ProjectContext.Provider>
  );
}

export function useProject() {
  const context = useContext(ProjectContext);
  if (context === undefined) {
    throw new Error("useProject must be used within a ProjectProvider");
  }
  return context;
}
