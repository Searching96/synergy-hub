import { Link, useLocation, useParams } from "react-router-dom";
import { cn } from "@/lib/utils";
import {
  LayoutDashboard,
  ListTodo,
  Calendar,
  Activity,
  MessageSquare,
  Video,
  Settings,
  ChevronLeft,
  List,
  ListChecks,
  Plus
} from "lucide-react";
import { useProject } from "@/context/ProjectContext";

const planningNavItems = [
  { title: "Timeline", href: "/timeline", icon: Calendar },
  { title: "Backlog", href: "/backlog", icon: ListTodo },
  { title: "Board", href: "/board", icon: LayoutDashboard },
  { title: "List", href: "/list", icon: List },
  { title: "Issues All", href: "/issues", icon: ListChecks },
];

const otherNavItems = [
  { title: "Activity", href: "/activity", icon: Activity },
  { title: "Chat", href: "/chat", icon: MessageSquare },
  { title: "Meetings", href: "/meetings", icon: Video },
  { title: "Settings", href: "/settings", icon: Settings },
];

export default function ProjectSidebar() {
  const location = useLocation();
  const { projectId } = useParams<{ projectId: string }>();
  const { project, isLoading } = useProject();

  return (
    <aside className="fixed left-0 top-14 z-30 hidden h-[calc(100vh-3.5rem)] w-64 border-r bg-background lg:block">
      <div className="flex h-full flex-col">
        {/* Project Header */}
        <div className="p-4">

          {isLoading ? (
            <div className="h-6 w-3/4 bg-muted animate-pulse rounded" />
          ) : (
            <h2 className="text-lg font-semibold truncate">{project?.name || "Project"}</h2>
          )}

          {project?.description && (
            <p className="text-sm text-muted-foreground mt-1 line-clamp-2">
              {project.description}
            </p>
          )}
        </div>

        {/* Navigation Links */}
        <nav className="flex-1 space-y-6 p-3 overflow-y-auto">
          {/* Planning Group */}
          <div>
            <div className="px-3 pb-2 text-[10px] font-semibold tracking-wider text-muted-foreground">PLANNING</div>
            <div className="space-y-1">
              {planningNavItems.map((item) => {
                const href = `/projects/${projectId}${item.href}`;
                const isActive = location.pathname === href;
                const Icon = item.icon as any;
                return (
                  <Link
                    key={item.href}
                    to={href}
                    className={cn(
                      "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                      isActive
                        ? "bg-blue-50 text-blue-600 dark:bg-blue-900/20"
                        : "text-muted-foreground hover:bg-accent hover:text-foreground"
                    )}
                  >
                    <Icon className="h-4 w-4" />
                    {item.title}
                  </Link>
                );
              })}

            </div>
          </div>

          {/* Other Links */}
          <div className="space-y-1">
            {otherNavItems.map((item) => {
              const href = `/projects/${projectId}${item.href}`;
              const isActive = location.pathname === href;
              const Icon = item.icon as any;
              const isDisabled = project?.status === "ARCHIVED" && item.href === "/settings";

              return (
                <Link
                  key={item.href}
                  to={isDisabled ? "#" : href}
                  onClick={(e) => isDisabled && e.preventDefault()}
                  className={cn(
                    "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                    isActive
                      ? "bg-blue-50 text-blue-600 dark:bg-blue-900/20"
                      : "text-muted-foreground hover:bg-accent hover:text-foreground",
                    isDisabled && "opacity-50 cursor-not-allowed hover:bg-transparent hover:text-muted-foreground"
                  )}
                >
                  <Icon className="h-4 w-4" />
                  {item.title}
                </Link>
              );
            })}
          </div>
        </nav>

        {/* Project Stats */}
        {project?.stats && (
          <div className="border-t p-4 space-y-2">
            <div className="text-xs font-medium text-muted-foreground">
              Project Stats
            </div>
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div>
                <div className="text-muted-foreground">Total Tasks</div>
                <div className="font-semibold">{project.stats.totalTasks}</div>
              </div>
              <div>
                <div className="text-muted-foreground">Completed</div>
                <div className="font-semibold">{project.stats.completedTasks}</div>
              </div>
              <div>
                <div className="text-muted-foreground">In Progress</div>
                <div className="font-semibold">{project.stats.inProgressTasks}</div>
              </div>
              <div>
                <div className="text-muted-foreground">Active Sprints</div>
                <div className="font-semibold">{project.stats.activeSprints}</div>
              </div>
            </div>
          </div>
        )}
      </div>
    </aside>
  );
}
