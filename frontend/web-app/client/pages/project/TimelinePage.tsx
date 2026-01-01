import { useProject } from "@/context/ProjectContext";

export default function TimelinePage() {
  const { project } = useProject();

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-3xl font-bold">Timeline</h1>
        <p className="text-muted-foreground mt-1">
          View project timeline for {project?.name}
        </p>
      </div>

      <div className="border rounded-lg p-8 text-center text-muted-foreground">
        <p>Timeline view coming soon...</p>
      </div>
    </div>
  );
}
