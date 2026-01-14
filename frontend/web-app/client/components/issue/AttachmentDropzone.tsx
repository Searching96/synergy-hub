/**
 * AttachmentDropzone Component
 * Reusable drag-and-drop zone with inline preview
 * Can be embedded in comments, descriptions, chat messages, etc.
 */

import { useState, useRef, useCallback } from "react";
import { Upload, X, File, Image as ImageIcon, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface AttachmentDropzoneProps {
  onFilesSelected: (files: File[]) => void;
  maxFiles?: number;
  maxFileSize?: number; // in MB
  acceptedTypes?: string[];
  disabled?: boolean;
  compact?: boolean; // Smaller, inline version
  showPreview?: boolean; // Show thumbnail previews
  className?: string;
  placeholder?: string;
}

interface PreviewFile {
  file: File;
  preview?: string;
  id: string;
}

const DEFAULT_MAX_SIZE = 10;
const DEFAULT_ACCEPTED_TYPES = ["image/*", "application/pdf", "text/*", ".zip", ".rar"];

export function AttachmentDropzone({
  onFilesSelected,
  maxFiles = 5,
  maxFileSize = DEFAULT_MAX_SIZE,
  acceptedTypes = DEFAULT_ACCEPTED_TYPES,
  disabled = false,
  compact = false,
  showPreview = true,
  className,
  placeholder = "Drop files here or click to browse",
}: AttachmentDropzoneProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [previewFiles, setPreviewFiles] = useState<PreviewFile[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const dragCounterRef = useRef(0);

  const validateFile = (file: File): boolean => {
    const maxSizeBytes = maxFileSize * 1024 * 1024;
    if (file.size > maxSizeBytes) return false;
    if (file.size === 0) return false;

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

    return isAccepted;
  };

  const createPreview = (file: File): Promise<string | undefined> => {
    return new Promise((resolve) => {
      if (file.type.startsWith("image/")) {
        const reader = new FileReader();
        reader.onloadend = () => resolve(reader.result as string);
        reader.onerror = () => resolve(undefined);
        reader.readAsDataURL(file);
      } else {
        resolve(undefined);
      }
    });
  };

  const handleFiles = async (files: File[]) => {
    const validFiles = files.filter(validateFile);
    
    if (validFiles.length === 0) return;

    // Check max files limit
    if (previewFiles.length + validFiles.length > maxFiles) {
      validFiles.splice(maxFiles - previewFiles.length);
    }

    // Create previews
    const newPreviewFiles: PreviewFile[] = await Promise.all(
      validFiles.map(async (file) => ({
        file,
        preview: showPreview ? await createPreview(file) : undefined,
        id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      }))
    );

    const allFiles = [...previewFiles, ...newPreviewFiles];
    setPreviewFiles(allFiles);
    onFilesSelected(allFiles.map((pf) => pf.file));
  };

  const handleDragEnter = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    dragCounterRef.current++;
    if (!disabled) setIsDragging(true);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    dragCounterRef.current--;
    if (dragCounterRef.current === 0) setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    dragCounterRef.current = 0;

    if (disabled) return;

    const files = Array.from(e.dataTransfer.files);
    handleFiles(files);
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      handleFiles(Array.from(files));
    }
    // Reset input
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const handleRemoveFile = (id: string) => {
    const newFiles = previewFiles.filter((pf) => pf.id !== id);
    setPreviewFiles(newFiles);
    onFilesSelected(newFiles.map((pf) => pf.file));
  };

  const handleBrowseClick = () => {
    fileInputRef.current?.click();
  };

  const handlePaste = useCallback(
    (e: ClipboardEvent) => {
      if (disabled) return;

      const items = e.clipboardData?.items;
      if (!items) return;

      const files: File[] = [];
      for (let i = 0; i < items.length; i++) {
        if (items[i].kind === "file") {
          const file = items[i].getAsFile();
          if (file) files.push(file);
        }
      }

      if (files.length > 0) {
        e.preventDefault();
        handleFiles(files);
      }
    },
    [disabled]
  );

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const isImage = (file: File) => file.type.startsWith("image/");

  if (compact) {
    return (
      <div className={cn("space-y-2", className)}>
        <div
          onDragEnter={handleDragEnter}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          className={cn(
            "border-2 border-dashed rounded-lg p-3 transition-all cursor-pointer",
            isDragging && "border-blue-500 bg-blue-50",
            !isDragging && "border-gray-300 hover:border-gray-400",
            disabled && "opacity-50 cursor-not-allowed"
          )}
          onClick={handleBrowseClick}
        >
          <input
            ref={fileInputRef}
            type="file"
            onChange={handleFileInputChange}
            accept={acceptedTypes.join(",")}
            className="hidden"
            disabled={disabled}
            multiple={maxFiles > 1}
          />
          <div className="flex items-center gap-2">
            <Upload className="h-4 w-4 text-muted-foreground flex-shrink-0" />
            <span className="text-sm text-muted-foreground">{placeholder}</span>
          </div>
        </div>

        {/* Preview thumbnails - compact */}
        {previewFiles.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {previewFiles.map((pf) => (
              <div
                key={pf.id}
                className="relative group bg-gray-100 rounded p-1 flex items-center gap-1"
              >
                {pf.preview ? (
                  <img
                    src={pf.preview}
                    alt={pf.file.name}
                    className="h-8 w-8 rounded object-cover"
                  />
                ) : (
                  <div className="h-8 w-8 rounded bg-gray-200 flex items-center justify-center">
                    <File className="h-4 w-4 text-gray-400" />
                  </div>
                )}
                <span className="text-xs max-w-[100px] truncate">
                  {pf.file.name}
                </span>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    handleRemoveFile(pf.id);
                  }}
                  className="ml-1 p-0.5 rounded hover:bg-gray-300"
                  type="button"
                >
                  <X className="h-3 w-3" />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  }

  return (
    <div className={cn("space-y-3", className)}>
      <div
        onDragEnter={handleDragEnter}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        className={cn(
          "border-2 border-dashed rounded-lg p-6 transition-all",
          isDragging && "border-blue-500 bg-blue-50 scale-[1.02]",
          !isDragging && "border-gray-300 hover:border-gray-400",
          disabled && "opacity-50 cursor-not-allowed"
        )}
      >
        <input
          ref={fileInputRef}
          type="file"
          onChange={handleFileInputChange}
          accept={acceptedTypes.join(",")}
          className="hidden"
          disabled={disabled}
          multiple={maxFiles > 1}
        />

        <div className="flex flex-col items-center justify-center gap-3 text-center">
          <div
            className={cn(
              "h-12 w-12 rounded-full flex items-center justify-center transition-colors",
              isDragging ? "bg-blue-100" : "bg-gray-100"
            )}
          >
            <Upload
              className={cn(
                "h-6 w-6 transition-colors",
                isDragging ? "text-blue-600" : "text-gray-400"
              )}
            />
          </div>
          <div>
            <p className="text-sm text-gray-600 mb-1">
              <button
                type="button"
                onClick={handleBrowseClick}
                disabled={disabled}
                className="text-blue-600 hover:text-blue-700 font-medium focus:outline-none focus:underline disabled:opacity-50"
              >
                Click to upload
              </button>{" "}
              or drag and drop
            </p>
            <p className="text-xs text-gray-500">
              Up to {maxFiles} files, {maxFileSize}MB max each
            </p>
          </div>
        </div>
      </div>

      {/* Preview grid */}
      {showPreview && previewFiles.length > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
          {previewFiles.map((pf) => (
            <div
              key={pf.id}
              className="relative group border rounded-lg overflow-hidden bg-card hover:shadow-md transition-shadow"
            >
              {/* Thumbnail */}
              <div className="aspect-square bg-gray-100 flex items-center justify-center">
                {pf.preview ? (
                  <img
                    src={pf.preview}
                    alt={pf.file.name}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <File className="h-12 w-12 text-gray-400" />
                )}
              </div>

              {/* Info overlay */}
              <div className="p-2 bg-white">
                <p className="text-xs font-medium truncate" title={pf.file.name}>
                  {pf.file.name}
                </p>
                <p className="text-xs text-muted-foreground">
                  {formatFileSize(pf.file.size)}
                </p>
              </div>

              {/* Remove button */}
              <button
                onClick={() => handleRemoveFile(pf.id)}
                className="absolute top-2 right-2 p-1 rounded-full bg-red-600 text-white opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-700"
                type="button"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          ))}
        </div>
      )}

      {/* File list (if no preview) */}
      {!showPreview && previewFiles.length > 0 && (
        <div className="space-y-2">
          {previewFiles.map((pf) => (
            <div
              key={pf.id}
              className="flex items-center justify-between gap-2 p-2 rounded border bg-card"
            >
              <div className="flex items-center gap-2 flex-1 min-w-0">
                <File className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm truncate">{pf.file.name}</p>
                  <p className="text-xs text-muted-foreground">
                    {formatFileSize(pf.file.size)}
                  </p>
                </div>
              </div>
              <button
                onClick={() => handleRemoveFile(pf.id)}
                className="p-1 hover:bg-red-50 rounded text-red-600"
                type="button"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}