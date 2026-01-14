import { Toaster } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "@/context/AuthContext";
import PrivateRoute from "@/components/guards/PrivateRoute";
import { OrganizationGuard } from "@/components/guards/OrganizationGuard";
import DashboardLayout from "@/components/layout/DashboardLayout";
import Home from "./pages/Home";
import NotFound from "./pages/NotFound";
import LoginPage from "./pages/auth/LoginPage";
import RegisterPage from "./pages/auth/RegisterPage";
import EmailVerificationPage from "./pages/auth/EmailVerificationPage";
import OAuth2RedirectHandler from "./pages/auth/OAuth2RedirectHandler";
import OrganizationWelcome from "./pages/organization/OrganizationWelcome";
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
import ChatPage from "./pages/project/ChatPage";
import MeetingsListPage from "./pages/project/MeetingsListPage";
import MeetingPage from "./pages/project/MeetingPage";
import ProjectSettingsPage from "./pages/project/ProjectSettingsPage";
import ListPage from "./pages/project/ListPage";
import IssuesPage from "./pages/project/IssuesPage";
import RoleManagerPage from "./pages/settings/RoleManagerPage";
import TeamCreatePage from "./pages/teams/TeamCreatePage";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        if (axios.isAxiosError(error)) {
          const status = error.response?.status || 0;
          if ([400, 401, 403, 404, 422].includes(status)) {
            return false;
          }
        }
        return failureCount < 2;
      },
      staleTime: 30 * 1000,
      refetchOnWindowFocus: false,
      refetchOnReconnect: true,
    },
    mutations: {
      retry: (failureCount, error) => {
        if (axios.isAxiosError(error)) {
          const status = error.response?.status || 0;
          if ([400, 401, 403, 404, 422].includes(status)) {
            return false;
          }
        }
        return failureCount < 1;
      },
    },
  },
});

const App = () => (
  <QueryClientProvider client={queryClient}>
    <BrowserRouter
      future={{
        v7_startTransition: true,
        v7_relativeSplatPath: true,
      }}
    >
      <AuthProvider>
        <TooltipProvider>
          <Toaster />

          <Routes>
            {/* ========== PUBLIC ROUTES - No Auth Required ========== */}
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/verify-email" element={<EmailVerificationPage />} />
            <Route path="/oauth2/redirect" element={<OAuth2RedirectHandler />} />

            {/* ========== AUTHENTICATED BUT NO ORG - Organization Setup ========== */}
            <Route element={<PrivateRoute />}>
              <Route path="/welcome" element={<OrganizationWelcome />} />
            </Route>

            {/* ========== PROTECTED ROUTES - Auth + Organization Required ========== */}
            <Route element={<PrivateRoute />}>
              <Route
                element={
                  <OrganizationGuard>
                    <DashboardLayout />
                  </OrganizationGuard>
                }
              >
                <Route path="/dashboard" element={<YourWork />} />
                <Route path="/projects" element={<ProjectsPage />} />
                <Route path="/teams/create" element={<TeamCreatePage />} />
                <Route path="/settings" element={<SettingsPage />} />
                <Route path="/settings/organization" element={<OrganizationSettingsPage />} />
                <Route path="/settings/roles" element={<RoleManagerPage />} />

                {/* Project Nested Routes with Relative Paths */}
                <Route path="/projects/:projectId" element={<ProjectLayout />}>
                  <Route index element={<Navigate to="activity" replace />} />
                  <Route path="board" element={<BoardPage />} />
                  <Route path="backlog" element={<BacklogPage />} />
                  <Route path="list" element={<ListPage />} />
                  <Route path="issues" element={<IssuesPage />} />
                  <Route path="timeline" element={<TimelinePage />} />
                  <Route path="activity" element={<ActivityPage />} />
                  <Route path="chat" element={<ChatPage />} />
                  <Route path="meetings" element={<MeetingsListPage />} />
                  <Route path="meetings/:meetingId" element={<MeetingPage />} />
                  <Route path="settings" element={<ProjectSettingsPage />} />
                </Route>
              </Route>
            </Route>

            {/* ========== CATCH-ALL 404 ========== */}
            <Route path="*" element={<NotFound />} />
          </Routes>
        </TooltipProvider>
      </AuthProvider>
    </BrowserRouter>
  </QueryClientProvider>
);

export default App;