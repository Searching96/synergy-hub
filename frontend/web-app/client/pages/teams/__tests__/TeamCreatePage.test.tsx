import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import TeamCreatePage from "../TeamCreatePage";
import { MemoryRouter } from "react-router-dom";
import { teamService } from "@/services/team.service";

// Mock Hooks & Services
const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
    const actual = await vi.importActual("react-router-dom");
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

vi.mock("@/services/team.service", () => ({
    teamService: {
        createTeam: vi.fn(() => Promise.resolve({ success: true })),
    },
}));

describe("TeamCreatePage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should render the create team form", () => {
        render(
            <MemoryRouter>
                <TeamCreatePage />
            </MemoryRouter>
        );

        expect(screen.getByText("Create a Team")).toBeInTheDocument();
        expect(screen.getByLabelText(/Team Name/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Description/i)).toBeInTheDocument();
    });

    it("should call createTeam and navigate on success", async () => {
        render(
            <MemoryRouter>
                <TeamCreatePage />
            </MemoryRouter>
        );

        const nameInput = screen.getByLabelText(/Team Name/i);
        const descInput = screen.getByLabelText(/Description/i);
        const submitBtn = screen.getByRole("button", { name: /Save Team/i });

        fireEvent.change(nameInput, { target: { value: "Engineering" } });
        fireEvent.change(descInput, { target: { value: "Main engineering team" } });
        fireEvent.click(submitBtn);

        await waitFor(() => {
            expect(teamService.createTeam).toHaveBeenCalledWith({
                name: "Engineering",
                description: "Main engineering team"
            });
            expect(mockNavigate).toHaveBeenCalledWith(-1);
        });
    });

    it("should disable buttons when submitting", async () => {
        vi.mocked(teamService.createTeam).mockReturnValue(new Promise(() => { })); // Never resolves

        render(
            <MemoryRouter>
                <TeamCreatePage />
            </MemoryRouter>
        );

        fireEvent.change(screen.getByLabelText(/Team Name/i), { target: { value: "Test" } });
        fireEvent.click(screen.getByRole("button", { name: /Save Team/i }));

        expect(screen.getByRole("button", { name: /Creating.../i })).toBeDisabled();
        expect(screen.getByRole("button", { name: /Cancel/i })).toBeDisabled();
    });
});
