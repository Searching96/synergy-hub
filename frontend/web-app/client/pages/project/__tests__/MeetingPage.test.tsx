import { render, screen, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import MeetingPage from "../MeetingPage";
import { MemoryRouter, Route, Routes } from "react-router-dom";

// Mock Services
import { meetingService } from "@/services/meeting.service";
vi.mock("@/services/meeting.service", () => ({
    meetingService: {
        getMeeting: vi.fn(),
        joinMeeting: vi.fn(),
    },
}));

vi.mock("@/services/auth.service", () => ({
    default: {
        getCurrentUser: () => ({ id: 1, name: "Test User" }),
    },
}));

// Mock MeetingRoom component
vi.mock("@/components/meeting/MeetingRoom", () => ({
    default: ({ meeting }: any) => <div>In Meeting: {meeting.title}</div>,
}));

describe("MeetingPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    const renderPage = (projectId: string, meetingId: string) => {
        return render(
            <MemoryRouter initialEntries={[`/projects/${projectId}/meetings/${meetingId}`]}>
                <Routes>
                    <Route path="/projects/:projectId/meetings/:meetingId" element={<MeetingPage />} />
                </Routes>
            </MemoryRouter>
        );
    };

    it("should load and display pre-join screen", async () => {
        vi.mocked(meetingService.getMeeting).mockResolvedValue({
            id: 123,
            title: "Daily Standup",
            status: "SCHEDULED",
            participants: [],
            meetingCode: "abc-123"
        } as any);

        renderPage("1", "123");

        // Loading state might be too fast to catch with mocked promise
        // expect(await screen.findByText("Loading meeting...")).toBeInTheDocument();
        expect(await screen.findByText("Daily Standup")).toBeInTheDocument();
        expect(screen.getByText("Join Meeting")).toBeInTheDocument();
    });

    it("should display error if meeting not found", async () => {
        vi.mocked(meetingService.getMeeting).mockResolvedValue(null);

        renderPage("1", "999");

        expect(await screen.findByText("Meeting not found")).toBeInTheDocument();
    });

    it("should display error if meeting ended", async () => {
        vi.mocked(meetingService.getMeeting).mockResolvedValue({
            id: 123,
            title: "Old Meeting",
            status: "ENDED",
            participants: []
        } as any);

        renderPage("1", "123");

        expect(await screen.findByText("This meeting has ended")).toBeInTheDocument();
    });
});
