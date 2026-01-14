import { renderHook, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { useProjects, useCreateProject } from "../useProjects";
import { projectService } from "@/services/project.service";
import { useToast } from "@/hooks/use-toast";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactNode } from "react";

vi.mock("@/services/project.service");
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

describe("useProjects Hooks", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe("useProjects", () => {
        it("should fetch projects", async () => {
            (projectService.getProjects as any).mockResolvedValue({ data: [] });

            const { result } = renderHook(() => useProjects(), { wrapper });

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            expect(projectService.getProjects).toHaveBeenCalled();
        });

        it("should fetch projects with filter", async () => {
            (projectService.getProjects as any).mockResolvedValue({ data: [] });
            const filter = { status: "ACTIVE" };

            const { result } = renderHook(() => useProjects(filter as any), { wrapper });

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            expect(projectService.getProjects).toHaveBeenCalledWith(filter);
        });
    });

    describe("useCreateProject", () => {
        it("should call createProject and toast success", async () => {
            (projectService.createProject as any).mockResolvedValue({ id: 1 });

            const { result } = renderHook(() => useCreateProject(), { wrapper });

            await result.current.mutateAsync({ name: "New Project", key: "NP", description: "Desc" });

            expect(projectService.createProject).toHaveBeenCalled();
            expect(mockToast).toHaveBeenCalledWith(expect.objectContaining({ title: "Success" }));
        });
    });
});
