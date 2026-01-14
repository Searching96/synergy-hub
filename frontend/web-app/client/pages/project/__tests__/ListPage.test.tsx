import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import ListPage from "../ListPage";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { taskService } from "@/services/task.service";

// Mock Hooks & Services
vi.mock("@/context/ProjectContext", () => ({
    useProject: () => ({
        project: { id: 1, name: "Test Project" },
    }),
}));

vi.mock("@/services/task.service", () => ({
    taskService: {
        getProjectTasks: vi.fn(),
    },
}));

const renderList = () => {
    const queryClient = new QueryClient({
        defaultOptions: { queries: { retry: false } }
    });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={["/projects/123/list"]}>
                <Routes>
                    <Route path="/projects/:projectId/list" element={<ListPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe("ListPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should render list of tasks", async () => {
        vi.mocked(taskService.getProjectTasks).mockResolvedValue({
            data: [
                { id: 101, title: "Task 1", status: "TO_DO", type: "STORY" },
                { id: 102, title: "Task 2", status: "DONE", type: "TASK" }
            ]
        } as any);

        renderList();

        expect(await screen.findByText("Task 1")).toBeInTheDocument();
        expect(await screen.findByText("Task 2")).toBeInTheDocument();
        expect(await screen.findByText("101")).toBeInTheDocument();
    });

    it("should filter tasks by search input", async () => {
        vi.mocked(taskService.getProjectTasks).mockResolvedValue({
            data: [
                { id: 101, title: "Apple", status: "TO_DO", type: "STORY" },
                { id: 102, title: "Banana", status: "DONE", type: "TASK" }
            ]
        } as any);

        renderList();

        expect(await screen.findByText("Apple")).toBeInTheDocument();

        const searchInput = screen.getByPlaceholderText(/Search issues/i);
        fireEvent.change(searchInput, { target: { value: "Apple" } });

        expect(screen.getByText("Apple")).toBeInTheDocument();
        expect(screen.queryByText("Banana")).not.toBeInTheDocument();
    });

    it("should show empty state when no tasks", async () => {
        vi.mocked(taskService.getProjectTasks).mockResolvedValue({ data: [] } as any);

        renderList();
        expect(await screen.findByText(/No issues found/i)).toBeInTheDocument();
    });
});
