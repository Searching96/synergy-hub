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
 */
export function enrichTaskWithHierarchy(task: Task): Task {
  const enriched = { ...task };

  // Add epic information
  if (task.type !== "EPIC" && task.type !== "SUBTASK") {
    const parentIssue = mockParentIssues.find(p => p.id === task.id);
    if (parentIssue?.epicId) {
      const epic = mockEpics.find(e => e.id === parentIssue.epicId);
      if (epic) {
        enriched.epicId = epic.id;
        enriched.epic = { id: epic.id, title: epic.title };
      }
    }
  }

  // Add parent task information for subtasks
  if (task.type === "SUBTASK") {
    const subtask = mockSubtasks.find(s => s.id === task.id);
    if (subtask?.parentTaskId) {
      const parent = mockParentIssues.find(p => p.id === subtask.parentTaskId);
      if (parent) {
        enriched.parentTaskId = parent.id;
        enriched.parentTask = { id: parent.id, title: parent.title, type: parent.type };
      }
    }
  }

  // Add subtasks for parent issues
  if (task.type === "STORY" || task.type === "TASK" || task.type === "BUG") {
    const subtaskIds = parentToSubtasksMap[task.id] || [];
    if (subtaskIds.length > 0) {
      enriched.subtasks = mockSubtasks
        .filter(s => subtaskIds.includes(s.id))
        .map(s => ({
          ...s,
          projectId: task.projectId,
          title: s.title,
          description: null,
          status: "TO_DO" as any,
          priority: "MEDIUM" as any,
          type: s.type,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }));
    }
  }

  // Add child issues for epics
  if (task.type === "EPIC") {
    const childIds = epicToIssuesMap[task.id] || [];
    // Note: In real implementation, would fetch full child issues
    enriched.subtasks = childIds.map(id => {
      const parent = mockParentIssues.find(p => p.id === id);
      return parent ? {
        ...parent,
        projectId: task.projectId,
        description: null,
        status: "TO_DO" as any,
        priority: "MEDIUM" as any,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      } as Task : null;
    }).filter(Boolean) as Task[];
  }

  return enriched;
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
