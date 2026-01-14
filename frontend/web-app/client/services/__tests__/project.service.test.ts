import { describe, it, expect, vi, beforeEach } from "vitest";
import { projectService } from "../project.service";
import api from "../api";

vi.mock("../api", () => ({
    default: {
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
        delete: vi.fn(),
    },
}));

describe("projectService", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe("getProjects", () => {
        it("should fetch projects", async () => {
            const mockProjects = [{ id: 1, name: "Project A" }];
            (api.get as any).mockResolvedValue({ data: { success: true, data: mockProjects } });

            const result = await projectService.getProjects();

            expect(api.get).toHaveBeenCalledWith("/projects", { params: undefined });
            expect(result.data).toEqual(mockProjects);
        });
    });

    describe("createProject", () => {
        it("should create a project", async () => {
            const mockProject = { id: 1, name: "New Project" };
            (api.post as any).mockResolvedValue({ data: { success: true, data: mockProject } });

            const result = await projectService.createProject({
                name: "New Project",
                key: "NEW",
                description: "Test"
            });

            expect(api.post).toHaveBeenCalledWith("/projects", {
                name: "New Project",
                key: "NEW",
                description: "Test"
            });
            expect(result.data).toEqual(mockProject);
        });
    });

    describe("archiveProject", () => {
        it("should archive a project", async () => {
            (api.put as any).mockResolvedValue({ data: { success: true } });

            await projectService.archiveProject(1);

            expect(api.put).toHaveBeenCalledWith("/projects/1/archive");
        });
    });
});
