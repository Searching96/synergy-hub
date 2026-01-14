import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import ProjectChatRoom from "../ProjectChatRoom";

// Mock Child Components
vi.mock("../ChatMessageBubble", () => ({
    ChatMessageBubble: ({ message, isCurrentUser }: any) => (
        <div data-testid="message-bubble" data-user-id={message.userId}>
            {message.message}
            {isCurrentUser && <span>(Me)</span>}
        </div>
    ),
}));

vi.mock("../ChatInput", () => ({
    ChatInput: ({ onSend, placeholder }: any) => (
        <div>
            <input
                placeholder={placeholder}
                onKeyDown={(e) => {
                    if (e.key === 'Enter') onSend((e.target as HTMLInputElement).value);
                }}
            />
        </div>
    ),
}));

describe("ProjectChatRoom", () => {
    const mockMessages = [
        {
            id: 1,
            projectId: 1,
            userId: 1,
            user: { id: 1, name: "User 1" },
            message: "Hello world",
            timestamp: new Date().toISOString(),
            reactions: [],
            attachments: []
        },
        {
            id: 2,
            projectId: 1,
            userId: 2,
            user: { id: 2, name: "User 2" },
            message: "Hey there",
            timestamp: new Date().toISOString(),
            reactions: [],
            attachments: []
        },
    ];

    it("should render messages grouped by date", () => {
        Element.prototype.scrollIntoView = vi.fn();

        render(
            <ProjectChatRoom
                projectId={1}
                projectName="Test Project"
                currentUserId={1}
                messages={mockMessages}
            />
        );

        expect(screen.getByText("Hello world")).toBeInTheDocument();
        expect(screen.getByText("Hey there")).toBeInTheDocument();
        expect(screen.getByText("Today")).toBeInTheDocument(); // Date separator
    });

    it("should identify current user's messages", () => {
        render(
            <ProjectChatRoom
                projectId={1}
                projectName="Test Project"
                currentUserId={1}
                messages={mockMessages}
            />
        );

        // One of them should have "(Me)" text based on mock
        expect(screen.getByText(/Hello world/)).toHaveTextContent("(Me)");
        expect(screen.getByText(/Hey there/)).not.toHaveTextContent("(Me)");
    });

    it("should call onSendMessage when input is submitted", () => {
        const onSendMessage = vi.fn();
        render(
            <ProjectChatRoom
                projectId={1}
                projectName="Test Project"
                currentUserId={1}
                onSendMessage={onSendMessage}
            />
        );

        const input = screen.getByPlaceholderText(/Message Test Project/i);
        fireEvent.change(input, { target: { value: "New message" } });
        fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

        expect(onSendMessage).toHaveBeenCalledWith("New message", undefined);
    });

    it("should render empty state when no messages", () => {
        render(
            <ProjectChatRoom
                projectId={1}
                projectName="Test Project"
                currentUserId={1}
                messages={[]}
            />
        );

        expect(screen.getByText(/No messages yet/i)).toBeInTheDocument();
    });
});
