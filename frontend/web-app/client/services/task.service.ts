import { Attachment } from "@/types/attachment.types";
import api from "./api";
import type { ApiResponse } from "@/types/auth.types";
import type { PaginatedResponse } from "@/types/project.types";
import type {
  Task,
  CreateTaskRequest,
  UpdateTaskRequest,
  TaskFilters,
  BoardData,
} from "@/types/task.types";

export const taskService = {
  // Create a new task
  async createTask(data: CreateTaskRequest): Promise<ApiResponse<Task>> {
    const response = await api.post<ApiResponse<Task>>("/tasks", data);
    return response.data;
  },

  // Update an existing task
  async updateTask(taskId: number | string, data: UpdateTaskRequest): Promise<ApiResponse<Task>> {
    const response = await api.put<ApiResponse<Task>>(`/tasks/${taskId}`, data);
    return response.data;
  },

  // Delete a task
  async deleteTask(taskId: number | string): Promise<ApiResponse<void>> {
    const response = await api.delete<ApiResponse<void>>(`/tasks/${taskId}`);
    return response.data;
  },

  // Archive a task
  async archiveTask(taskId: number | string): Promise<ApiResponse<void>> {
    const response = await api.put<ApiResponse<void>>(`/tasks/${taskId}/archive`, {});
    return response.data;
  },

  // Unarchive a task
  async unarchiveTask(taskId: number | string): Promise<ApiResponse<void>> {
    const response = await api.put<ApiResponse<void>>(`/tasks/${taskId}/unarchive`, {});
    return response.data;
  },

  // Get single task by ID
  async getTask(taskId: number | string): Promise<ApiResponse<Task>> {
    const response = await api.get<ApiResponse<Task>>(`/tasks/${taskId}`);
    return response.data;
  },

  // Get project tasks
  async getProjectTasks(
    projectId: number | string,
    params: TaskFilters | Record<string, unknown> = {}
  ): Promise<ApiResponse<PaginatedResponse<Task> | Task[]>> {
    const response = await api.get<ApiResponse<PaginatedResponse<Task> | Task[]>>(`/projects/${projectId}/tasks`, { params });
    return response.data;
  },

  // Get project board (Kanban view)
  async getProjectBoard(projectId: number | string): Promise<ApiResponse<BoardData>> {
    const response = await api.get<ApiResponse<BoardData>>(`/projects/${projectId}/board`);
    return response.data;
  },

  // Move task to sprint
  async moveTaskToSprint(taskId: number | string, sprintId: number | string): Promise<ApiResponse<Task>> {
    const response = await api.post<ApiResponse<Task>>(`/tasks/${taskId}/move-to-sprint/${sprintId}`);
    return response.data;
  },

  // Move task to backlog
  async moveTaskToBacklog(taskId: number | string): Promise<ApiResponse<Task>> {
    const response = await api.post<ApiResponse<Task>>(`/tasks/${taskId}/move-to-backlog`);
    return response.data;
  },

  // Update task assignee
  async updateTaskAssignee(taskId: number | string, assigneeId: number | null): Promise<ApiResponse<Task>> {
    const response = await api.put<ApiResponse<Task>>(`/tasks/${taskId}/assignee`, { assigneeId });
    return response.data;
  },

  // Get backlog tasks for a project
  async getBacklogTasks(projectId: number | string): Promise<ApiResponse<Task[]>> {
    const response = await api.get<ApiResponse<Task[]>>(`/projects/${projectId}/backlog`);
    return response.data;
  },

  // Get sprint tasks
  async getSprintTasks(sprintId: number | string): Promise<ApiResponse<Task[]>> {
    const response = await api.get<ApiResponse<Task[]>>(`/sprints/${sprintId}/tasks`);
    return response.data;
  },

  // Get my tasks
  async getMyTasks(params?: TaskFilters): Promise<ApiResponse<PaginatedResponse<Task>>> {
    const response = await api.get<ApiResponse<PaginatedResponse<Task>>>("/tasks/my-tasks", { params });
    return response.data;
  },

  // Get task subtasks
  async getTaskSubtasks(taskId: number | string): Promise<ApiResponse<Task[]>> {
    const response = await api.get<ApiResponse<Task[]>>(`/tasks/${taskId}/subtasks`);
    return response.data;
  },

  // Get project epics
  async getProjectEpics(projectId: number | string): Promise<ApiResponse<Task[]>> {
    const response = await api.get<ApiResponse<Task[]>>(`/projects/${projectId}/epics`);
    return response.data;
  },

  // Get epic children (issues belonging to an epic)
  async getEpicChildren(epicId: number | string): Promise<ApiResponse<Task[]>> {
    const response = await api.get<ApiResponse<Task[]>>(`/tasks/${epicId}/epic-children`);
    return response.data;
  },

  // Get task attachments
  async getTaskAttachments(taskId: number | string): Promise<Attachment[]> {
    const response = await api.get<Attachment[]>(`/tasks/${taskId}/attachments`);
    return response.data;
  },

  async uploadAttachment(
    taskId: number,
    file: File,
    onProgress?: (progress: number) => void
  ): Promise<ApiResponse<Attachment>> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await api.post<ApiResponse<Attachment>>(
      `/tasks/${taskId}/attachments`,
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (e) => {
          if (onProgress && e.total) {
            onProgress(Math.round((e.loaded * 100) / e.total));
          }
        }
      }
    );
    return response.data;
  },

  async deleteAttachment(taskId: number | string, attachmentId: number | string): Promise<ApiResponse<void>> {
    return api.delete(`/tasks/${taskId}/attachments/${attachmentId}`);
  },

  async downloadAttachment(taskId: number | string, attachment: Attachment): Promise<void> {
    // Use signed URL from backend for secure download
    const { data } = await api.get<string>(
      `/tasks/${taskId}/attachments/${attachment.id}/download-url`
    );
    // If data is an object with url property (legacy), use that, otherwise use data directly if it is string
    const url = (typeof data === 'object' && (data as any).url) ? (data as any).url : data;
    window.open(url, '_blank');
  },

  async bulkDownloadAttachments(taskId: number | string, attachmentIds: number[]): Promise<void> {
    // Backend creates zip file and returns download URL
    const { data } = await api.post<string>(
      `/tasks/${taskId}/attachments/bulk-download`, // Assuming this exists or works similar to above?
      { attachmentIds }
    );
    const url = (typeof data === 'object' && (data as any).url) ? (data as any).url : data;
    window.open(url, '_blank');
  },

  // Watch a task
  async watchTask(taskId: number | string): Promise<ApiResponse<Task>> {
    const response = await api.put<ApiResponse<Task>>(`/tasks/${taskId}/watch`);
    return response.data;
  },

  // Unwatch a task
  async unwatchTask(taskId: number | string): Promise<ApiResponse<Task>> {
    const response = await api.delete<ApiResponse<Task>>(`/tasks/${taskId}/watch`);
    return response.data;
  },

  // Link tasks
  async linkTasks(taskId: number | string, linkedTaskId: number | string): Promise<ApiResponse<Task>> {
    const response = await api.post<ApiResponse<Task>>(`/tasks/${taskId}/links/${linkedTaskId}`);
    return response.data;
  }
};
