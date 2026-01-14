/**
 * AttachmentList Component - Production Ready
 * Displays list of attachments with preview, download, and bulk operations
 */

import { useState } from "react";
import { 
  Download, 
  Eye, 
  File, 
  Trash2, 
  Image as ImageIcon,
  FileText,
  X,
  Loader2,
  ExternalLink,
  Download as DownloadIcon,
  CheckSquare,
  Square
} from "lucide-react";
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
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Checkbox } from "@/components/ui/checkbox";

interface AttachmentListProps {
  attachments: Attachment[];
  onDelete?: (attachmentId: number) => Promise<void>;
  onDownload?: (attachment: Attachment) => Promise<void>;
  onPreview?: (attachment: Attachment) => void;
  onBulkDownload?: (attachmentIds: number[]) => Promise<void>;
  onBulkDelete?: (attachmentIds: number[]) => Promise<void>;
  isReadOnly?: boolean;
  isLoading?: boolean;
}

const FILE_ICONS: Record<string, any> = {
  image: ImageIcon,
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
  onBulkDownload,
  onBulkDelete,
  isReadOnly = false,
  isLoading = false,
}: AttachmentListProps) {
  const [previewAttachment, setPreviewAttachment] = useState<Attachment | null>(null);
  const [deletingIds, setDeletingIds] = useState<Set<number>>(new Set());
  const [downloadingIds, setDownloadingIds] = useState<Set<number>>(new Set());
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [bulkOperating, setBulkOperating] = useState(false);

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
  const isPDF = (fileType: string) => fileType.includes("pdf");

  const handleDownload = async (attachment: Attachment) => {
    setDownloadingIds(prev => new Set(prev).add(attachment.id));
    
    try {
      if (onDownload) {
        await onDownload(attachment);
      } else {
        // Default download behavior with error handling
        const response = await fetch(attachment.fileUrl);
        if (!response.ok) throw new Error('Download failed');
        
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = attachment.fileName;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      }
    } catch (error) {
      console.error('Download failed:', error);
      alert('Failed to download file. Please try again.');
    } finally {
      setDownloadingIds(prev => {
        const next = new Set(prev);
        next.delete(attachment.id);
        return next;
      });
    }
  };

  const handleDelete = async (attachmentId: number) => {
    if (!onDelete) return;
    
    setDeletingIds(prev => new Set(prev).add(attachmentId));
    
    try {
      await onDelete(attachmentId);
      setSelectedIds(prev => {
        const next = new Set(prev);
        next.delete(attachmentId);
        return next;
      });
    } catch (error) {
      console.error('Delete failed:', error);
      alert('Failed to delete attachment. Please try again.');
    } finally {
      setDeletingIds(prev => {
        const next = new Set(prev);
        next.delete(attachmentId);
        return next;
      });
    }
  };

  const handlePreview = (attachment: Attachment) => {
    if (isImage(attachment.fileType) || isPDF(attachment.fileType)) {
      setPreviewAttachment(attachment);
    } else if (onPreview) {
      onPreview(attachment);
    }
  };

  const toggleSelection = (id: number) => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const toggleSelectAll = () => {
    if (selectedIds.size === attachments.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(attachments.map(a => a.id)));
    }
  };

  const handleBulkDownload = async () => {
    if (!onBulkDownload || selectedIds.size === 0) return;
    
    setBulkOperating(true);
    try {
      await onBulkDownload(Array.from(selectedIds));
      setSelectedIds(new Set());
    } catch (error) {
      console.error('Bulk download failed:', error);
      alert('Failed to download selected files. Please try again.');
    } finally {
      setBulkOperating(false);
    }
  };

  const handleBulkDelete = async () => {
    if (!onBulkDelete || selectedIds.size === 0) return;
    
    setBulkOperating(true);
    try {
      await onBulkDelete(Array.from(selectedIds));
      setSelectedIds(new Set());
    } catch (error) {
      console.error('Bulk delete failed:', error);
      alert('Failed to delete selected files. Please try again.');
    } finally {
      setBulkOperating(false);
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    
    return date.toLocaleDateString();
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (attachments.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground text-sm">
        No attachments yet
      </div>
    );
  }

  const showBulkActions = selectedIds.size > 0;
  const allSelected = selectedIds.size === attachments.length && attachments.length > 0;

  return (
    <div className="space-y-3">
      {/* Bulk Actions Bar */}
      {(onBulkDownload || onBulkDelete) && attachments.length > 1 && (
        <div className={cn(
          "flex items-center justify-between p-3 rounded-lg border transition-all",
          showBulkActions ? "bg-blue-50 border-blue-200" : "bg-muted/50"
        )}>
          <div className="flex items-center gap-2">
            <Checkbox
              checked={allSelected}
              onCheckedChange={toggleSelectAll}
              className="data-[state=checked]:bg-blue-600"
            />
            <span className="text-sm font-medium">
              {showBulkActions 
                ? `${selectedIds.size} selected` 
                : 'Select files'}
            </span>
          </div>
          
          {showBulkActions && (
            <div className="flex items-center gap-2">
              {onBulkDownload && (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={handleBulkDownload}
                  disabled={bulkOperating}
                >
                  {bulkOperating ? (
                    <Loader2 className="h-4 w-4 mr-1 animate-spin" />
                  ) : (
                    <DownloadIcon className="h-4 w-4 mr-1" />
                  )}
                  Download
                </Button>
              )}
              {onBulkDelete && !isReadOnly && (
                <AlertDialog>
                  <AlertDialogTrigger asChild>
                    <Button
                      size="sm"
                      variant="outline"
                      className="text-red-600 hover:text-red-700 hover:bg-red-50"
                      disabled={bulkOperating}
                    >
                      <Trash2 className="h-4 w-4 mr-1" />
                      Delete
                    </Button>
                  </AlertDialogTrigger>
                  <AlertDialogContent>
                    <AlertDialogHeader>
                      <AlertDialogTitle>Delete {selectedIds.size} Attachments</AlertDialogTitle>
                      <AlertDialogDescription>
                        Are you sure you want to delete {selectedIds.size} selected attachment(s)? 
                        This action cannot be undone.
                      </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                      <AlertDialogCancel>Cancel</AlertDialogCancel>
                      <AlertDialogAction
                        onClick={handleBulkDelete}
                        className="bg-red-600 hover:bg-red-700"
                      >
                        Delete All
                      </AlertDialogAction>
                    </AlertDialogFooter>
                  </AlertDialogContent>
                </AlertDialog>
              )}
            </div>
          )}
        </div>
      )}

      {/* Attachments List */}
      <div className="space-y-2">
        {attachments.map((attachment) => {
          const FileIcon = getFileIcon(attachment.fileType);
          const showPreview = isImage(attachment.fileType) || isPDF(attachment.fileType);
          const isDeleting = deletingIds.has(attachment.id);
          const isDownloading = downloadingIds.has(attachment.id);
          const isSelected = selectedIds.has(attachment.id);

          return (
            <div
              key={attachment.id}
              className={cn(
                "flex items-center gap-3 p-3 rounded-lg border bg-card transition-all",
                isSelected && "ring-2 ring-blue-500 bg-blue-50",
                !isDeleting && "hover:bg-accent/50"
              )}
            >
              {/* Selection Checkbox */}
              {(onBulkDownload || onBulkDelete) && attachments.length > 1 && (
                <Checkbox
                  checked={isSelected}
                  onCheckedChange={() => toggleSelection(attachment.id)}
                  className="data-[state=checked]:bg-blue-600"
                />
              )}

              {/* File Icon or Thumbnail */}
              <div className="flex-shrink-0">
                {showPreview && attachment.thumbnailUrl ? (
                  <button
                    onClick={() => handlePreview(attachment)}
                    className="group relative"
                  >
                    <img
                      src={attachment.thumbnailUrl}
                      alt={attachment.fileName}
                      className="h-12 w-12 rounded object-cover"
                    />
                    <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 rounded transition-colors flex items-center justify-center">
                      <Eye className="h-4 w-4 text-white opacity-0 group-hover:opacity-100 transition-opacity" />
                    </div>
                  </button>
                ) : (
                  <div className="h-12 w-12 rounded bg-muted flex items-center justify-center">
                    <FileIcon className="h-6 w-6 text-muted-foreground" />
                  </div>
                )}
              </div>

              {/* File Info */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handlePreview(attachment)}
                    className="text-sm font-medium truncate hover:text-blue-600 transition-colors text-left"
                  >
                    {attachment.fileName}
                  </button>
                </div>
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <span>{formatFileSize(attachment.fileSize)}</span>
                  <span>•</span>
                  <span>{attachment.uploadedBy.name}</span>
                  <span>•</span>
                  <span>{formatDate(attachment.uploadedAt)}</span>
                </div>
              </div>

              {/* Actions */}
              <div className="flex items-center gap-1">
                {/* Preview Button */}
                {showPreview && (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => handlePreview(attachment)}
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
                  disabled={isDownloading}
                >
                  {isDownloading ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Download className="h-4 w-4" />
                  )}
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
                        disabled={isDeleting}
                      >
                        {isDeleting ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                          <Trash2 className="h-4 w-4" />
                        )}
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
                          onClick={() => handleDelete(attachment.id)}
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

      {/* Preview Dialog */}
      <Dialog open={!!previewAttachment} onOpenChange={() => setPreviewAttachment(null)}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-hidden flex flex-col">
          <DialogHeader>
            <DialogTitle className="flex items-center justify-between">
              <span className="truncate">{previewAttachment?.fileName}</span>
              <div className="flex items-center gap-2 ml-4">
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => previewAttachment && handleDownload(previewAttachment)}
                >
                  <Download className="h-4 w-4 mr-1" />
                  Download
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => setPreviewAttachment(null)}
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
            </DialogTitle>
          </DialogHeader>
          <div className="flex-1 overflow-auto">
            {previewAttachment && isImage(previewAttachment.fileType) && (
              <img
                src={previewAttachment.fileUrl}
                alt={previewAttachment.fileName}
                className="w-full h-auto"
              />
            )}
            {previewAttachment && isPDF(previewAttachment.fileType) && (
              <iframe
                src={previewAttachment.fileUrl}
                className="w-full h-full min-h-[600px]"
                title={previewAttachment.fileName}
              />
            )}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}