/**
 * AttachmentList Component
 * Displays list of attachments with preview and download options
 */

import { Download, ExternalLink, FileText, Image, File, Trash2, Eye } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { Attachment } from "@/types/attachment.types";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";

interface AttachmentListProps {
  attachments: Attachment[];
  onDelete?: (attachmentId: number) => void;
  onDownload?: (attachment: Attachment) => void;
  onPreview?: (attachment: Attachment) => void;
  isReadOnly?: boolean;
}

const FILE_ICONS: Record<string, any> = {
  image: Image,
  pdf: FileText,
  document: FileText,
  text: FileText,
  default: File,
};

export function AttachmentList({
  attachments,
  onDelete,
  onDownload,
  onPreview,
  isReadOnly = false,
}: AttachmentListProps) {
  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const getFileCategory = (fileType: string): string => {
    if (fileType.startsWith("image/")) return "image";
    if (fileType.includes("pdf")) return "pdf";
    if (
      fileType.includes("word") ||
      fileType.includes("document") ||
      fileType.includes("msword")
    )
      return "document";
    if (fileType.startsWith("text/")) return "text";
    return "default";
  };

  const getFileIcon = (fileType: string) => {
    const category = getFileCategory(fileType);
    return FILE_ICONS[category] || FILE_ICONS.default;
  };

  const isImage = (fileType: string) => fileType.startsWith("image/");

  const handleDownload = (attachment: Attachment) => {
    if (onDownload) {
      onDownload(attachment);
    } else {
      // Default download behavior
      const link = document.createElement("a");
      link.href = attachment.fileUrl;
      link.download = attachment.fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  };

  if (attachments.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground text-sm">
        No attachments yet
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {attachments.map((attachment) => {
        const FileIcon = getFileIcon(attachment.fileType);
        const showPreview = isImage(attachment.fileType);

        return (
          <div
            key={attachment.id}
            className="flex items-center gap-3 p-3 rounded-lg border bg-card hover:bg-accent/50 transition-colors"
          >
            {/* File Icon or Thumbnail */}
            <div className="flex-shrink-0">
              {showPreview && attachment.thumbnailUrl ? (
                <img
                  src={attachment.thumbnailUrl}
                  alt={attachment.fileName}
                  className="h-10 w-10 rounded object-cover"
                />
              ) : (
                <div className="h-10 w-10 rounded bg-muted flex items-center justify-center">
                  <FileIcon className="h-5 w-5 text-muted-foreground" />
                </div>
              )}
            </div>

            {/* File Info */}
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <p className="text-sm font-medium truncate">{attachment.fileName}</p>
              </div>
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <span>{formatFileSize(attachment.fileSize)}</span>
                <span>•</span>
                <span>
                  Uploaded by {attachment.uploadedBy.name}
                </span>
                <span>•</span>
                <span>
                  {new Date(attachment.uploadedAt).toLocaleDateString()}
                </span>
              </div>
            </div>

            {/* Actions */}
            <div className="flex items-center gap-1">
              {/* Preview Button (for images) */}
              {showPreview && onPreview && (
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => onPreview(attachment)}
                  className="h-8 w-8 p-0"
                  title="Preview"
                >
                  <Eye className="h-4 w-4" />
                </Button>
              )}

              {/* Download Button */}
              <Button
                size="sm"
                variant="ghost"
                onClick={() => handleDownload(attachment)}
                className="h-8 w-8 p-0"
                title="Download"
              >
                <Download className="h-4 w-4" />
              </Button>

              {/* Delete Button */}
              {!isReadOnly && onDelete && (
                <AlertDialog>
                  <AlertDialogTrigger asChild>
                    <Button
                      size="sm"
                      variant="ghost"
                      className="h-8 w-8 p-0 text-red-600 hover:text-red-700 hover:bg-red-50"
                      title="Delete"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </AlertDialogTrigger>
                  <AlertDialogContent>
                    <AlertDialogHeader>
                      <AlertDialogTitle>Delete Attachment</AlertDialogTitle>
                      <AlertDialogDescription>
                        Are you sure you want to delete "{attachment.fileName}"? This
                        action cannot be undone.
                      </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                      <AlertDialogCancel>Cancel</AlertDialogCancel>
                      <AlertDialogAction
                        onClick={() => onDelete(attachment.id)}
                        className="bg-red-600 hover:bg-red-700"
                      >
                        Delete
                      </AlertDialogAction>
                    </AlertDialogFooter>
                  </AlertDialogContent>
                </AlertDialog>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
