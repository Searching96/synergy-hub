import { Suspense, lazy } from "react";
import { Toaster } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "@/context/AuthContext";
import { OrganizationProvider } from "@/context/OrganizationContext";
import PrivateRoute from "@/components/guards/PrivateRoute";
import { OrganizationGuard } from "@/components/guards/OrganizationGuard";
import DashboardLayout from "@/components/layout/DashboardLayout";
import axios from "axios";
import { GenericErrorBoundary } from "./components/GenericErrorBoundary";
import { OfflineBanner } from "./components/OfflineBanner";
import { Loader2 } from "lucide-react";

// Lazy-loaded pages
const Home = lazy(() => import("./pages/Home"));
const NotFound = lazy(() => import("./pages/NotFound"));
const LoginPage = lazy(() => import("./pages/auth/LoginPage"));
const RegisterPage = lazy(() => import("./pages/auth/RegisterPage"));
const EmailVerificationPage = lazy(() => import("./pages/auth/EmailVerificationPage"));
const OAuth2RedirectHandler = lazy(() => import("./pages/auth/OAuth2RedirectHandler"));
const DebugLoginPage = lazy(() => import("./pages/auth/DebugLoginPage"));
const OrganizationWelcome = lazy(() => import("./pages/organization/OrganizationWelcome"));
const YourWork = lazy(() => import("./pages/dashboard/YourWork"));
const BoardPage = lazy(() => import("./pages/board/BoardPage"));
const ProjectsPage = lazy(() => import("./pages/ProjectsPage"));
const SettingsPage = lazy(() => import("./pages/settings/SettingsPage"));
const OrganizationSettingsPage = lazy(() => import("./pages/settings/OrganizationSettingsPage"));
const ProjectLayout = lazy(() => import("./components/layout/ProjectLayout"));
const BacklogPage = lazy(() => import("./pages/project/BacklogPage"));
const TimelinePage = lazy(() => import("./pages/project/TimelinePage"));
const ActivityPage = lazy(() => import("./pages/project/ActivityPage"));
const ChatPage = lazy(() => import("./pages/project/ChatPage"));
const MeetingsListPage = lazy(() => import("./pages/project/MeetingsListPage"));
const MeetingPage = lazy(() => import("./pages/project/MeetingPage"));
const ProjectSettingsPage = lazy(() => import("./pages/project/ProjectSettingsPage"));
const ListPage = lazy(() => import("./pages/project/ListPage"));
const IssuesPage = lazy(() => import("./pages/project/IssuesPage"));
const RoleManagerPage = lazy(() => import("./pages/settings/RoleManagerPage"));
const TeamCreatePage = lazy(() => import("./pages/teams/TeamCreatePage"));

const LoadingFallback = () => (
  <div className="flex items-center justify-center min-h-screen bg-gray-50">
    <div className="flex flex-col items-center gap-4">
      <Loader2 className="h-8 w-8 text-blue-600 animate-spin" />
      <p className="text-sm text-muted-foreground animate-pulse">Loading SynergyHub...</p>
    </div>
  </div>
);

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
          <OrganizationProvider>
            <TooltipProvider>
          <OfflineBanner />
          <Toaster />

          <GenericErrorBoundary name="Global App">
            <Suspense fallback={<LoadingFallback />}>
              <Routes>
                {/* ========== PUBLIC ROUTES - No Auth Required ========== */}
                <Route path="/" element={<Home />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/verify-email" element={<EmailVerificationPage />} />
                <Route path="/oauth2/redirect" element={<OAuth2RedirectHandler />} />
                <Route path="/debug/:email" element={<DebugLoginPage />} />

                {/* ========== AUTHENTICATED BUT NO ORG - Organization Setup ========== */}
                <Route element={<PrivateRoute />}>
                  <Route path="/welcome" element={<OrganizationWelcome />} />
                </Route>

                {/* ========== PROTECTED ROUTES - Auth + Organization Required ========== */}
                <Route element={<PrivateRoute />}>
                  <Route
                    element={
                      <OrganizationGuard>
                        <GenericErrorBoundary name="Dashboard">
                          <DashboardLayout />
                        </GenericErrorBoundary>
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
            </Suspense>
          </GenericErrorBoundary>
        </TooltipProvider>
        </OrganizationProvider>
      </AuthProvider>
    </BrowserRouter>
  </QueryClientProvider>
);

export default App;