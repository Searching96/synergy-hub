import { useParams, Link } from "react-router-dom";
import { useProject } from "@/context/ProjectContext";
import {
  Breadcrumb,
  BreadcrumbList,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbSeparator,
  BreadcrumbPage,
} from "@/components/ui/breadcrumb";

interface ProjectBreadcrumbProps {
  current: string;
}

export function ProjectBreadcrumb({ current }: ProjectBreadcrumbProps) {
  const { project } = useProject();
  const { projectId } = useParams<{ projectId: string }>();

  const projectName = project?.name || "Project";
  const projectHref = projectId ? `/projects/${projectId}/board` : "/projects";

  return (
    <Breadcrumb>
      <BreadcrumbList>
        <BreadcrumbItem>
          <BreadcrumbLink asChild>
            <Link to="/projects">Projects</Link>
          </BreadcrumbLink>
        </BreadcrumbItem>
        <BreadcrumbSeparator />
        <BreadcrumbItem>
          <BreadcrumbLink asChild>
            <Link to={projectHref}>{projectName}</Link>
          </BreadcrumbLink>
        </BreadcrumbItem>
        <BreadcrumbSeparator />
        <BreadcrumbItem>
          <BreadcrumbPage>{current}</BreadcrumbPage>
        </BreadcrumbItem>
      </BreadcrumbList>
    </Breadcrumb>
  );
}
