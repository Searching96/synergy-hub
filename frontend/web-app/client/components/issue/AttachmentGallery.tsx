/**
 * AttachmentGallery Component
 * Grid view for image attachments with lightbox and slideshow functionality
 */

import { useState } from "react";
import {
  Dialog,
  DialogContent,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import {
  ChevronLeft,
  ChevronRight,
  X,
  Download,
  ZoomIn,
  ZoomOut,
  RotateCw,
  Play,
  Pause,
  Grid3x3,
  Image as ImageIcon,
  Maximize2,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { Attachment } from "@/types/attachment.types";

interface AttachmentGalleryProps {
  attachments: Attachment[];
  onDownload?: (attachment: Attachment) => void;
  columns?: 2 | 3 | 4 | 5;
  showControls?: boolean;
  className?: string;
}

export function AttachmentGallery({
  attachments,
  onDownload,
  columns = 3,
  showControls = true,
  className,
}: AttachmentGalleryProps) {
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null);
  const [zoom, setZoom] = useState(100);
  const [rotation, setRotation] = useState(0);
  const [isSlideshow, setIsSlideshow] = useState(false);
  const [slideshowInterval, setSlideshowInterval] = useState<NodeJS.Timeout | null>(null);

  // Filter only images
  const imageAttachments = attachments.filter((a) =>
    a.fileType.startsWith("image/")
  );

  const selectedAttachment =
    selectedIndex !== null ? imageAttachments[selectedIndex] : null;

  const handleOpenLightbox = (index: number) => {
    setSelectedIndex(index);
    setZoom(100);
    setRotation(0);
  };

  const handleCloseLightbox = () => {
    setSelectedIndex(null);
    stopSlideshow();
  };

  const handlePrevious = () => {
    if (selectedIndex !== null && selectedIndex > 0) {
      setSelectedIndex(selectedIndex - 1);
      setZoom(100);
      setRotation(0);
    }
  };

  const handleNext = () => {
    if (selectedIndex !== null && selectedIndex < imageAttachments.length - 1) {
      setSelectedIndex(selectedIndex + 1);
      setZoom(100);
      setRotation(0);
    } else if (isSlideshow && selectedIndex === imageAttachments.length - 1) {
      // Loop back to start in slideshow mode
      setSelectedIndex(0);
      setZoom(100);
      setRotation(0);
    }
  };

  const handleZoomIn = () => setZoom((prev) => Math.min(prev + 25, 400));
  const handleZoomOut = () => setZoom((prev) => Math.max(prev - 25, 25));
  const handleRotate = () => setRotation((prev) => (prev + 90) % 360);
  const handleResetView = () => {
    setZoom(100);
    setRotation(0);
  };

  const startSlideshow = () => {
    setIsSlideshow(true);
    const interval = setInterval(() => {
      handleNext();
    }, 3000); // 3 seconds per image
    setSlideshowInterval(interval);
  };

  const stopSlideshow = () => {
    setIsSlideshow(false);
    if (slideshowInterval) {
      clearInterval(slideshowInterval);
      setSlideshowInterval(null);
    }
  };

  const toggleSlideshow = () => {
    if (isSlideshow) {
      stopSlideshow();
    } else {
      startSlideshow();
    }
  };

  const handleDownloadCurrent = () => {
    if (selectedAttachment && onDownload) {
      onDownload(selectedAttachment);
    } else if (selectedAttachment) {
      const link = document.createElement("a");
      link.href = selectedAttachment.fileUrl;
      link.download = selectedAttachment.fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  };

  const handleKeyDown = (e: KeyboardEvent) => {
    if (selectedIndex === null) return;

    switch (e.key) {
      case "ArrowLeft":
        handlePrevious();
        break;
      case "ArrowRight":
        handleNext();
        break;
      case "Escape":
        handleCloseLightbox();
        break;
      case "+":
      case "=":
        handleZoomIn();
        break;
      case "-":
        handleZoomOut();
        break;
    }
  };

  // Attach keyboard listeners
  useState(() => {
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  });

  if (imageAttachments.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        <ImageIcon className="h-12 w-12 mx-auto mb-3 opacity-50" />
        <p>No images to display</p>
      </div>
    );
  }

  const gridColsClass = {
    2: "grid-cols-2",
    3: "grid-cols-2 sm:grid-cols-3",
    4: "grid-cols-2 sm:grid-cols-3 md:grid-cols-4",
    5: "grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5",
  }[columns];

  return (
    <>
      {/* Gallery Grid */}
      <div className={cn("space-y-3", className)}>
        {showControls && (
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              {imageAttachments.length} image{imageAttachments.length !== 1 ? "s" : ""}
            </p>
            <Button
              size="sm"
              variant="outline"
              onClick={() => handleOpenLightbox(0)}
            >
              <Grid3x3 className="h-4 w-4 mr-1" />
              View Gallery
            </Button>
          </div>
        )}

        <div className={cn("grid gap-3", gridColsClass)}>
          {imageAttachments.map((attachment, index) => (
            <button
              key={attachment.id}
              onClick={() => handleOpenLightbox(index)}
              className="group relative aspect-square rounded-lg overflow-hidden bg-gray-100 hover:shadow-lg transition-all"
            >
              <img
                src={attachment.thumbnailUrl || attachment.fileUrl}
                alt={attachment.fileName}
                className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300"
              />
              {/* Overlay on hover */}
              <div className="absolute inset-0 bg-black/0 group-hover:bg-black/40 transition-colors flex items-center justify-center">
                <ZoomIn className="h-8 w-8 text-white opacity-0 group-hover:opacity-100 transition-opacity" />
              </div>
              {/* Image info overlay */}
              <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/60 to-transparent p-3 opacity-0 group-hover:opacity-100 transition-opacity">
                <p className="text-white text-xs truncate font-medium">
                  {attachment.fileName}
                </p>
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Lightbox Dialog */}
      <Dialog open={selectedIndex !== null} onOpenChange={handleCloseLightbox}>
        <DialogContent className="max-w-[95vw] max-h-[95vh] w-full h-full p-0 gap-0 bg-black/95">
          {selectedAttachment && (
            <>
              {/* Header Controls */}
              <div className="absolute top-0 left-0 right-0 z-50 bg-gradient-to-b from-black/80 to-transparent p-4">
                <div className="flex items-center justify-between">
                  <div className="flex-1 min-w-0">
                    <p className="text-white text-sm font-medium truncate">
                      {selectedAttachment.fileName}
                    </p>
                    <p className="text-white/70 text-xs">
                      {selectedIndex! + 1} / {imageAttachments.length}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    {/* Zoom Controls */}
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={handleZoomOut}
                      className="text-white hover:bg-white/20"
                    >
                      <ZoomOut className="h-4 w-4" />
                    </Button>
                    <span className="text-white text-sm min-w-[4ch] text-center">
                      {zoom}%
                    </span>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={handleZoomIn}
                      className="text-white hover:bg-white/20"
                    >
                      <ZoomIn className="h-4 w-4" />
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={handleRotate}
                      className="text-white hover:bg-white/20"
                    >
                      <RotateCw className="h-4 w-4" />
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={handleResetView}
                      className="text-white hover:bg-white/20"
                    >
                      <Maximize2 className="h-4 w-4" />
                    </Button>

                    <div className="w-px h-6 bg-white/20 mx-1" />

                    {/* Slideshow */}
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={toggleSlideshow}
                      className="text-white hover:bg-white/20"
                    >
                      {isSlideshow ? (
                        <Pause className="h-4 w-4" />
                      ) : (
                        <Play className="h-4 w-4" />
                      )}
                    </Button>

                    {/* Download */}
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={handleDownloadCurrent}
                      className="text-white hover:bg-white/20"
                    >
                      <Download className="h-4 w-4" />
                    </Button>

                    {/* Close */}
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={handleCloseLightbox}
                      className="text-white hover:bg-white/20"
                    >
                      <X className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </div>

              {/* Main Image */}
              <div className="w-full h-full flex items-center justify-center p-16">
                <img
                  src={selectedAttachment.fileUrl}
                  alt={selectedAttachment.fileName}
                  className="max-w-full max-h-full object-contain transition-transform duration-300"
                  style={{
                    transform: `scale(${zoom / 100}) rotate(${rotation}deg)`,
                  }}
                />
              </div>

              {/* Navigation Buttons */}
              <button
                onClick={handlePrevious}
                disabled={selectedIndex === 0}
                className={cn(
                  "absolute left-4 top-1/2 -translate-y-1/2 z-50",
                  "h-12 w-12 rounded-full bg-black/50 hover:bg-black/70",
                  "flex items-center justify-center transition-all",
                  "disabled:opacity-30 disabled:cursor-not-allowed"
                )}
              >
                <ChevronLeft className="h-6 w-6 text-white" />
              </button>

              <button
                onClick={handleNext}
                disabled={selectedIndex === imageAttachments.length - 1 && !isSlideshow}
                className={cn(
                  "absolute right-4 top-1/2 -translate-y-1/2 z-50",
                  "h-12 w-12 rounded-full bg-black/50 hover:bg-black/70",
                  "flex items-center justify-center transition-all",
                  "disabled:opacity-30 disabled:cursor-not-allowed"
                )}
              >
                <ChevronRight className="h-6 w-6 text-white" />
              </button>

              {/* Thumbnail Strip */}
              <div className="absolute bottom-0 left-0 right-0 z-50 bg-gradient-to-t from-black/80 to-transparent p-4">
                <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-thin scrollbar-thumb-white/20">
                  {imageAttachments.map((attachment, index) => (
                    <button
                      key={attachment.id}
                      onClick={() => {
                        setSelectedIndex(index);
                        setZoom(100);
                        setRotation(0);
                      }}
                      className={cn(
                        "flex-shrink-0 h-16 w-16 rounded overflow-hidden border-2 transition-all",
                        index === selectedIndex
                          ? "border-white scale-110"
                          : "border-transparent opacity-50 hover:opacity-100"
                      )}
                    >
                      <img
                        src={attachment.thumbnailUrl || attachment.fileUrl}
                        alt={attachment.fileName}
                        className="w-full h-full object-cover"
                      />
                    </button>
                  ))}
                </div>
              </div>

              {/* Keyboard Hints */}
              <div className="absolute left-4 bottom-24 z-40 text-white/50 text-xs space-y-1">
                <p>← → Navigate</p>
                <p>+ - Zoom</p>
                <p>ESC Close</p>
              </div>
            </>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}