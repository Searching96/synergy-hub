/**
 * Enum validation schemas using Zod
 * Ensures API responses match expected types
 */

import { z } from "zod";

// Define valid enum values
export const TaskStatusSchema = z.enum([
  "TO_DO",
  "IN_PROGRESS",
  "IN_REVIEW",
  "DONE",
  "BLOCKED",
]);

export const TaskPrioritySchema = z.enum([
  "LOW",
  "MEDIUM",
  "HIGH",
  "CRITICAL",
]);

export const TaskTypeSchema = z.enum([
  "TASK",
  "BUG",
  "STORY",
  "EPIC",
  "CHORE",
]);

export const UserRoleSchema = z.enum([
  "ADMIN",
  "PROJECT_LEAD",
  "DEVELOPER",
  "VIEWER",
]);

// User schema
export const UserSchema = z.object({
  id: z.number(),
  name: z.string(),
  email: z.string().email(),
  roles: z.array(z.string()).optional(),
  permissions: z.array(z.string()).optional(),
  emailVerified: z.boolean().optional(),
  twoFactorEnabled: z.boolean().optional(),
  createdAt: z.string().optional(),
});

// Task schema
export const TaskSchema = z.object({
  id: z.number(),
  projectId: z.number(),
  projectName: z.string().optional(),
  title: z.string(),
  description: z.string().nullable().optional(),
  status: TaskStatusSchema,
  priority: TaskPrioritySchema,
  type: TaskTypeSchema,
  sprintId: z.number().nullable().optional(),
  sprintName: z.string().nullable().optional(),
  assigneeId: z.number().nullable().optional(),
  assigneeName: z.string().nullable().optional(),
  assignee: z
    .object({
      id: z.number(),
      name: z.string(),
      email: z.string().optional(),
    })
    .nullable()
    .optional(),
  reporterName: z.string().optional(),
  reporter: z.any().optional(),
  createdBy: z.any().optional(),
  storyPoints: z.number().nullable().optional(),
  dueDate: z.string().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
  overdue: z.boolean().optional(),
  archived: z.boolean().optional(),
  commentCount: z.number().optional(),
  estimatedHours: z.number().optional(),
  actualHours: z.number().optional(),
  goal: z.string().optional(),
});

// Generic API response schema
export const ApiResponseSchema = z.object({
  success: z.boolean(),
  message: z.string().nullable(),
  data: z.any(),
  errors: z
    .array(
      z.object({
        field: z.string(),
        message: z.string(),
      })
    )
    .optional(),
  timestamp: z.string().optional(),
  path: z.string().optional(),
});

// Type exports
export type TaskStatus = z.infer<typeof TaskStatusSchema>;
export type TaskPriority = z.infer<typeof TaskPrioritySchema>;
export type TaskType = z.infer<typeof TaskTypeSchema>;
export type UserRole = z.infer<typeof UserRoleSchema>;
export type User = z.infer<typeof UserSchema>;
export type Task = z.infer<typeof TaskSchema>;
export type ApiResponse<T = any> = z.infer<typeof ApiResponseSchema> & {
  data: T;
};

/**
 * Validate and coerce API response data
 * Handles unknown enum values with fallbacks
 */
export function validateTask(data: unknown): Task {
  try {
    return TaskSchema.parse(data);
  } catch (error) {
    console.error("Task validation error:", error);
    // Return a partial task with safe defaults
    const unsafeData = data as any;
    return {
      id: unsafeData?.id || 0,
      projectId: unsafeData?.projectId || 0,
      title: unsafeData?.title || "Unknown Task",
      status: "TO_DO",
      priority: "MEDIUM",
      type: "TASK",
      createdAt: unsafeData?.createdAt || new Date().toISOString(),
      updatedAt: unsafeData?.updatedAt || new Date().toISOString(),
      ...unsafeData,
    };
  }
}

/**
 * Safe enum value getter with fallback
 */
export function getTaskStatus(value: unknown): TaskStatus {
  if (typeof value === "string" && TaskStatusSchema.safeParse(value).success) {
    return value as TaskStatus;
  }
  return "TO_DO";
}

export function getTaskPriority(value: unknown): TaskPriority {
  if (typeof value === "string" && TaskPrioritySchema.safeParse(value).success) {
    return value as TaskPriority;
  }
  return "MEDIUM";
}

export function getTaskType(value: unknown): TaskType {
  if (typeof value === "string" && TaskTypeSchema.safeParse(value).success) {
    return value as TaskType;
  }
  return "TASK";
}
