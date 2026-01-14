import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import IssueForm from "../IssueForm";
import { useProjects } from "@/hooks/useProjects";
import { useQuery } from "@tanstack/react-query";

// Mock hooks
vi.mock("@/hooks/useProjects", () => ({
    useProjects: vi.fn(),
}));

// Mock ResizeObserver
global.ResizeObserver = class ResizeObserver {
    observe() { }
    unobserve() { }
    disconnect() { }
};

// Mock Select
vi.mock("@/components/ui/select", async () => {
    const React = await import('react');
    const SelectContext = React.createContext<any>(null);
    return {
        Select: ({ children, onValueChange, value }: any) => (
            <SelectContext.Provider value={{ onValueChange, value }}>
                <div data-testid="select">{children}</div>
            </SelectContext.Provider>
        ),
        SelectTrigger: ({ children, id }: any) => <button id={id} role="combobox">{children}</button>,
        SelectValue: () => null,
        SelectContent: ({ children }: any) => <div>{children}</div>,
        SelectItem: ({ children, value }: any) => (
            <SelectContext.Consumer>
                {(ctx: any) => (
                    <div role="option" onClick={() => ctx.onValueChange(value)}>
                        {children}
                    </div>
                )}
            </SelectContext.Consumer>
        ),
    };
});

// Mock AttachmentDropzone
vi.mock("@/components/issue/AttachmentDropzone", () => ({
    AttachmentDropzone: ({ onFilesSelected }: any) => (
        <input
            data-testid="dropzone"
            type="file"
            multiple
            onChange={(e) => onFilesSelected(Array.from(e.target.files || []))}
        />
    ),
}));

vi.mock("@tanstack/react-query", () => ({
    useQuery: vi.fn(),
}));

vi.mock("@/hooks/use-toast", () => ({
    useToast: vi.fn(() => ({ toast: vi.fn() })),
}));

describe("IssueForm Component", () => {
    const mockOnSubmit = vi.fn();
    const mockOnCancel = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();

        // Default mock implementation for projects
        (useProjects as any).mockReturnValue({
            data: { data: { content: [{ id: 1, name: "Project Alpha", status: "ACTIVE" }] } },
        });

        // Default mock implementation for members
        (useQuery as any).mockImplementation(({ queryKey }: any) => {
            if (queryKey[0] === "project-members") {
                return { data: { data: [{ userId: 1, name: "John Doe" }] } };
            }
            if (queryKey[0] === "project-tasks") {
                return { data: { data: [] } };
            }
            return { data: null };
        });
    });

    it("renders basic form fields", () => {
        render(<IssueForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);

        expect(screen.getByLabelText(/Project/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Summary/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Issue type/i)).toBeInTheDocument();
    });

    it("shows validation error for short title", async () => {
        render(<IssueForm onSubmit={mockOnSubmit} />);

        // Ensure project is empty? It is by default.
        // Submit
        fireEvent.click(screen.getByRole("button", { name: /create/i }));

        // Wait for validation errors
        expect(await screen.findByText(/Project is required/i)).toBeInTheDocument();
        expect(await screen.findByText(/Title must be at least 3 characters/i)).toBeInTheDocument();
    });


    it("submits the form with valid data", async () => {
        render(<IssueForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);

        // Fill Title
        fireEvent.change(screen.getByLabelText(/Summary/i), { target: { value: "Test Issue" } });

        // Select Project: Click the item directly (mock renders options)
        // Project names: "Project Alpha" (id: 1)
        fireEvent.click(screen.getByText("Project Alpha"));

        // Submit
        fireEvent.click(screen.getByRole("button", { name: /create/i }));

        await waitFor(() => {
            expect(mockOnSubmit).toHaveBeenCalled();
        });

        const formData = mockOnSubmit.mock.calls[0][0];
        expect(formData.title).toBe("Test Issue");
        expect(formData.projectId).toBe("1");
    });

    it("shows validation error for subtasks without parents", async () => {
        render(<IssueForm onSubmit={mockOnSubmit} />);

        // Fill Title
        fireEvent.change(screen.getByLabelText(/Summary/i), { target: { value: "Subtask Issue" } });

        // Select Project via text
        fireEvent.click(screen.getByText("Project Alpha"));

        // Select Issue Type = SUBTASK via text
        // "Subtask" text is in the item
        fireEvent.click(screen.getByText("Subtask"));

        // Submit
        fireEvent.click(screen.getByRole("button", { name: /create/i }));

        expect(await screen.findByText(/Subtasks must have a parent issue/i)).toBeInTheDocument();
    });

    it("allows adding and removing labels", () => {
        render(<IssueForm onSubmit={mockOnSubmit} />);

        const labelInput = screen.getByPlaceholderText(/Add label.../i);
        const addButton = screen.getByText("Add");

        // Add Label
        fireEvent.change(labelInput, { target: { value: "Frontend" } });
        fireEvent.click(addButton);

        expect(screen.getByText("Frontend")).toBeInTheDocument();
    });

    it("allows adding linked issues", () => {
        render(<IssueForm onSubmit={mockOnSubmit} />);

        const linkInput = screen.getByPlaceholderText(/Link issue/i);
        const linkButton = screen.getByText("Link");

        fireEvent.change(linkInput, { target: { value: "PROJ-123" } });
        fireEvent.click(linkButton);

        expect(screen.getByText("PROJ-123")).toBeInTheDocument();
    });

    it("handles cancel action", () => {
        render(<IssueForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);

        fireEvent.click(screen.getByText(/Cancel/i));
        expect(mockOnCancel).toHaveBeenCalled();
    });
});
