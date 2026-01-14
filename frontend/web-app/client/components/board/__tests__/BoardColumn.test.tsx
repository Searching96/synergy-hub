import { render, screen } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import BoardColumn from "../BoardColumn";

// Mock Hello Pangea DnD
vi.mock("@hello-pangea/dnd", () => ({
    Droppable: ({ children }: any) => children({
        draggableProps: {},
        innerRef: null,
        placeholder: <div data-testid="placeholder" />
    }, {
        isDraggingOver: false
    }),
}));

// Mock IssueCard
vi.mock("../IssueCard", () => ({
    default: ({ task }: any) => <div data-testid="issue-card">{task.title}</div>
}));

// Mock Skeleton
vi.mock("@/components/ui/skeleton", () => ({
    Skeleton: () => <div data-testid="skeleton" />
}));

describe("BoardColumn", () => {
    const mockTasks = [
        { id: 1, title: "Task 1", status: "TO_DO" },
        { id: 2, title: "Task 2", status: "TO_DO" }
    ];

    it("renders column title and task count", () => {
        render(
            <BoardColumn
                status="TO_DO"
                title="To Do"
                tasks={mockTasks as any}
                projectKey="TEST"
            />
        );

        expect(screen.getByText("To Do")).toBeInTheDocument();
        expect(screen.getByText("2")).toBeInTheDocument();
    });

    it("renders tasks using IssueCard", () => {
        render(
            <BoardColumn
                status="TO_DO"
                title="To Do"
                tasks={mockTasks as any}
                projectKey="TEST"
            />
        );

        expect(screen.getAllByTestId("issue-card")).toHaveLength(2);
        expect(screen.getByText("Task 1")).toBeInTheDocument();
    });

    it("renders empty state", () => {
        render(
            <BoardColumn
                status="TO_DO"
                title="To Do"
                tasks={[]}
                projectKey="TEST"
            />
        );

        expect(screen.getByText(/No tasks/i)).toBeInTheDocument();
    });

    it("renders loading state (skeletons when empty)", () => {
        render(
            <BoardColumn
                status="TO_DO"
                title="To Do"
                tasks={[]}
                projectKey="TEST"
                isLoading={true}
            />
        );

        expect(screen.getAllByTestId("skeleton")).toHaveLength(3);
    });

    it("renders loading state (overlay when has tasks)", () => {
        const { container } = render(
            <BoardColumn
                status="TO_DO"
                title="To Do"
                tasks={mockTasks as any}
                projectKey="TEST"
                isLoading={true}
            />
        );

        // Check for the pulse overlay
        // The component renders standard IssueCards AND an overlay div
        expect(screen.getAllByTestId("issue-card")).toHaveLength(2);
        // Look for animate-pulse class?
        // Querying purely by class is brittle but we can check if the overlay div exists.
        // It has `animate-pulse` class.
        // Or checking `container.querySelectorAll('.animate-pulse')`.
        const pulsingOverlays = container.querySelectorAll('.animate-pulse');
        expect(pulsingOverlays.length).toBeGreaterThan(0);
    });

    it("shows restricted move badge when canMove is false", () => {
        render(
            <BoardColumn
                status="TO_DO"
                title="To Do"
                tasks={mockTasks as any}
                projectKey="TEST"
                canMove={false}
            />
        );

        expect(screen.getByText(/Move restricted/i)).toBeInTheDocument();
    });
});
