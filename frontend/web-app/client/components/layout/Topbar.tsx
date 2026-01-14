import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Plus, LogOut, User, ChevronDown, Settings, UserPlus, Users, Search } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { useNavigate, useSearchParams, NavLink, useLocation } from "react-router-dom";
import { cn } from "@/lib/utils";
import { useQuery } from "@tanstack/react-query";
import { projectService } from "@/services/project.service";

export default function Topbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();

  const { data: projectsResponse } = useQuery({
    queryKey: ["projects"],
    queryFn: () => projectService.getProjects(),
  });

  const projects = projectsResponse?.data?.filter(p => p.status !== "ARCHIVED") || [];
  const isAdmin = user?.roles?.includes("GLOBAL_ADMIN") || user?.roles?.includes("ORG_ADMIN");

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const getInitials = (name: string) => {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2);
  };

  const handleCreateIssue = () => {
    const next = new URLSearchParams(searchParams);
    next.set("create", "true");
    next.delete("issue");
    setSearchParams(next);
  };

  const isActiveRoute = (path: string) => {
    return location.pathname === path || location.pathname.startsWith(path + "/");
  };

  return (
    <header className="fixed top-0 left-0 right-0 z-20 h-14 bg-background border-b">
      <div className="h-full px-4 flex items-center justify-between">
        {/* Left: Logo + Navigation */}
        <div className="flex items-center gap-6">
          <h1 className="text-lg font-bold text-blue-600 cursor-pointer" onClick={() => navigate("/dashboard")}>
            SynergyHub
          </h1>

          <nav className="hidden md:flex items-center gap-1">
            {/* Your Work */}
            <NavLink
              to="/dashboard"
              className={({ isActive }) =>
                cn(
                  "px-3 py-1.5 text-sm font-medium rounded hover:bg-accent transition-colors",
                  isActive ? "text-foreground" : "text-muted-foreground"
                )
              }
            >
              Your work
            </NavLink>

            {/* Projects Dropdown */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  className={cn(
                    "px-3 h-auto py-1.5 text-sm font-medium",
                    isActiveRoute("/projects") ? "text-foreground" : "text-muted-foreground"
                  )}
                >
                  Projects
                  <ChevronDown className="ml-1 h-3 w-3" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="start" className="w-56">
                <DropdownMenuItem onClick={() => navigate("/projects")}>
                  View all projects
                </DropdownMenuItem>
                {projects.length > 0 && (
                  <>
                    <DropdownMenuSeparator />
                    <DropdownMenuLabel>Recent Projects</DropdownMenuLabel>
                    {projects.slice(0, 5).map((project) => (
                      <DropdownMenuItem
                        key={project.id}
                        onClick={() => navigate(`/projects/${project.id}/board`)}
                      >
                        {project.name}
                      </DropdownMenuItem>
                    ))}
                  </>
                )}
              </DropdownMenuContent>
            </DropdownMenu>

            {/* Team Dropdown */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  className={cn(
                    "px-3 h-auto py-1.5 text-sm font-medium",
                    isActiveRoute("/teams") ? "text-foreground" : "text-muted-foreground"
                  )}
                >
                  Team
                  <ChevronDown className="ml-1 h-3 w-3" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="start" className="w-64">
                <DropdownMenuItem onClick={() => navigate("/teams/create")}>
                  <Users className="mr-2 h-4 w-4" />
                  Create a team
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </nav>
        </div>

        {/* Right: Actions */}
        <div className="flex items-center gap-2">
          <Button
            className="bg-blue-600 hover:bg-blue-700 hidden sm:flex"
            onClick={handleCreateIssue}
          >
            <Plus className="h-4 w-4 mr-2" />
            Create
          </Button>

          <Button
            size="icon"
            className="bg-blue-600 hover:bg-blue-700 sm:hidden"
            onClick={handleCreateIssue}
          >
            <Plus className="h-4 w-4" />
          </Button>

          {/* Settings Dropdown */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon">
                <Settings className="h-5 w-5" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => navigate("/settings")}>
                Profile Settings
              </DropdownMenuItem>
              {isAdmin && (
                <DropdownMenuItem onClick={() => navigate("/settings/organization")}>
                  Organization
                </DropdownMenuItem>
              )}
            </DropdownMenuContent>
          </DropdownMenu>

          {/* User Dropdown */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="rounded-full">
                <Avatar className="h-8 w-8">
                  <AvatarFallback className="bg-blue-600 text-white text-xs">
                    {user?.name ? getInitials(user.name) : "U"}
                  </AvatarFallback>
                </Avatar>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel>
                <div className="flex flex-col space-y-1">
                  <p className="text-sm font-medium">{user?.name || "User"}</p>
                  <p className="text-xs text-muted-foreground">{user?.email}</p>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={handleLogout}>
                <LogOut className="mr-2 h-4 w-4" />
                Logout
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </header>
  );
}