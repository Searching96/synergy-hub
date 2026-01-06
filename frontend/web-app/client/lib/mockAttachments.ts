/**
 * Mock Attachment Data and Utilities
 * Simulates file attachment functionality until backend S3 integration is complete
 */

import type { Attachment } from "@/types/attachment.types";
import type { Task } from "@/types/task.types";

/**
 * Mock attachments database
 */
export const mockAttachments: Attachment[] = [
  {
    id: 1,
    taskId: 1001,
    fileName: "user-flow-diagram.png",
    fileSize: 245678,
    fileType: "image/png",
    fileUrl: "https://picsum.photos/800/600",
    thumbnailUrl: "https://picsum.photos/200/150",
    uploadedBy: { id: 1, name: "John Doe" },
    uploadedAt: "2026-01-05T10:30:00Z",
  },
  {
    id: 2,
    taskId: 1001,
    fileName: "requirements-document.pdf",
    fileSize: 512000,
    fileType: "application/pdf",
    fileUrl: "/mock-files/requirements.pdf",
    uploadedBy: { id: 2, name: "Jane Smith" },
    uploadedAt: "2026-01-04T14:20:00Z",
  },
  {
    id: 3,
    taskId: 1002,
    fileName: "design-mockup.fig",
    fileSize: 1048576,
    fileType: "application/octet-stream",
    fileUrl: "/mock-files/design-mockup.fig",
    uploadedBy: { id: 3, name: "Bob Designer" },
    uploadedAt: "2026-01-03T09:15:00Z",
  },
  {
    id: 4,
    taskId: 1003,
    fileName: "bug-screenshot.jpg",
    fileSize: 156789,
    fileType: "image/jpeg",
    fileUrl: "https://picsum.photos/800/600?random=1",
    thumbnailUrl: "https://picsum.photos/200/150?random=1",
    uploadedBy: { id: 1, name: "John Doe" },
    uploadedAt: "2026-01-02T16:45:00Z",
  },
];

/**
 * In-memory storage for mock uploaded files
 */
let attachmentIdCounter = 1000;
const mockAttachmentsStorage = new Map<number, Attachment[]>();

// Initialize with mock data
mockAttachments.forEach((attachment) => {
  const taskAttachments = mockAttachmentsStorage.get(attachment.taskId) || [];
  taskAttachments.push(attachment);
  mockAttachmentsStorage.set(attachment.taskId, taskAttachments);
});

/**
 * Get attachments for a specific task
 */
export function getTaskAttachments(taskId: number): Attachment[] {
  return mockAttachmentsStorage.get(taskId) || [];
}

/**
 * Mock file upload - simulates uploading to S3
 */
export async function mockUploadFile(
  taskId: number,
  file: File,
  userId: number = 1,
  userName: string = "Current User"
): Promise<Attachment> {
  // Simulate upload delay
  await new Promise((resolve) => setTimeout(resolve, 1500));

  // Create mock URL (in real app, this would be S3 URL)
  const fileUrl = URL.createObjectURL(file);
  
  // Create thumbnail for images
  let thumbnailUrl: string | undefined;
  if (file.type.startsWith("image/")) {
    thumbnailUrl = fileUrl; // In real app, would generate actual thumbnail
  }

  const newAttachment: Attachment = {
    id: ++attachmentIdCounter,
    taskId,
    fileName: file.name,
    fileSize: file.size,
    fileType: file.type,
    fileUrl,
    thumbnailUrl,
    uploadedBy: { id: userId, name: userName },
    uploadedAt: new Date().toISOString(),
  };

  // Store in mock database
  const taskAttachments = mockAttachmentsStorage.get(taskId) || [];
  taskAttachments.push(newAttachment);
  mockAttachmentsStorage.set(taskId, taskAttachments);

  return newAttachment;
}

/**
 * Mock file deletion
 */
export async function mockDeleteFile(
  taskId: number,
  attachmentId: number
): Promise<void> {
  // Simulate deletion delay
  await new Promise((resolve) => setTimeout(resolve, 500));

  const taskAttachments = mockAttachmentsStorage.get(taskId);
  if (taskAttachments) {
    const filtered = taskAttachments.filter((att) => att.id !== attachmentId);
    mockAttachmentsStorage.set(taskId, filtered);
  }
}

/**
 * Mock file download - triggers browser download
 */
export function mockDownloadFile(attachment: Attachment): void {
  const link = document.createElement("a");
  link.href = attachment.fileUrl;
  link.download = attachment.fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

/**
 * Enrich task with attachments
 */
export function enrichTaskWithAttachments(task: Task): Task {
  const attachments = getTaskAttachments(task.id);
  return {
    ...task,
    attachments,
  };
}

/**
 * Get file icon based on file type
 */
export function getFileIconUrl(fileType: string): string {
  if (fileType.startsWith("image/")) return "/icons/image-icon.svg";
  if (fileType.includes("pdf")) return "/icons/pdf-icon.svg";
  if (fileType.includes("word") || fileType.includes("document"))
    return "/icons/doc-icon.svg";
  if (fileType.includes("excel") || fileType.includes("spreadsheet"))
    return "/icons/excel-icon.svg";
  if (fileType.startsWith("text/")) return "/icons/text-icon.svg";
  return "/icons/file-icon.svg";
}

/**
 * Validate file before upload
 */
export function validateFileUpload(
  file: File,
  maxSizeMB: number = 10
): { valid: boolean; error?: string } {
  // Check file size
  const maxSizeBytes = maxSizeMB * 1024 * 1024;
  if (file.size > maxSizeBytes) {
    return {
      valid: false,
      error: `File size must be less than ${maxSizeMB}MB`,
    };
  }

  // Check if file has extension
  if (!file.name.includes(".")) {
    return {
      valid: false,
      error: "File must have an extension",
    };
  }

  return { valid: true };
}

/**
 * Format file size for display
 */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024)
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`;
}

/**
 * Check if file type is supported
 */
export function isSupportedFileType(fileType: string): boolean {
  const supportedTypes = [
    "image/",
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument",
    "text/",
    "application/zip",
    "application/x-rar",
  ];

  return supportedTypes.some((type) => fileType.startsWith(type));
}

/**
 * Get attachment statistics for a task
 */
export function getAttachmentStats(taskId: number) {
  const attachments = getTaskAttachments(taskId);
  const totalSize = attachments.reduce((sum, att) => sum + att.fileSize, 0);
  const imageCount = attachments.filter((att) =>
    att.fileType.startsWith("image/")
  ).length;
  const documentCount = attachments.filter(
    (att) =>
      att.fileType.includes("pdf") ||
      att.fileType.includes("word") ||
      att.fileType.includes("document")
  ).length;

  return {
    count: attachments.length,
    totalSize,
    imageCount,
    documentCount,
  };
}
