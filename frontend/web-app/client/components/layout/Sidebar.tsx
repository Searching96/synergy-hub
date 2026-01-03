import { NavLink } from "react-router-dom";
import { cn } from "@/lib/utils";
import { Layout, Briefcase, Settings, Building2, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/context/AuthContext";

interface SidebarProps {
  isOpen?: boolean;
  onClose?: () => void;
}

export default function Sidebar({ isOpen = true, onClose }: SidebarProps) {
  const { user } = useAuth();
  
  const navItems = [
    { to: "/dashboard", label: "Your Work", icon: Layout },
    { to: "/projects", label: "Projects", icon: Briefcase },
    { to: "/settings", label: "Settings", icon: Settings },
  ];
  
  // Add Organization Settings for admins
  const isAdmin = user?.roles?.includes("GLOBAL_ADMIN") || user?.roles?.includes("ORG_ADMIN");
  if (isAdmin) {
    navItems.push({
      to: "/settings/organization",
      label: "Organization",
      icon: Building2,
    });
  }

  return (
    <>
      {/* Mobile Overlay */}
      {isOpen && (
        <div 
          className="fixed inset-0 bg-black/50 z-40 lg:hidden"
          onClick={onClose}
        />
      )}

      {/* Sidebar */}
      <aside
        className={cn(
          "fixed left-0 top-0 z-50 h-screen w-64 bg-background border-r transition-transform duration-300 lg:translate-x-0 lg:z-30",
          isOpen ? "translate-x-0" : "-translate-x-full"
        )}
      >
        <div className="flex flex-col h-full">
          {/* Header */}
          <div className="flex items-center justify-between h-14 px-6 border-b">
            <h2 className="text-lg font-bold text-blue-600">SynergyHub</h2>
            <Button
              variant="ghost"
              size="icon"
              className="lg:hidden"
              onClick={onClose}
            >
              <X className="h-5 w-5" />
            </Button>
          </div>

          {/* Navigation */}
          <nav className="flex-1 px-3 py-4 space-y-1">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={onClose}
                className={({ isActive }) =>
                  cn(
                    "flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors",
                    isActive
                      ? "bg-blue-600 text-white"
                      : "text-muted-foreground hover:bg-muted hover:text-foreground"
                  )
                }
              >
                <item.icon className="h-5 w-5" />
                {item.label}
              </NavLink>
            ))}
          </nav>

          {/* Footer */}
          <div className="p-4 border-t text-xs text-muted-foreground">
            © 2026 SynergyHub
          </div>
        </div>
      </aside>
    </>
  );
}
