import { describe, it, expect, vi, beforeEach } from "vitest";
import authService from "../auth.service";
import api from "../api";

// Mock the API instance
vi.mock("../api", () => ({
    default: {
        post: vi.fn(),
    },
}));

describe("authService", () => {
    let setItemSpy: any;
    let removeItemSpy: any;

    beforeEach(() => {
        vi.clearAllMocks();
        localStorage.clear();

        // Create spies and capture them
        setItemSpy = vi.spyOn(Storage.prototype, 'setItem');
        removeItemSpy = vi.spyOn(Storage.prototype, 'removeItem');

        // Suppress console logs
        vi.spyOn(console, 'warn').mockImplementation(() => { });
        vi.spyOn(console, 'error').mockImplementation(() => { });
    });

    describe("login", () => {
        it("should successfully login and store tokens", async () => {
            const mockResponse = {
                data: {
                    success: true,
                    data: {
                        accessToken: "fake-jwt-token",
                        refreshToken: "fake-refresh-token",
                        user: { id: 1, name: "Test User", email: "test@example.com" },
                    },
                },
            };

            (api.post as any).mockResolvedValue(mockResponse);

            const result = await authService.login("test@example.com", "password123");

            expect(api.post).toHaveBeenCalledWith("/auth/login", {
                email: "test@example.com",
                password: "password123",
            });
            expect(result).toEqual(mockResponse.data);
            expect(setItemSpy).toHaveBeenCalledWith("token", "fake-jwt-token");
            expect(setItemSpy).toHaveBeenCalledWith("refreshToken", "fake-refresh-token");
            expect(setItemSpy).toHaveBeenCalledWith("user", JSON.stringify(mockResponse.data.data.user));
        });
    });

    describe("register", () => {
        it("should register a new user", async () => {
            const mockResponse = {
                data: {
                    success: true,
                    message: "Registration successful",
                },
            };

            (api.post as any).mockResolvedValue(mockResponse);

            const result = await authService.register({
                name: "New User",
                email: "new@example.com",
                password: "password123",
                confirmPassword: "password123",
            });

            expect(api.post).toHaveBeenCalledWith("/auth/register", {
                name: "New User",
                email: "new@example.com",
                password: "password123",
            });
            expect(result).toEqual(mockResponse.data);
        });
    });

    describe("logout", () => {
        it("should clear storage even if server logout fails", async () => {
            (api.post as any).mockRejectedValue(new Error("Network error"));

            await authService.logout();

            expect(api.post).toHaveBeenCalledWith("/users/logout");
            expect(removeItemSpy).toHaveBeenCalledWith("token");
            expect(removeItemSpy).toHaveBeenCalledWith("user");
        });
    });

    describe("isAuthenticated", () => {
        it("should return true if token exists", () => {
            // For checking existence, we need to mock return value of getItem
            const getItemSpy = vi.spyOn(Storage.prototype, 'getItem').mockReturnValue("valid-token");

            expect(authService.isAuthenticated()).toBe(true);
        });

        it("should return false if token is missing", () => {
            const getItemSpy = vi.spyOn(Storage.prototype, 'getItem').mockReturnValue(null);
            expect(authService.isAuthenticated()).toBe(false);
        });
    });
});
