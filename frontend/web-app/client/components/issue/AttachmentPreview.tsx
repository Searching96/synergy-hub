/**
 * AttachmentPreview Component
 * Full-featured preview for attachments with support for images, PDFs, videos, audio
 * Includes zoom, rotation, navigation, and download controls
 */

import { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import {
  Download,
  X,
  ZoomIn,
  ZoomOut,
  RotateCw,
  ChevronLeft,
  ChevronRight,
  Maximize2,
  Play,
  Pause,
  Volume2,
  VolumeX,
  FileText,
  File,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { Attachment } from "@/types/attachment.types";
import { Slider } from "@/components/ui/slider";

interface AttachmentPreviewProps {
  attachment: Attachment | null;
  attachments?: Attachment[]; // For navigation between multiple attachments
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onDownload?: (attachment: Attachment) => void;
  onDelete?: (attachment: Attachment) => void;
}

export function AttachmentPreview({
  attachment,
  attachments = [],
  open,
  onOpenChange,
  onDownload,
  onDelete,
}: AttachmentPreviewProps) {
  const [zoom, setZoom] = useState(100);
  const [rotation, setRotation] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [volume, setVolume] = useState(50);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  const currentIndex = attachments.findIndex((a) => a.id === attachment?.id);
  const hasPrevious = currentIndex > 0;
  const hasNext = currentIndex < attachments.length - 1;

  useEffect(() => {
    if (!open) {
      // Reset state when dialog closes
      setZoom(100);
      setRotation(0);
      setCurrentPage(1);
    }
  }, [open]);

  if (!attachment) return null;

  const fileType = attachment.fileType.toLowerCase();
  const isImage = fileType.startsWith("image/");
  const isPDF = fileType.includes("pdf");
  const isVideo = fileType.startsWith("video/");
  const isAudio = fileType.startsWith("audio/");
  const isText = fileType.startsWith("text/");
  const isPreviewable = isImage || isPDF || isVideo || isAudio || isText;

  const handleZoomIn = () => setZoom((prev) => Math.min(prev + 25, 400));
  const handleZoomOut = () => setZoom((prev) => Math.max(prev - 25, 25));
  const handleRotate = () => setRotation((prev) => (prev + 90) % 360);
  const handleResetView = () => {
    setZoom(100);
    setRotation(0);
  };

  const handlePrevious = () => {
    if (hasPrevious && attachments[currentIndex - 1]) {
      onOpenChange(false);
      setTimeout(() => {
        onOpenChange(true);
      }, 50);
    }
  };

  const handleNext = () => {
    if (hasNext && attachments[currentIndex + 1]) {
      onOpenChange(false);
      setTimeout(() => {
        onOpenChange(true);
      }, 50);
    }
  };

  const handleDownload = () => {
    if (onDownload) {
      onDownload(attachment);
    } else {
      // Default download
      const link = document.createElement("a");
      link.href = attachment.fileUrl;
      link.download = attachment.fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-7xl h-[90vh] p-0 gap-0 flex flex-col">
        {/* Header */}
        <DialogHeader className="px-6 py-4 border-b flex-shrink-0">
          <div className="flex items-center justify-between">
            <div className="flex-1 min-w-0">
              <DialogTitle className="truncate text-lg">
                {attachment.fileName}
              </DialogTitle>
              <p className="text-sm text-muted-foreground mt-1">
                {formatFileSize(attachment.fileSize)} • Uploaded by{" "}
                {attachment.uploadedBy.name} •{" "}
                {new Date(attachment.uploadedAt).toLocaleDateString()}
              </p>
            </div>
            <div className="flex items-center gap-2 ml-4">
              {/* Navigation */}
              {attachments.length > 1 && (
                <div className="flex items-center gap-1 mr-2 border-r pr-2">
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={handlePrevious}
                    disabled={!hasPrevious}
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </Button>
                  <span className="text-sm text-muted-foreground px-2">
                    {currentIndex + 1} / {attachments.length}
                  </span>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={handleNext}
                    disabled={!hasNext}
                  >
                    <ChevronRight className="h-4 w-4" />
                  </Button>
                </div>
              )}

              {/* Image Controls */}
              {isImage && (
                <>
                  <Button size="sm" variant="ghost" onClick={handleZoomOut}>
                    <ZoomOut className="h-4 w-4" />
                  </Button>
                  <span className="text-sm text-muted-foreground min-w-[4ch] text-center">
                    {zoom}%
                  </span>
                  <Button size="sm" variant="ghost" onClick={handleZoomIn}>
                    <ZoomIn className="h-4 w-4" />
                  </Button>
                  <Button size="sm" variant="ghost" onClick={handleRotate}>
                    <RotateCw className="h-4 w-4" />
                  </Button>
                  <Button size="sm" variant="ghost" onClick={handleResetView}>
                    <Maximize2 className="h-4 w-4" />
                  </Button>
                </>
              )}

              {/* PDF Controls */}
              {isPDF && (
                <div className="flex items-center gap-2">
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                    disabled={currentPage <= 1}
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </Button>
                  <span className="text-sm text-muted-foreground">
                    Page {currentPage} / {totalPages}
                  </span>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() =>
                      setCurrentPage((p) => Math.min(totalPages, p + 1))
                    }
                    disabled={currentPage >= totalPages}
                  >
                    <ChevronRight className="h-4 w-4" />
                  </Button>
                </div>
              )}

              <div className="border-l pl-2 flex items-center gap-2">
                <Button size="sm" variant="outline" onClick={handleDownload}>
                  <Download className="h-4 w-4 mr-1" />
                  Download
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => onOpenChange(false)}
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>
        </DialogHeader>

        {/* Content */}
        <div className="flex-1 overflow-auto bg-gray-50 flex items-center justify-center p-4">
          {/* Image Preview */}
          {isImage && (
            <div className="relative">
              <img
                src={attachment.fileUrl}
                alt={attachment.fileName}
                className="max-w-full max-h-full object-contain transition-transform"
                style={{
                  transform: `scale(${zoom / 100}) rotate(${rotation}deg)`,
                }}
              />
            </div>
          )}

          {/* PDF Preview */}
          {isPDF && (
            <iframe
              src={`${attachment.fileUrl}#page=${currentPage}`}
              className="w-full h-full rounded border bg-white"
              title={attachment.fileName}
            />
          )}

          {/* Video Preview */}
          {isVideo && (
            <div className="w-full max-w-4xl">
              <video
                src={attachment.fileUrl}
                controls
                className="w-full rounded"
                onPlay={() => setIsPlaying(true)}
                onPause={() => setIsPlaying(false)}
              >
                Your browser does not support the video tag.
              </video>
            </div>
          )}

          {/* Audio Preview */}
          {isAudio && (
            <div className="w-full max-w-2xl bg-white rounded-lg shadow-lg p-6">
              <div className="flex items-center gap-4 mb-4">
                <div className="h-16 w-16 rounded bg-blue-100 flex items-center justify-center flex-shrink-0">
                  <Volume2 className="h-8 w-8 text-blue-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="font-medium truncate">
                    {attachment.fileName}
                  </h3>
                  <p className="text-sm text-muted-foreground">
                    {formatFileSize(attachment.fileSize)}
                  </p>
                </div>
              </div>
              <audio
                src={attachment.fileUrl}
                controls
                className="w-full"
                onPlay={() => setIsPlaying(true)}
                onPause={() => setIsPlaying(false)}
              >
                Your browser does not support the audio element.
              </audio>
            </div>
          )}

          {/* Text Preview */}
          {isText && (
            <div className="w-full max-w-4xl bg-white rounded-lg shadow p-6">
              <iframe
                src={attachment.fileUrl}
                className="w-full h-[600px] border-0"
                title={attachment.fileName}
              />
            </div>
          )}

          {/* Unsupported File Type */}
          {!isPreviewable && (
            <div className="text-center">
              <div className="h-24 w-24 rounded-full bg-gray-200 flex items-center justify-center mx-auto mb-4">
                <File className="h-12 w-12 text-gray-400" />
              </div>
              <h3 className="text-lg font-medium mb-2">
                Preview not available
              </h3>
              <p className="text-sm text-muted-foreground mb-4">
                This file type cannot be previewed in the browser
              </p>
              <Button onClick={handleDownload}>
                <Download className="h-4 w-4 mr-2" />
                Download to view
              </Button>
            </div>
          )}
        </div>

        {/* Footer Info */}
        <div className="px-6 py-3 border-t bg-white flex-shrink-0">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span>
              Type: {attachment.fileType} • Size:{" "}
              {formatFileSize(attachment.fileSize)}
            </span>
            {isImage && (
              <span>
                Use mouse wheel to zoom • Click and drag to pan
              </span>
            )}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}