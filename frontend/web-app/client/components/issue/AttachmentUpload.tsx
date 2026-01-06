/**
 * AttachmentUpload Component
 * Handles file upload for task attachments with drag-and-drop
 */

import { useState, useRef } from "react";
import { Upload, X, FileIcon, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

interface AttachmentUploadProps {
  taskId: number;
  onUpload: (file: File) => Promise<void>;
  maxFileSize?: number; // in MB
  acceptedTypes?: string[];
  disabled?: boolean;
}

const DEFAULT_MAX_SIZE = 10; // 10MB
const DEFAULT_ACCEPTED_TYPES = [
  "image/*",
  "application/pdf",
  "application/msword",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "application/vnd.ms-excel",
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "text/*",
  ".zip",
  ".rar",
];

export function AttachmentUpload({
  taskId,
  onUpload,
  maxFileSize = DEFAULT_MAX_SIZE,
  acceptedTypes = DEFAULT_ACCEPTED_TYPES,
  disabled = false,
}: AttachmentUploadProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const validateFile = (file: File): boolean => {
    // Check file size
    const maxSizeBytes = maxFileSize * 1024 * 1024;
    if (file.size > maxSizeBytes) {
      toast.error(`File size must be less than ${maxFileSize}MB`);
      return false;
    }

    // Check file type (basic validation)
    const fileExtension = file.name.split(".").pop()?.toLowerCase();
    const isAccepted =
      acceptedTypes.some((type) => {
        if (type.startsWith(".")) {
          return `.${fileExtension}` === type;
        }
        if (type.endsWith("/*")) {
          const category = type.split("/")[0];
          return file.type.startsWith(category);
        }
        return file.type === type;
      }) || acceptedTypes.includes("*/*");

    if (!isAccepted) {
      toast.error("File type not supported");
      return false;
    }

    return true;
  };

  const handleFileSelect = async (file: File) => {
    if (!validateFile(file)) {
      return;
    }

    setSelectedFile(file);
    setIsUploading(true);

    try {
      await onUpload(file);
      toast.success(`${file.name} uploaded successfully`);
      setSelectedFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    } catch (error: any) {
      toast.error(error.message || "Failed to upload file");
      setSelectedFile(null);
    } finally {
      setIsUploading(false);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!disabled && !isUploading) {
      setIsDragging(true);
    }
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    if (disabled || isUploading) return;

    const files = Array.from(e.dataTransfer.files);
    if (files.length > 0) {
      handleFileSelect(files[0]);
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      handleFileSelect(files[0]);
    }
  };

  const handleBrowseClick = () => {
    fileInputRef.current?.click();
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="space-y-3">
      {/* Drop Zone */}
      <div
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        className={cn(
          "border-2 border-dashed rounded-lg p-6 transition-all",
          isDragging && "border-blue-500 bg-blue-50",
          !isDragging && "border-gray-300 hover:border-gray-400",
          disabled && "opacity-50 cursor-not-allowed",
          isUploading && "bg-gray-50"
        )}
      >
        <input
          ref={fileInputRef}
          type="file"
          onChange={handleFileInputChange}
          accept={acceptedTypes.join(",")}
          className="hidden"
          disabled={disabled || isUploading}
        />

        {isUploading && selectedFile ? (
          <div className="flex items-center gap-3">
            <Loader2 className="h-5 w-5 animate-spin text-blue-600" />
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium truncate">{selectedFile.name}</p>
              <p className="text-xs text-muted-foreground">
                Uploading... {formatFileSize(selectedFile.size)}
              </p>
            </div>
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center gap-2 text-center">
            <Upload className="h-8 w-8 text-gray-400" />
            <div>
              <p className="text-sm text-gray-600">
                <button
                  type="button"
                  onClick={handleBrowseClick}
                  disabled={disabled}
                  className="text-blue-600 hover:text-blue-700 font-medium focus:outline-none focus:underline disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Click to upload
                </button>{" "}
                or drag and drop
              </p>
              <p className="text-xs text-gray-500 mt-1">
                Max file size: {maxFileSize}MB
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Accepted file types info */}
      <div className="text-xs text-muted-foreground">
        Supported: Images, PDF, Word, Excel, Text files, Archives
      </div>
    </div>
  );
}
