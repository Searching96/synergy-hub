import { useLocation, Link, useNavigate } from "react-router-dom";
import { useEffect } from "react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/context/AuthContext";

const NotFound = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  useEffect(() => {
    console.error(
      "404 Error: User attempted to access non-existent route:",
      location.pathname,
    );
  }, [location.pathname]);

  // Suggest appropriate links based on authentication state
  const suggestedLinks = isAuthenticated
    ? [
        { label: "Dashboard", path: "/dashboard", icon: "📊" },
        { label: "Projects", path: "/projects", icon: "📁" },
        { label: "Settings", path: "/settings", icon: "⚙️" },
      ]
    : [
        { label: "Login", path: "/login", icon: "🔐" },
        { label: "Register", path: "/register", icon: "📝" },
      ];

  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4">
      <div className="text-center max-w-md">
        <div className="text-6xl font-bold text-muted-foreground mb-4">404</div>
        <h1 className="text-3xl font-bold mb-2">Page Not Found</h1>
        <p className="text-muted-foreground mb-2">
          The page you're looking for doesn't exist or may have been moved.
        </p>
        <p className="text-sm text-muted-foreground mb-6">
          Requested path: <code className="bg-muted px-2 py-1 rounded text-xs">{location.pathname}</code>
        </p>

        <div className="flex flex-col gap-3 mb-6">
          {suggestedLinks.map((link) => (
            <Button
              key={link.path}
              variant="outline"
              className="w-full"
              onClick={() => navigate(link.path)}
            >
              {link.icon} {link.label}
            </Button>
          ))}
        </div>

        <Button
          variant="ghost"
          onClick={() => navigate(-1)}
          className="w-full"
        >
          ← Go Back
        </Button>
      </div>
    </div>
  );
};

export default NotFound;
