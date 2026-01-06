/**
 * IssueAttachmentsSection Component
 * Displays attachments section in issue detail modal
 */

import { useState } from "react";
import { Paperclip } from "lucide-react";
import { Separator } from "@/components/ui/separator";
import { AttachmentUpload } from "@/components/issue/AttachmentUpload";
import { AttachmentList } from "@/components/issue/AttachmentList";
import { toast } from "sonner";
import type { Attachment } from "@/types/attachment.types";
import {
  mockUploadFile,
  mockDeleteFile,
  mockDownloadFile,
} from "@/lib/mockAttachments";

interface IssueAttachmentsSectionProps {
  taskId: number;
  attachments: Attachment[];
  isReadOnly?: boolean;
  onAttachmentsUpdate?: () => void;
}

export function IssueAttachmentsSection({
  taskId,
  attachments: initialAttachments,
  isReadOnly = false,
  onAttachmentsUpdate,
}: IssueAttachmentsSectionProps) {
  const [attachments, setAttachments] = useState<Attachment[]>(initialAttachments);
  const [showUpload, setShowUpload] = useState(false);

  const handleUpload = async (file: File) => {
    try {
      const newAttachment = await mockUploadFile(taskId, file);
      setAttachments((prev) => [...prev, newAttachment]);
      setShowUpload(false);
      
      if (onAttachmentsUpdate) {
        onAttachmentsUpdate();
      }
    } catch (error: any) {
      throw new Error(error.message || "Failed to upload file");
    }
  };

  const handleDelete = async (attachmentId: number) => {
    try {
      await mockDeleteFile(taskId, attachmentId);
      setAttachments((prev) => prev.filter((att) => att.id !== attachmentId));
      toast.success("Attachment deleted");
      
      if (onAttachmentsUpdate) {
        onAttachmentsUpdate();
      }
    } catch (error: any) {
      toast.error(error.message || "Failed to delete attachment");
    }
  };

  const handleDownload = (attachment: Attachment) => {
    mockDownloadFile(attachment);
    toast.success(`Downloading ${attachment.fileName}`);
  };

  const handlePreview = (attachment: Attachment) => {
    // Open in new tab for preview
    window.open(attachment.fileUrl, "_blank");
  };

  return (
    <div className="space-y-4">
      <Separator />
      
      <div>
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Paperclip className="h-4 w-4" />
            <h3 className="text-sm font-semibold">
              Attachments ({attachments.length})
            </h3>
          </div>
          {!isReadOnly && !showUpload && (
            <button
              onClick={() => setShowUpload(true)}
              className="text-sm text-blue-600 hover:text-blue-700 font-medium"
            >
              Add attachment
            </button>
          )}
        </div>

        {/* Upload Section */}
        {showUpload && !isReadOnly && (
          <div className="mb-4">
            <AttachmentUpload taskId={taskId} onUpload={handleUpload} />
            <button
              onClick={() => setShowUpload(false)}
              className="text-sm text-gray-600 hover:text-gray-700 mt-2"
            >
              Cancel
            </button>
          </div>
        )}

        {/* Attachments List */}
        <AttachmentList
          attachments={attachments}
          onDelete={!isReadOnly ? handleDelete : undefined}
          onDownload={handleDownload}
          onPreview={handlePreview}
          isReadOnly={isReadOnly}
        />
      </div>
    </div>
  );
}
