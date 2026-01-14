/**
 * Mock Issue Hierarchy Data
 * Simulates Jira-style issue relationships until backend support is available
 */

import type { Task, TaskType } from "@/types/task.types";

/**
 * Mock epics for the application
 */
export const mockEpics = [
  { id: 2001, title: "User Management System", projectId: 1 },
  { id: 2002, title: "Reporting Dashboard", projectId: 1 },
  { id: 2003, title: "Mobile App Development", projectId: 1 },
  { id: 2004, title: "Performance Optimization", projectId: 2 },
  { id: 2005, title: "Security Enhancements", projectId: 2 },
];

/**
 * Mock parent issues (Stories, Tasks, Bugs) that can have subtasks
 */
export const mockParentIssues = [
  { id: 1001, title: "Implement user authentication", type: "STORY" as TaskType, epicId: 2001, projectId: 1 },
  { id: 1002, title: "Create dashboard layout", type: "STORY" as TaskType, epicId: 2002, projectId: 1 },
  { id: 1003, title: "Fix responsive issues", type: "BUG" as TaskType, epicId: null, projectId: 1 },
  { id: 1004, title: "Setup API endpoints", type: "TASK" as TaskType, epicId: 2001, projectId: 1 },
  { id: 1005, title: "Design database schema", type: "TASK" as TaskType, epicId: 2001, projectId: 1 },
];

/**
 * Mock subtasks
 */
export const mockSubtasks = [
  { id: 3001, title: "Create login form", type: "SUBTASK" as TaskType, parentTaskId: 1001 },
  { id: 3002, title: "Add JWT authentication", type: "SUBTASK" as TaskType, parentTaskId: 1001 },
  { id: 3003, title: "Implement password reset", type: "SUBTASK" as TaskType, parentTaskId: 1001 },
  { id: 3004, title: "Design header component", type: "SUBTASK" as TaskType, parentTaskId: 1002 },
  { id: 3005, title: "Create sidebar navigation", type: "SUBTASK" as TaskType, parentTaskId: 1002 },
  { id: 3006, title: "Fix mobile menu overflow", type: "SUBTASK" as TaskType, parentTaskId: 1003 },
];

/**
 * Relationship map: epicId -> issue IDs
 */
export const epicToIssuesMap: Record<number, number[]> = {
  2001: [1001, 1004, 1005],
  2002: [1002],
  2003: [],
  2004: [],
  2005: [],
};

/**
 * Relationship map: parentTaskId -> subtask IDs
 */
export const parentToSubtasksMap: Record<number, number[]> = {
  1001: [3001, 3002, 3003],
  1002: [3004, 3005],
  1003: [3006],
  1004: [],
  1005: [],
};

/**
 * Enrich a task with hierarchy information
 * 
 * NOTE: This function is deprecated. The backend now returns epic and parentTask
 * relationships directly in the Task entity. This function now just returns
 * the task as-is for backward compatibility.
 */
export function enrichTaskWithHierarchy(task: Task): Task {
  return task;
}

/**
 * Get available parent issues for creating a subtask
 */
export function getAvailableParents(projectId: number): typeof mockParentIssues {
  return mockParentIssues.filter(issue => issue.projectId === projectId);
}

/**
 * Get available epics for linking issues
 */
export function getAvailableEpics(projectId: number): typeof mockEpics {
  return mockEpics.filter(epic => epic.projectId === projectId);
}

/**
 * Check if an issue type can have subtasks
 */
export function canHaveSubtasks(type: TaskType): boolean {
  return type === "STORY" || type === "TASK" || type === "BUG";
}

/**
 * Check if an issue type can belong to an epic
 */
export function canBelongToEpic(type: TaskType): boolean {
  return type === "STORY" || type === "TASK" || type === "BUG";
}

/**
 * Check if an issue type needs a parent
 */
export function requiresParent(type: TaskType): boolean {
  return type === "SUBTASK";
}

/**
 * Get the hierarchy level of an issue type
 * 0 = Epic, 1 = Story/Task/Bug, 2 = Subtask
 */
export function getHierarchyLevel(type: TaskType): number {
  if (type === "EPIC") return 0;
  if (type === "SUBTASK") return 2;
  return 1;
}
