import { render, screen, waitFor, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { AuthProvider, useAuth } from "../AuthContext";
import authService from "@/services/auth.service";
import { MemoryRouter, useNavigate } from "react-router-dom";
import { useEffect } from "react";

// Mock authService
vi.mock("@/services/auth.service", () => ({
    default: {
        getToken: vi.fn(),
        getCurrentUser: vi.fn(),
        login: vi.fn(),
        logout: vi.fn(),
    },
}));

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
    const actual = await vi.importActual("react-router-dom");
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

const TestComponent = () => {
    const { user, login, logout, loading } = useAuth();
    if (loading) return <div>Loading...</div>;
    return (
        <div>
            {user ? <div>User: {user.name}</div> : <div>Guest</div>}
            <button onClick={() => login("test@example.com", "password")}>Login</button>
            <button onClick={logout}>Logout</button>
        </div>
    );
};

describe("AuthContext", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should initialize with user if token exists", async () => {
        (authService.getToken as any).mockReturnValue("valid-token");
        (authService.getCurrentUser as any).mockReturnValue({ id: 1, name: "Existing User" });

        render(
            <MemoryRouter>
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            </MemoryRouter>
        );

        expect(await screen.findByText("User: Existing User")).toBeInTheDocument();
    });

    it("should initialize as guest if no token", async () => {
        (authService.getToken as any).mockReturnValue(null);

        render(
            <MemoryRouter>
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            </MemoryRouter>
        );

        expect(await screen.findByText("Guest")).toBeInTheDocument();
    });

    it("should login successfully", async () => {
        (authService.getToken as any).mockReturnValue(null);
        (authService.login as any).mockResolvedValue({
            success: true,
            data: { user: { id: 1, name: "Logged In User" } }
        });

        render(
            <MemoryRouter>
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            </MemoryRouter>
        );

        const loginBtn = await screen.findByText("Login");
        await act(async () => {
            loginBtn.click();
        });

        expect(await screen.findByText("User: Logged In User")).toBeInTheDocument();
    });

    it("should logout successfully", async () => {
        (authService.getToken as any).mockReturnValue("token");
        (authService.getCurrentUser as any).mockReturnValue({ id: 1, name: "User" });

        render(
            <MemoryRouter>
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            </MemoryRouter>
        );

        const logoutBtn = await screen.findByText("Logout");
        await act(async () => {
            logoutBtn.click();
        });

        expect(authService.logout).toHaveBeenCalled();
        expect(mockNavigate).toHaveBeenCalledWith("/login");
        expect(await screen.findByText("Guest")).toBeInTheDocument();
    });
});
