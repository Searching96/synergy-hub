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

import DashboardHome from "./pages/dashboard/Home";
import BoardPage from "./pages/board/BoardPage";
import ProjectsPage from "./pages/ProjectsPage";
import SettingsPage from "./pages/settings/SettingsPage";
import ProjectLayout from "./components/layout/ProjectLayout";
import BacklogPage from "./pages/project/BacklogPage";
import TimelinePage from "./pages/project/TimelinePage";
import ProjectSettingsPage from "./pages/project/ProjectSettingsPage";

const queryClient = new QueryClient();

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
                <Route path="/dashboard" element={<DashboardHome />} />
                <Route path="/projects" element={<ProjectsPage />} />
                <Route path="/settings" element={<SettingsPage />} />
                
                {/* Project Nested Routes */}
                <Route path="/projects/:projectId" element={<ProjectLayout />}>
                  <Route index element={<Navigate to="board" replace />} />
                  <Route path="board" element={<BoardPage />} />
                  <Route path="backlog" element={<BacklogPage />} />
                  <Route path="timeline" element={<TimelinePage />} />
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
