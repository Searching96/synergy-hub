/**
 * RBAC Permission Checking Utility
 * Maps role-based permissions for client-side checks
 */

export type UserRole = "ADMIN" | "PROJECT_LEAD" | "DEVELOPER" | "VIEWER";

// Define permissions for each role
const ROLE_PERMISSIONS: Record<UserRole, string[]> = {
  ADMIN: [
    "TASK_CREATE",
    "TASK_READ",
    "TASK_UPDATE",
    "TASK_DELETE",
    "TASK_MOVE",
    "TASK_ASSIGN",
    "TASK_ARCHIVE",
    "PROJECT_MANAGE",
    "MEMBER_MANAGE",
  ],
  PROJECT_LEAD: [
    "TASK_CREATE",
    "TASK_READ",
    "TASK_UPDATE",
    "TASK_DELETE",
    "TASK_MOVE",
    "TASK_ASSIGN",
    "TASK_ARCHIVE",
    "PROJECT_MANAGE",
  ],
  DEVELOPER: [
    "TASK_CREATE",
    "TASK_READ",
    "TASK_UPDATE",
    "TASK_MOVE",
    "TASK_ASSIGN",
  ],
  VIEWER: ["TASK_READ"],
};

export interface ProjectMemberWithRole {
  userId: number;
  name: string;
  email: string;
  role: UserRole;
  joinedAt: string;
}

/**
 * Check if a user has a specific permission
 */
export function hasPermission(
  userRole: UserRole | undefined,
  permission: string
): boolean {
  if (!userRole) return false;
  const permissions = ROLE_PERMISSIONS[userRole];
  return permissions ? permissions.includes(permission) : false;
}

/**
 * Check if user can move tasks (required for drag-and-drop)
 */
export function canMoveTask(userRole: UserRole | undefined): boolean {
  return hasPermission(userRole, "TASK_MOVE");
}

/**
 * Check if user can edit task (status, priority, assignee)
 */
export function canEditTask(userRole: UserRole | undefined): boolean {
  return hasPermission(userRole, "TASK_UPDATE");
}

/**
 * Check if user can delete task
 */
export function canDeleteTask(userRole: UserRole | undefined): boolean {
  return hasPermission(userRole, "TASK_DELETE");
}

/**
 * Check if user can archive task
 */
export function canArchiveTask(userRole: UserRole | undefined): boolean {
  return hasPermission(userRole, "TASK_ARCHIVE");
}

/**
 * Check if user can create task
 */
export function canCreateTask(userRole: UserRole | undefined): boolean {
  return hasPermission(userRole, "TASK_CREATE");
}

/**
 * Check if user can assign tasks
 */
export function canAssignTask(userRole: UserRole | undefined): boolean {
  return hasPermission(userRole, "TASK_ASSIGN");
}

/**
 * Get user's role in a project
 */
export function getUserRoleInProject(
  userId: number,
  members: ProjectMemberWithRole[]
): UserRole | undefined {
  const member = members.find((m) => m.userId === userId);
  return member?.role;
}

/**
 * Check if user is project admin/lead
 */
export function isProjectOwnerOrLead(userRole: UserRole | undefined): boolean {
  return userRole === "ADMIN" || userRole === "PROJECT_LEAD";
}
