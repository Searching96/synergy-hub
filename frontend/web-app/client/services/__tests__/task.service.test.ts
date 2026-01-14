import { describe, it, expect, vi, beforeEach } from "vitest";
import { taskService } from "../task.service";
import api from "../api";

vi.mock("../api", () => ({
    default: {
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
        delete: vi.fn(),
    },
}));

describe("taskService", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe("createTask", () => {
        it("should create a task", async () => {
            const mockTask = { id: 1, title: "New Task" };
            (api.post as any).mockResolvedValue({ data: { success: true, data: mockTask } });

            const result = await taskService.createTask({
                title: "New Task",
                projectId: 1,
                type: "STORY",
                status: "TO_DO",
                priority: "MEDIUM"
            });

            expect(api.post).toHaveBeenCalledWith("/tasks", expect.objectContaining({
                title: "New Task",
                projectId: 1
            }));
            expect(result.data).toEqual(mockTask);
        });
    });

    describe("updateTask", () => {
        it("should update task properties", async () => {
            const mockTask = { id: 1, status: "DONE" };
            (api.put as any).mockResolvedValue({ data: { success: true, data: mockTask } });

            const result = await taskService.updateTask(1, { status: "DONE" });

            expect(api.put).toHaveBeenCalledWith("/tasks/1", { status: "DONE" });
            expect(result.data).toEqual(mockTask);
        });
    });

    describe("deleteTask", () => {
        it("should delete a task", async () => {
            (api.delete as any).mockResolvedValue({ data: { success: true } });

            await taskService.deleteTask(1);

            expect(api.delete).toHaveBeenCalledWith("/tasks/1");
        });
    });
});
