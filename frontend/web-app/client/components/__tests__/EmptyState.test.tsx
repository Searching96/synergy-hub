import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { EmptyState } from "../EmptyState";
import { AlertCircle } from "lucide-react";

describe("EmptyState Component", () => {
    it("renders the title and description", () => {
        render(
            <EmptyState
                icon={AlertCircle}
                title="No Results"
                description="Try adjusting your filters"
            />
        );

        expect(screen.getByText("No Results")).toBeInTheDocument();
        expect(screen.getByText("Try adjusting your filters")).toBeInTheDocument();
    });

    it("renders the action button when provided", () => {
        const onAction = vi.fn();
        render(
            <EmptyState
                icon={AlertCircle}
                title="Title"
                description="Desc"
                actionLabel="Click Me"
                onAction={onAction}
            />
        );

        const button = screen.getByRole("button", { name: /click me/i });
        expect(button).toBeInTheDocument();

        fireEvent.click(button);
        expect(onAction).toHaveBeenCalledTimes(1);
    });

    it("does not render the button if actionLabel or onAction is missing", () => {
        const { rerender } = render(
            <EmptyState
                icon={AlertCircle}
                title="Title"
                description="Desc"
                actionLabel="Click Me"
            />
        );
        expect(screen.queryByRole("button")).not.toBeInTheDocument();

        rerender(
            <EmptyState
                icon={AlertCircle}
                title="Title"
                description="Desc"
                onAction={() => { }}
            />
        );
        expect(screen.queryByRole("button")).not.toBeInTheDocument();
    });
});
