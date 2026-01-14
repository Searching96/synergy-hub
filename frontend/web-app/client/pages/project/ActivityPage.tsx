import { useParams } from "react-router-dom";
import { useProject } from "@/context/ProjectContext";
import ActivityStream from "@/components/activity/ActivityStream";
import { AlertCircle } from "lucide-react";
import { ProjectBreadcrumb } from "@/components/project/ProjectBreadcrumb";

export default function ActivityPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { project } = useProject();

  if (!projectId) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-center">
          <AlertCircle className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
          <p className="text-muted-foreground">Project not found</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="mb-4">
        <ProjectBreadcrumb current="Activity" />
      </div>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold mb-1">Activity Stream</h1>
          <p className="text-muted-foreground">
            View all recent activity and changes in {project?.name || "this project"}
          </p>
        </div>

        <ActivityStream projectId={parseInt(projectId)} showTitle={false} height="calc(100vh - 250px)" />
      </div>
    </div>
  );
}
