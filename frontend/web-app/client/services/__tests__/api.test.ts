import { describe, it, expect, vi, beforeEach } from "vitest";
import api from "../api";

describe("API Service Interceptors", () => {
    beforeEach(() => {
        localStorage.clear();
    });

    it("should add Authorization header if token exists", async () => {
        localStorage.setItem("token", "test-token");

        // Test the internal request interceptor logic
        const requestHandlers = (api.interceptors.request as any).handlers;
        const interceptor = requestHandlers.find((h: any) => h.fulfilled !== undefined).fulfilled;

        const config = { headers: {} };
        const result = interceptor(config);

        expect(result.headers.Authorization).toBe("Bearer test-token");
    });

    it("should add X-Organization-ID header if organizationId exists", async () => {
        localStorage.setItem("organizationId", "42");

        const requestHandlers = (api.interceptors.request as any).handlers;
        const interceptor = requestHandlers.find((h: any) => h.fulfilled !== undefined).fulfilled;

        const config = { headers: {} };
        const result = interceptor(config);

        expect(result.headers["X-Organization-ID"]).toBe("42");
    });

    it("should normalize status values in responses", async () => {
        const responseHandlers = (api.interceptors.response as any).handlers;
        const interceptor = responseHandlers.find((h: any) => h.fulfilled !== undefined).fulfilled;

        const mockResponse = {
            data: {
                data: {
                    status: "TO_DO",
                    title: "Test Task"
                }
            }
        };

        const result = interceptor(mockResponse);
        expect(result.data.data.status).toBe("TO_DO");
    });

    it("should handle error responses in interceptor", async () => {
        const responseHandlers = (api.interceptors.response as any).handlers;
        const errorInterceptor = responseHandlers.find((h: any) => h.rejected !== undefined).rejected;

        const mockError = {
            config: { url: "/test", headers: {} },
            response: { status: 403, data: { message: "Forbidden" } }
        };

        // Should reject with original error
        await expect(errorInterceptor(mockError)).rejects.toEqual(mockError);
    });
});
