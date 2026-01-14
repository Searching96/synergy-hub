import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import TimelinePage from "../TimelinePage";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { taskService } from "@/services/task.service";
import { sprintService } from "@/services/sprint.service";

// Mock Hooks & Services
vi.mock("@/context/ProjectContext", () => ({
    useProject: () => ({
        project: { id: 1, name: "Test Project" },
    }),
}));

vi.mock("@/services/task.service", () => ({
    taskService: {
        getProjectEpics: vi.fn(),
        getProjectTasks: vi.fn(),
    },
}));

vi.mock("@/services/sprint.service", () => ({
    sprintService: {
        getProjectSprints: vi.fn(),
    },
}));

const renderTimeline = () => {
    const queryClient = new QueryClient({
        defaultOptions: { queries: { retry: false } }
    });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={["/projects/123/timeline"]}>
                <Routes>
                    <Route path="/projects/:projectId/timeline" element={<TimelinePage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe("TimelinePage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should render loading state", async () => {
        vi.mocked(taskService.getProjectEpics).mockReturnValue(new Promise(() => { })); // Never resolves
        vi.mocked(taskService.getProjectTasks).mockReturnValue(new Promise(() => { }));
        renderTimeline();
        // Wait for the loading spinner
        expect(await screen.findByTestId("timeline-loading")).toBeInTheDocument();
    });

    it("should render timeline with epics", async () => {
        vi.mocked(taskService.getProjectEpics).mockResolvedValue({ data: [{ id: 1, title: "Epic 1", createdAt: new Date().toISOString() }] } as any);
        vi.mocked(taskService.getProjectTasks).mockResolvedValue({ data: [] } as any);
        vi.mocked(sprintService.getProjectSprints).mockResolvedValue({ data: [] } as any);

        renderTimeline();

        // There are two Timeline elements (breadcrumb and header)
        const timelineHeadings = await screen.findAllByText("Timeline");
        expect(timelineHeadings.length).toBeGreaterThanOrEqual(2);

        // Expand the epic row to reveal the title
        const epicRows = await screen.findAllByText("Epic 1");
        const epicRow = epicRows[0]; // The list item
        fireEvent.click(epicRow);
        expect(epicRow).toBeInTheDocument();
    });

    it("should filter epics by search query", async () => {
        vi.mocked(taskService.getProjectEpics).mockResolvedValue({
            data: [
                { id: 1, title: "Apple", createdAt: new Date().toISOString() },
                { id: 2, title: "Banana", createdAt: new Date().toISOString() }
            ]
        } as any);
        vi.mocked(taskService.getProjectTasks).mockResolvedValue({ data: [] } as any);
        vi.mocked(sprintService.getProjectSprints).mockResolvedValue({ data: [] } as any);

        renderTimeline();

        // Expand both epic rows to reveal the titles
        const appleEpics = await screen.findAllByText("Apple");
        fireEvent.click(appleEpics[0]);
        const bananaEpics = await screen.findAllByText("Banana");
        fireEvent.click(bananaEpics[0]);

        // Wait for the search input to appear
        const searchInput = await screen.findByPlaceholderText("Search timeline");
        fireEvent.change(searchInput, { target: { value: "Apple" } });

        expect(screen.getAllByText("Apple")[0]).toBeInTheDocument();
        expect(screen.queryByText("Banana")).not.toBeInTheDocument();
    });
});
