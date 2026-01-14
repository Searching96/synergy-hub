import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";

// Mock ResizeObserver
global.ResizeObserver = class ResizeObserver {
    observe() { }
    unobserve() { }
    disconnect() { }
};

// Mock Select
vi.mock("@/components/ui/select", () => ({
    Select: ({ children, onValueChange, value }: any) => (
        <select onChange={e => onValueChange(e.target.value)} value={value}>
            {children}
        </select>
    ),
    SelectTrigger: ({ children }: any) => <div>{children}</div>,
    SelectValue: () => <span>Select Value</span>,
    SelectContent: ({ children }: any) => <div>{children}</div>,
    SelectItem: ({ children, value }: any) => <option value={value}>{children}</option>,
}));

// Mock DragDropContext
vi.mock("@hello-pangea/dnd", () => ({
    DragDropContext: ({ children }: any) => <div>{children}</div>,
    Droppable: ({ children }: any) => children({ draggableProps: {}, innerRef: null }, {}),
    Draggable: ({ children }: any) => children({ draggableProps: {}, dragHandleProps: {}, innerRef: null }, {}),
}));
import BoardView from "../BoardView";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useProjectBoard } from "@/hooks/useProjectBoard";
import { Mock } from "vitest";

// Mock Child Components
vi.mock("@/components/board/BoardColumn", () => ({
    default: ({ title, tasks }: any) => (
        <div data-testid="board-column">
            <h2>{title}</h2>
            {tasks.map((t: any) => <div key={t.id}>{t.title}</div>)}
        </div>
    ),
}));

vi.mock("@/components/sprint/CreateSprintDialog", () => ({
    default: () => <div data-testid="create-sprint-dialog" />,
}));

vi.mock("@/components/sprint/SprintListDialog", () => ({
    default: () => <div data-testid="sprint-list-dialog" />,
}));

vi.mock("@/components/issue/IssueDetailModal", () => ({
    default: () => <div data-testid="issue-detail-modal" />,
}));

vi.mock("@/components/project/ProjectBreadcrumb", () => ({
    ProjectBreadcrumb: () => <div data-testid="project-breadcrumb" />,
}));

vi.mock("@/components/EmptyState", () => ({
    EmptyState: ({ title }: any) => <div>{title}</div>,
}));

// Mock Hooks
vi.mock("@/context/ProjectContext", () => ({
    useProject: () => ({
        project: { id: 1, name: "Test Project", members: [] },
        projectKey: "TEST",
    }),
}));

vi.mock("@/context/AuthContext", () => ({
    useAuth: () => ({
        user: { id: 1, name: "Test User", roles: ["OWNER"] },
    }),
}));

vi.mock("@/hooks/usePermissionError", () => ({
    usePermissionError: () => ({
        error: null,
        clearError: vi.fn(),
    }),
}));

vi.mock("react-router-dom", async () => {
    const actual = await vi.importActual("react-router-dom");
    return {
        ...actual,
        useParams: () => ({ projectId: "1" }),
        useSearchParams: () => [new URLSearchParams(), vi.fn()],
    };
});

vi.mock("@/hooks/useProjectBoard", () => ({
    useProjectBoard: vi.fn(),
    groupTasksByStatus: (tasks: any[]) => {
        const groups: any = { TO_DO: [], IN_PROGRESS: [], DONE: [] };
        tasks.forEach(t => {
            if (groups[t.status]) groups[t.status].push(t);
        });
        return groups;
    },
    COLUMN_LABELS: { TO_DO: "To Do", IN_PROGRESS: "In Progress", DONE: "Done" },
    COLUMN_ORDER: ["TO_DO", "IN_PROGRESS", "DONE"],
}));

const renderBoard = () => {
    const queryClient = new QueryClient({
        defaultOptions: { queries: { retry: false } }
    });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <BoardView />
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe("BoardView", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should render loading state", () => {
        (useProjectBoard as Mock).mockReturnValue({ isLoading: true });

        renderBoard();
        // Loader2 has animate-spin class or similar, we check for visibility
        expect(document.querySelector(".animate-spin")).toBeInTheDocument();
    });

    it("should render empty state when no active sprint", async () => {
        (useProjectBoard as Mock).mockReturnValue({
            activeSprint: null,
            isLoading: false,
            backlogTasks: []
        });

        renderBoard();
        expect(screen.getByText(/No Active Sprint/i)).toBeInTheDocument();
        expect(screen.getByText(/Create Sprint/i)).toBeInTheDocument();
    });

    it("should render active sprint and tasks", async () => {
        (useProjectBoard as Mock).mockReturnValue({
            activeSprint: {
                id: 1,
                name: "Test Sprint",
                tasks: [
                    { id: 101, title: "Task 1", status: "TO_DO", archived: false },
                    { id: 102, title: "Task 2", status: "IN_PROGRESS", archived: false },
                ],
                metrics: { completionPercentage: 50 },
            },
            isLoading: false,
        });

        renderBoard();
        expect(screen.getByText(/Test Sprint/)).toBeInTheDocument();
        expect(screen.getByText("Task 1")).toBeInTheDocument();
        expect(screen.getByText("Task 2")).toBeInTheDocument();
    });

    it("should filter tasks by search query", async () => {
        (useProjectBoard as Mock).mockReturnValue({
            activeSprint: {
                id: 1,
                name: "Test Sprint",
                tasks: [
                    { id: 101, title: "Apple", status: "TO_DO", archived: false },
                    { id: 102, title: "Banana", status: "TO_DO", archived: false },
                ],
            },
            isLoading: false,
        });

        renderBoard();

        const searchInput = screen.getByPlaceholderText(/Filter tasks/i);
        fireEvent.change(searchInput, { target: { value: "Apple" } });

        // Wait for debounce (300ms)
        await waitFor(() => {
            expect(screen.getByText("Apple")).toBeInTheDocument();
            expect(screen.queryByText("Banana")).not.toBeInTheDocument();
        }, { timeout: 1000 });
    });
});
