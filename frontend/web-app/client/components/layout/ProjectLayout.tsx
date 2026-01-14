import { Outlet, Navigate } from "react-router-dom";
import { ProjectProvider, useProject } from "@/context/ProjectContext";
import ProjectSidebar from "./ProjectSidebar";
import { Loader2 } from "lucide-react";

function ProjectLayoutContent() {
  const { project, isLoading, error } = useProject();

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-center">
          <h2 className="text-xl font-semibold text-destructive mb-2">
            Failed to Load Project
          </h2>
          <p className="text-sm text-muted-foreground">
            {error.message || "An error occurred while loading the project"}
          </p>
        </div>
      </div>
    );
  }

  if (!project) {
    return <Navigate to="/projects" replace />;
  }

  return (
    <>
      <ProjectSidebar />
      <div className="lg:pl-64">
        <Outlet />
      </div>
    </>
  );
}

export default function ProjectLayout() {
  return (
    <ProjectProvider>
      <ProjectLayoutContent />
    </ProjectProvider>
  );
}
