/**
 * IssueAttachmentsSection Component - Reusable Version
 * Generic attachments section that can be used for issues, comments, documents, etc.
 */

import { useState, useEffect } from "react";
import { Paperclip, Grid3x3, List } from "lucide-react";
import { Separator } from "@/components/ui/separator";
import { AttachmentUpload } from "@/components/issue/AttachmentUpload";
import { AttachmentList } from "@/components/issue/AttachmentList";
import { AttachmentGallery } from "@/components/issue/AttachmentGallery";
import { AttachmentPreview } from "@/components/issue/AttachmentPreview";
import { toast } from "sonner";
import type { Attachment } from "@/types/attachment.types";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface IssueAttachmentsSectionProps {
  // Core props
  entityId: number; // Generic: taskId, commentId, documentId, etc.
  entityType?: "task" | "comment" | "document" | "project"; // For API routing
  attachments?: Attachment[]; // Optional: can be controlled or uncontrolled
  
  // Service functions (dependency injection for flexibility)
  onUpload: (entityId: number, file: File, onProgress?: (progress: number) => void) => Promise<Attachment>;
  onDelete?: (attachmentId: number) => Promise<void>;
  onDownload?: (attachment: Attachment) => Promise<void>;
  onBulkDownload?: (attachmentIds: number[]) => Promise<void>;
  onBulkDelete?: (attachmentIds: number[]) => Promise<void>;
  
  // Callbacks
  onAttachmentsChange?: (attachments: Attachment[]) => void; // More generic than onAttachmentsUpdate
  
  // UI customization
  isReadOnly?: boolean;
  title?: string;
  showTitle?: boolean;
  showSeparator?: boolean;
  showAddButton?: boolean;
  autoExpandUpload?: boolean; // Auto-show upload area
  maxFileSize?: number;
  maxFiles?: number;
  acceptedTypes?: string[];
  
  // View options
  defaultView?: "list" | "gallery";
  allowViewToggle?: boolean; // Allow users to switch between list/gallery
  galleryColumns?: 2 | 3 | 4 | 5;
  
  // Feature flags
  enablePreview?: boolean;
  enableBulkOperations?: boolean;
  
  // Styling
  className?: string;
  compact?: boolean; // Minimal spacing for inline use
}

type ViewMode = "list" | "gallery";

export function IssueAttachmentsSection({
  entityId,
  entityType = "task",
  attachments: controlledAttachments,
  onUpload,
  onDelete,
  onDownload,
  onBulkDownload,
  onBulkDelete,
  onAttachmentsChange,
  isReadOnly = false,
  title = "Attachments",
  showTitle = true,
  showSeparator = true,
  showAddButton = true,
  autoExpandUpload = false,
  maxFileSize = 10,
  maxFiles = 5,
  acceptedTypes,
  defaultView = "list",
  allowViewToggle = true,
  galleryColumns = 3,
  enablePreview = true,
  enableBulkOperations = true,
  className,
  compact = false,
}: IssueAttachmentsSectionProps) {
  // Support both controlled and uncontrolled modes
  const [internalAttachments, setInternalAttachments] = useState<Attachment[]>(
    controlledAttachments || []
  );
  const [showUpload, setShowUpload] = useState(autoExpandUpload);
  const [viewMode, setViewMode] = useState<ViewMode>(defaultView);
  const [previewAttachment, setPreviewAttachment] = useState<Attachment | null>(null);

  // Sync with controlled attachments if provided
  useEffect(() => {
    if (controlledAttachments) {
      setInternalAttachments(controlledAttachments);
    }
  }, [controlledAttachments]);

  const attachments = controlledAttachments || internalAttachments;
  const isControlled = controlledAttachments !== undefined;

  const updateAttachments = (newAttachments: Attachment[]) => {
    if (!isControlled) {
      setInternalAttachments(newAttachments);
    }
    onAttachmentsChange?.(newAttachments);
  };

  const handleUpload = async (file: File, onProgress?: (progress: number) => void) => {
    try {
      const newAttachment = await onUpload(entityId, file, onProgress);
      const updated = [...attachments, newAttachment];
      updateAttachments(updated);
      setShowUpload(false);
    } catch (error: any) {
      throw new Error(error.message || "Failed to upload file");
    }
  };

  const handleDelete = async (attachmentId: number) => {
    if (!onDelete) return;

    try {
      await onDelete(attachmentId);
      const updated = attachments.filter((att) => att.id !== attachmentId);
      updateAttachments(updated);
      toast.success("Attachment deleted");
    } catch (error: any) {
      toast.error(error.message || "Failed to delete attachment");
      throw error;
    }
  };

  const handleDownload = async (attachment: Attachment) => {
    if (onDownload) {
      try {
        await onDownload(attachment);
        toast.success(`Downloading ${attachment.fileName}`);
      } catch (error: any) {
        toast.error("Failed to download file");
      }
    } else {
      // Default download behavior
      const link = document.createElement("a");
      link.href = attachment.fileUrl;
      link.download = attachment.fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      toast.success(`Downloading ${attachment.fileName}`);
    }
  };

  const handleBulkDownload = async (attachmentIds: number[]) => {
    if (onBulkDownload) {
      try {
        await onBulkDownload(attachmentIds);
        toast.success(`Downloading ${attachmentIds.length} files`);
      } catch (error: any) {
        toast.error("Failed to download files");
        throw error;
      }
    }
  };

  const handleBulkDelete = async (attachmentIds: number[]) => {
    if (onBulkDelete) {
      try {
        await onBulkDelete(attachmentIds);
        const updated = attachments.filter((att) => !attachmentIds.includes(att.id));
        updateAttachments(updated);
        toast.success(`Deleted ${attachmentIds.length} attachments`);
      } catch (error: any) {
        toast.error("Failed to delete attachments");
        throw error;
      }
    }
  };

  const handlePreview = (attachment: Attachment) => {
    if (enablePreview) {
      setPreviewAttachment(attachment);
    } else {
      // Fallback: open in new tab
      window.open(attachment.fileUrl, "_blank");
    }
  };

  const imageAttachments = attachments.filter((a) =>
    a.fileType.startsWith("image/")
  );
  const hasImages = imageAttachments.length > 0;

  return (
    <div className={cn("space-y-4", compact && "space-y-2", className)}>
      {showSeparator && <Separator />}

      <div>
        {showTitle && (
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Paperclip className="h-4 w-4" />
              <h3 className={cn("font-semibold", compact ? "text-sm" : "text-sm")}>
                {title} ({attachments.length})
              </h3>
            </div>
            
            <div className="flex items-center gap-2">
              {/* View Toggle */}
              {allowViewToggle && hasImages && attachments.length > 0 && (
                <div className="flex items-center gap-1 border rounded-md p-0.5">
                  <Button
                    size="sm"
                    variant={viewMode === "list" ? "secondary" : "ghost"}
                    className="h-7 px-2"
                    onClick={() => setViewMode("list")}
                  >
                    <List className="h-3.5 w-3.5" />
                  </Button>
                  <Button
                    size="sm"
                    variant={viewMode === "gallery" ? "secondary" : "ghost"}
                    className="h-7 px-2"
                    onClick={() => setViewMode("gallery")}
                  >
                    <Grid3x3 className="h-3.5 w-3.5" />
                  </Button>
                </div>
              )}

              {/* Add Attachment Button */}
              {!isReadOnly && showAddButton && !showUpload && (
                <button
                  onClick={() => setShowUpload(true)}
                  className="text-sm text-blue-600 hover:text-blue-700 font-medium"
                >
                  Add attachment
                </button>
              )}
            </div>
          </div>
        )}

        {/* Upload Section */}
        {showUpload && !isReadOnly && (
          <div className={cn("mb-4", compact && "mb-2")}>
            <AttachmentUpload
              taskId={entityId}
              onUpload={handleUpload}
              maxFileSize={maxFileSize}
              maxFiles={maxFiles}
              acceptedTypes={acceptedTypes}
            />
            <button
              onClick={() => setShowUpload(false)}
              className="text-sm text-gray-600 hover:text-gray-700 mt-2"
            >
              Cancel
            </button>
          </div>
        )}

        {/* Attachments Display */}
        {viewMode === "gallery" && hasImages ? (
          <AttachmentGallery
            attachments={imageAttachments}
            onDownload={handleDownload}
            columns={galleryColumns}
          />
        ) : (
          <AttachmentList
            attachments={attachments}
            onDelete={!isReadOnly && onDelete ? handleDelete : undefined}
            onDownload={handleDownload}
            onPreview={enablePreview ? handlePreview : undefined}
            onBulkDownload={enableBulkOperations && onBulkDownload ? handleBulkDownload : undefined}
            onBulkDelete={enableBulkOperations && onBulkDelete && !isReadOnly ? handleBulkDelete : undefined}
            isReadOnly={isReadOnly}
          />
        )}
      </div>

      {/* Preview Modal */}
      {enablePreview && (
        <AttachmentPreview
          attachment={previewAttachment}
          attachments={attachments}
          open={!!previewAttachment}
          onOpenChange={(open) => !open && setPreviewAttachment(null)}
          onDownload={handleDownload}
        />
      )}
    </div>
  );
}