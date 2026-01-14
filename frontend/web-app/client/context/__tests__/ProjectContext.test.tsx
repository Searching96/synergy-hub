import { render, screen, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { ProjectProvider, useProject } from "../ProjectContext";
import { projectService } from "@/services/project.service";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";

// Mock projectService
vi.mock("@/services/project.service", () => ({
    projectService: {
        getProjectById: vi.fn(),
    },
}));

const TestComponent = () => {
    const { project, projectKey, isLoading, error } = useProject();
    if (isLoading) return <div>Loading Project...</div>;
    if (error) return <div>Error: {error.message}</div>;
    return (
        <div>
            <div>Project: {project?.name}</div>
            <div>Key: {projectKey}</div>
        </div>
    );
};

const renderProvider = (projectId: string) => {
    const queryClient = new QueryClient({
        defaultOptions: { queries: { retry: false } },
    });

    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[`/projects/${projectId}`]}>
                <Routes>
                    <Route
                        path="/projects/:projectId"
                        element={
                            <ProjectProvider>
                                <TestComponent />
                            </ProjectProvider>
                        }
                    />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe("ProjectContext", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should fetch and display project data", async () => {
        (projectService.getProjectById as any).mockResolvedValue({
            data: { id: 1, name: "Synergy Hub", description: "Test" }
        });

        renderProvider("1");

        expect(await screen.findByText("Loading Project...")).toBeInTheDocument();
        expect(await screen.findByText("Project: Synergy Hub")).toBeInTheDocument();
        expect(await screen.findByText("Key: SYNE")).toBeInTheDocument(); // Key derivation check
    });

    it("should handle error state", async () => {
        (projectService.getProjectById as any).mockRejectedValue(new Error("Failed to fetch"));

        renderProvider("1");

        expect(await screen.findByText("Error: Failed to fetch")).toBeInTheDocument();
    });
});
