import { renderHook, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { useCreateTask, useProjectTasks, useTask } from "../useTasks";
import { taskService } from "@/services/task.service";
import { useToast } from "@/hooks/use-toast";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactNode } from "react";

// Mock dependencies
vi.mock("@/services/task.service");
vi.mock("@/hooks/use-toast");
vi.mock("@tanstack/react-query", async () => {
    const actual = await vi.importActual("@tanstack/react-query");
    return {
        ...actual,
        useQueryClient: () => ({
            invalidateQueries: vi.fn(),
        }),
    };
});

const mockToast = vi.fn();
(useToast as any).mockReturnValue({ toast: mockToast });

const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            retry: false,
        },
    },
});

const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

describe("useTasks Hooks", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe("useCreateTask", () => {
        it("should call taskService.createTask and show success toast", async () => {
            (taskService.createTask as any).mockResolvedValue({ data: { id: 1 } });

            const { result } = renderHook(() => useCreateTask(), { wrapper });

            await result.current.mutateAsync({ title: "New Task" } as any);

            expect(taskService.createTask).toHaveBeenCalled();
            expect(mockToast).toHaveBeenCalledWith(expect.objectContaining({
                title: "Success"
            }));
        });

        it("should handle creation error", async () => {
            (taskService.createTask as any).mockRejectedValue({ response: { data: { message: "Failed" } } });

            const { result } = renderHook(() => useCreateTask(), { wrapper });

            try {
                await result.current.mutateAsync({ title: "New Task" } as any);
            } catch (e) {
                // Ignore error, we check toast
            }

            expect(mockToast).toHaveBeenCalledWith(expect.objectContaining({
                title: "Error",
                description: "Failed"
            }));
        });
    });

    describe("useTask", () => {
        it("should fetch task by id", async () => {
            (taskService.getTask as any).mockResolvedValue({ id: 1, title: "My Task" });

            const { result } = renderHook(() => useTask(1), { wrapper });

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            expect(taskService.getTask).toHaveBeenCalledWith(1);
            expect(result.current.data).toEqual({ id: 1, title: "My Task" });
        });
    });

    describe("useProjectTasks", () => {
        it("should fetch project tasks", async () => {
            (taskService.getProjectTasks as any).mockResolvedValue([{ id: 1, title: "Task 1" }]);

            const { result } = renderHook(() => useProjectTasks(100), { wrapper });

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            expect(taskService.getProjectTasks).toHaveBeenCalledWith(100, undefined);
        });
    });
});
