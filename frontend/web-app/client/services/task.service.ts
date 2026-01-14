import { Attachment } from "@/types/attachment.types";
import api from "./api";
import type { ApiResponse } from "@/types/auth.types";
import type {
  Task,
  CreateTaskRequest,
  UpdateTaskRequest,
  TaskFilters,
  BoardData,
} from "@/types/task.types";

export const taskService = {
  // Create a new task/issue
  async createTask(taskData: CreateTaskRequest): Promise<ApiResponse<Task>> {
    const response = await api.post<ApiResponse<Task>>("/tasks", taskData);
    return response.data;
  },

  // Get task by ID
  async getTaskById(taskId: number | string): Promise<ApiResponse<Task>> {
    const response = await api.get<ApiResponse<Task>>(`/tasks/${taskId}`);
    return response.data;
  },

  // Update task
  async updateTask(taskId: number | string, taskData: UpdateTaskRequest): Promise<ApiResponse<Task>> {
    const response = await api.put<ApiResponse<Task>>(`/tasks/${taskId}`, taskData);
    return response.data;
  },

  // Delete task (permanently)
  async deleteTask(taskId: number | string): Promise<ApiResponse<unknown>> {
    const response = await api.delete<ApiResponse<unknown>>(`/tasks/${taskId}`);
    return response.data;
  },

  // Archive task
  async archiveTask(taskId: number | string): Promise<ApiResponse<Task>> {
    const response = await api.put<ApiResponse<Task>>(`/tasks/${taskId}/archive`);
    return response.data;
  },

  // Unarchive task
  async unarchiveTask(taskId: number | string): Promise<ApiResponse<Task>> {
    const response = await api.put<ApiResponse<Task>>(`/tasks/${taskId}/unarchive`);
    return response.data;
  },

  // Get project tasks
  async getProjectTasks(
    projectId: number | string,
    params: TaskFilters | Record<string, unknown> = {}
  ): Promise<ApiResponse<Task[]>> {
    const response = await api.get<ApiResponse<Task[]>>(`/projects/${projectId}/tasks`, { params });
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
  async getMyTasks(params?: TaskFilters): Promise<ApiResponse<Task[]>> {
    const response = await api.get<ApiResponse<Task[]>>("/tasks/my-tasks", { params });
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
  async getTaskAttachments(taskId: number | string): Promise<ApiResponse<Attachment[]>> {
    const response = await api.get<ApiResponse<Attachment[]>>(`/tasks/${taskId}/attachments`);
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

  async deleteAttachment(attachmentId: number): Promise<ApiResponse<void>> {
    return api.delete(`/attachments/${attachmentId}`);
  },

  async downloadAttachment(attachment: Attachment): Promise<void> {
    // Use signed URL from backend for secure download
    const { data } = await api.get<{ url: string }>(
      `/attachments/${attachment.id}/download-url`
    );
    window.open(data.url, '_blank');
  },

  async bulkDownloadAttachments(attachmentIds: number[]): Promise<void> {
    // Backend creates zip file and returns download URL
    const { data } = await api.post<{ url: string }>(
      '/attachments/bulk-download',
      { attachmentIds }
    );
    window.open(data.url, '_blank');
  }
};
