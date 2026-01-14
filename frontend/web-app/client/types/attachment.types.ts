/**
 * Attachment Type Definitions
 * For file attachments on tasks/issues
 */

export interface Attachment {
  id: number;
  taskId: number;
  fileName: string;
  fileSize: number; // in bytes
  fileType: string; // MIME type
  fileUrl: string;
  uploadedBy: {
    id: number;
    name: string;
  };
  uploadedAt: string;
  thumbnailUrl?: string; // for images
}

export interface UploadAttachmentRequest {
  taskId: number;
  file: File;
}

export interface AttachmentResponse {
  success: boolean;
  message: string;
  data: Attachment;
}

export interface DeleteAttachmentRequest {
  taskId: number;
  attachmentId: number;
}
