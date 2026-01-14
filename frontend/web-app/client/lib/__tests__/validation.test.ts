import { describe, it, expect, vi } from "vitest";
import {
    TaskStatusSchema,
    TaskPrioritySchema,
    TaskTypeSchema,
    validateTask,
    getTaskStatus
} from "../validation";

describe("Validation Utils", () => {
    describe("Zod Enum Schemas", () => {
        it("should validate correct status", () => {
            expect(TaskStatusSchema.safeParse("TO_DO").success).toBe(true);
            expect(TaskStatusSchema.safeParse("DONE").success).toBe(true);
            expect(TaskStatusSchema.safeParse("INVALID").success).toBe(false);
        });

        it("should validate correct priority", () => {
            expect(TaskPrioritySchema.safeParse("HIGH").success).toBe(true);
            expect(TaskPrioritySchema.safeParse("LOW").success).toBe(true);
            expect(TaskPrioritySchema.safeParse("SUPER_HIGH").success).toBe(false);
        });
    });

    describe("validateTask", () => {
        it("should coerce invalid task data to safe default", () => {
            const invalidData = { id: 1, title: "Broken Task" }; // missing required fields
            const result = validateTask(invalidData);

            expect(result.status).toBe("TO_DO"); // Default fallback
            expect(result.priority).toBe("MEDIUM");
            expect(result.title).toBe("Broken Task");
        });

        it("should pass valid task data", () => {
            const validData = {
                id: 1,
                projectId: 1,
                title: "Good Task",
                status: "DONE",
                priority: "HIGH",
                type: "STORY",
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString(),
            };
            const result = validateTask(validData);
            expect(result).toMatchObject(validData);
        });
    });

    describe("getTaskStatus", () => {
        it("should return valid status unchanged", () => {
            expect(getTaskStatus("IN_PROGRESS")).toBe("IN_PROGRESS");
        });

        it("should fallback to TO_DO for invalid status", () => {
            expect(getTaskStatus("UNKNOWN_STATUS")).toBe("TO_DO");
            expect(getTaskStatus(null)).toBe("TO_DO");
        });
    });
});
