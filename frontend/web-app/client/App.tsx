import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "@/context/AuthContext";
import PrivateRoute from "@/components/PrivateRoute";
import DashboardLayout from "@/components/layout/DashboardLayout";
import Home from "./pages/Home";
import NotFound from "./pages/NotFound";
import LoginPage from "./pages/auth/LoginPage";
import RegisterPage from "./pages/auth/RegisterPage";
import axios from "axios";

import YourWork from "./pages/dashboard/YourWork";
import BoardPage from "./pages/board/BoardPage";
import ProjectsPage from "./pages/ProjectsPage";
import SettingsPage from "./pages/settings/SettingsPage";
import OrganizationSettingsPage from "./pages/settings/OrganizationSettingsPage";
import ProjectLayout from "./components/layout/ProjectLayout";
import BacklogPage from "./pages/project/BacklogPage";
import TimelinePage from "./pages/project/TimelinePage";
import ActivityPage from "./pages/project/ActivityPage";
import ProjectSettingsPage from "./pages/project/ProjectSettingsPage";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        // Don't retry on client errors (401, 403, 404)
        if (axios.isAxiosError(error)) {
          const status = error.response?.status || 0;
          if ([401, 403, 404].includes(status)) {
            return false;
          }
        }
        // Retry server errors max 2 times
        return failureCount < 2;
      },
      staleTime: 30 * 1000, // 30 seconds default
      refetchOnWindowFocus: false, // Prevent aggressive refetching on window focus
      refetchOnReconnect: true, // Refetch on network reconnection
    },
    mutations: {
      retry: false, // Don't retry mutations by default
    },
  },
});

const App = () => (
  <QueryClientProvider client={queryClient}>
    <AuthProvider>
      <TooltipProvider>
        <Toaster />
        <Sonner />
        <BrowserRouter
          future={{
            v7_startTransition: true,
            v7_relativeSplatPath: true,
          }}
        >
          <Routes>
            {/* Public Routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            
            {/* Protected Routes with Dashboard Layout */}
            <Route element={<PrivateRoute />}>
              <Route element={<DashboardLayout />}>
                <Route path="/dashboard" element={<YourWork />} />
                <Route path="/projects" element={<ProjectsPage />} />
                <Route path="/settings" element={<SettingsPage />} />
                <Route path="/settings/organization" element={<OrganizationSettingsPage />} />
                
                {/* Project Nested Routes */}
                <Route path="/projects/:projectId" element={<ProjectLayout />}>
                  <Route index element={<Navigate to="board" replace />} />
                  <Route path="board" element={<BoardPage />} />
                  <Route path="backlog" element={<BacklogPage />} />
                  <Route path="timeline" element={<TimelinePage />} />
                  <Route path="activity" element={<ActivityPage />} />
                  <Route path="settings" element={<ProjectSettingsPage />} />
                </Route>
              </Route>
            </Route>
            
            {/* ADD ALL CUSTOM ROUTES ABOVE THE CATCH-ALL "*" ROUTE */}
            <Route path="*" element={<NotFound />} />
          </Routes>
        </BrowserRouter>
      </TooltipProvider>
    </AuthProvider>
  </QueryClientProvider>
);

export default App;
