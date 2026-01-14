/**
 * AttachmentUpload Component - Production Ready
 * Handles file upload for task attachments with drag-and-drop, progress tracking,
 * retry logic, and better error handling
 */

import { useState, useRef, useCallback } from "react";
import { Upload, X, FileIcon, Loader2, AlertCircle, RefreshCw, Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { toast } from "sonner";
import { Progress } from "@/components/ui/progress";

interface AttachmentUploadProps {
  taskId: number;
  onUpload: (file: File, onProgress?: (progress: number) => void) => Promise<void>;
  maxFileSize?: number; // in MB
  acceptedTypes?: string[];
  disabled?: boolean;
  maxFiles?: number;
  onUploadComplete?: () => void;
}

interface UploadingFile {
  file: File;
  progress: number;
  status: 'uploading' | 'success' | 'error';
  error?: string;
  id: string;
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

// MIME type magic numbers for server-side validation
const MAGIC_NUMBERS: Record<string, number[]> = {
  'image/jpeg': [0xFF, 0xD8, 0xFF],
  'image/png': [0x89, 0x50, 0x4E, 0x47],
  'application/pdf': [0x25, 0x50, 0x44, 0x46],
  'application/zip': [0x50, 0x4B, 0x03, 0x04],
};

export function AttachmentUpload({
  taskId,
  onUpload,
  maxFileSize = DEFAULT_MAX_SIZE,
  acceptedTypes = DEFAULT_ACCEPTED_TYPES,
  disabled = false,
  maxFiles = 5,
  onUploadComplete,
}: AttachmentUploadProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [uploadingFiles, setUploadingFiles] = useState<UploadingFile[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const dragCounterRef = useRef(0);

  // Validate file using magic numbers (basic client-side check)
  const validateFileMagicNumber = async (file: File): Promise<boolean> => {
    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onloadend = (e) => {
        if (!e.target?.result) {
          resolve(true); // Can't validate, let server handle it
          return;
        }
        
        const arr = new Uint8Array(e.target.result as ArrayBuffer).subarray(0, 4);
        const header = Array.from(arr);
        
        // Check if file signature matches declared MIME type
        const expectedMagic = MAGIC_NUMBERS[file.type];
        if (expectedMagic) {
          const matches = expectedMagic.every((byte, i) => header[i] === byte);
          resolve(matches);
        } else {
          resolve(true); // No magic number defined, allow it
        }
      };
      reader.onerror = () => resolve(true);
      reader.readAsArrayBuffer(file.slice(0, 4));
    });
  };

  const validateFile = async (file: File): Promise<{ valid: boolean; error?: string }> => {
    // Check file size
    const maxSizeBytes = maxFileSize * 1024 * 1024;
    if (file.size > maxSizeBytes) {
      return { valid: false, error: `File size must be less than ${maxFileSize}MB` };
    }

    // Check if file size is suspiciously small (might be corrupted)
    if (file.size === 0) {
      return { valid: false, error: 'File appears to be empty or corrupted' };
    }

    // Check file type
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
      return { valid: false, error: 'File type not supported' };
    }

    // Validate file name (prevent path traversal)
    if (file.name.includes('..') || file.name.includes('/') || file.name.includes('\\')) {
      return { valid: false, error: 'Invalid file name' };
    }

    // Check magic number for common file types
    const isMagicNumberValid = await validateFileMagicNumber(file);
    if (!isMagicNumberValid) {
      return { valid: false, error: 'File content does not match its extension' };
    }

    return { valid: true };
  };

  const uploadFile = async (uploadingFile: UploadingFile) => {
    const updateFileProgress = (id: string, updates: Partial<UploadingFile>) => {
      setUploadingFiles(prev => 
        prev.map(f => f.id === id ? { ...f, ...updates } : f)
      );
    };

    try {
      await onUpload(
        uploadingFile.file,
        (progress) => {
          updateFileProgress(uploadingFile.id, { progress });
        }
      );

      updateFileProgress(uploadingFile.id, { 
        status: 'success', 
        progress: 100 
      });

      toast.success(`${uploadingFile.file.name} uploaded successfully`);

      // Remove successful upload after 2 seconds
      setTimeout(() => {
        setUploadingFiles(prev => prev.filter(f => f.id !== uploadingFile.id));
        if (onUploadComplete) onUploadComplete();
      }, 2000);

    } catch (error: any) {
      const errorMessage = error.message || 'Failed to upload file';
      updateFileProgress(uploadingFile.id, { 
        status: 'error', 
        error: errorMessage 
      });
      toast.error(`Failed to upload ${uploadingFile.file.name}: ${errorMessage}`);
    }
  };

  const handleFileSelect = async (files: File[]) => {
    // Check max files limit
    const currentUploadCount = uploadingFiles.filter(f => f.status === 'uploading').length;
    const availableSlots = maxFiles - currentUploadCount;
    
    if (files.length > availableSlots) {
      toast.error(`You can only upload ${availableSlots} more file(s)`);
      files = files.slice(0, availableSlots);
    }

    // Validate and prepare files for upload
    const validatedFiles: UploadingFile[] = [];
    
    for (const file of files) {
      const validation = await validateFile(file);
      
      if (!validation.valid) {
        toast.error(`${file.name}: ${validation.error}`);
        continue;
      }

      validatedFiles.push({
        file,
        progress: 0,
        status: 'uploading',
        id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      });
    }

    if (validatedFiles.length === 0) return;

    // Add to uploading files list
    setUploadingFiles(prev => [...prev, ...validatedFiles]);

    // Start uploads
    validatedFiles.forEach(uploadFile);

    // Clear file input
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const handleRetry = (uploadingFile: UploadingFile) => {
    setUploadingFiles(prev =>
      prev.map(f =>
        f.id === uploadingFile.id
          ? { ...f, status: 'uploading', progress: 0, error: undefined }
          : f
      )
    );
    uploadFile(uploadingFile);
  };

  const handleRemove = (id: string) => {
    setUploadingFiles(prev => prev.filter(f => f.id !== id));
  };

  const handleDragEnter = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    dragCounterRef.current++;
    if (!disabled) {
      setIsDragging(true);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    dragCounterRef.current--;
    if (dragCounterRef.current === 0) {
      setIsDragging(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    dragCounterRef.current = 0;

    if (disabled) return;

    const files = Array.from(e.dataTransfer.files);
    if (files.length > 0) {
      handleFileSelect(files);
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      handleFileSelect(Array.from(files));
    }
  };

  const handleBrowseClick = () => {
    fileInputRef.current?.click();
  };

  const handlePaste = useCallback((e: ClipboardEvent) => {
    if (disabled) return;
    
    const items = e.clipboardData?.items;
    if (!items) return;

    const files: File[] = [];
    for (let i = 0; i < items.length; i++) {
      if (items[i].kind === 'file') {
        const file = items[i].getAsFile();
        if (file) files.push(file);
      }
    }

    if (files.length > 0) {
      e.preventDefault();
      handleFileSelect(files);
      toast.info('Pasted files from clipboard');
    }
  }, [disabled]);

  // Add paste listener
  useState(() => {
    document.addEventListener('paste', handlePaste);
    return () => document.removeEventListener('paste', handlePaste);
  });

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const activeUploads = uploadingFiles.filter(f => f.status !== 'success');

  return (
    <div className="space-y-3">
      {/* Drop Zone */}
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

        <div className="flex flex-col items-center justify-center gap-2 text-center">
          <Upload className={cn(
            "h-8 w-8 transition-colors",
            isDragging ? "text-blue-600" : "text-gray-400"
          )} />
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
              Max {maxFiles} files, up to {maxFileSize}MB each
            </p>
            <p className="text-xs text-gray-400 mt-1">
              💡 Tip: You can also paste images from clipboard
            </p>
          </div>
        </div>
      </div>

      {/* Uploading Files List */}
      {activeUploads.length > 0 && (
        <div className="space-y-2">
          {activeUploads.map((uploadingFile) => (
            <div
              key={uploadingFile.id}
              className="border rounded-lg p-3 bg-card"
            >
              <div className="flex items-start gap-3">
                {/* Status Icon */}
                <div className="flex-shrink-0 mt-0.5">
                  {uploadingFile.status === 'uploading' && (
                    <Loader2 className="h-5 w-5 animate-spin text-blue-600" />
                  )}
                  {uploadingFile.status === 'success' && (
                    <div className="h-5 w-5 rounded-full bg-green-500 flex items-center justify-center">
                      <Check className="h-3 w-3 text-white" />
                    </div>
                  )}
                  {uploadingFile.status === 'error' && (
                    <AlertCircle className="h-5 w-5 text-red-600" />
                  )}
                </div>

                {/* File Info */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2 mb-1">
                    <p className="text-sm font-medium truncate">
                      {uploadingFile.file.name}
                    </p>
                    <button
                      onClick={() => handleRemove(uploadingFile.id)}
                      className="text-gray-400 hover:text-gray-600"
                      type="button"
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </div>
                  
                  <p className="text-xs text-muted-foreground mb-2">
                    {formatFileSize(uploadingFile.file.size)}
                  </p>

                  {/* Progress Bar */}
                  {uploadingFile.status === 'uploading' && (
                    <Progress value={uploadingFile.progress} className="h-1.5" />
                  )}

                  {/* Error Message */}
                  {uploadingFile.status === 'error' && (
                    <div className="flex items-center justify-between gap-2">
                      <p className="text-xs text-red-600">
                        {uploadingFile.error}
                      </p>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleRetry(uploadingFile)}
                        className="h-7 text-xs"
                        type="button"
                      >
                        <RefreshCw className="h-3 w-3 mr-1" />
                        Retry
                      </Button>
                    </div>
                  )}

                  {/* Success Message */}
                  {uploadingFile.status === 'success' && (
                    <p className="text-xs text-green-600">Upload complete</p>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Accepted file types info */}
      <div className="text-xs text-muted-foreground">
        Supported: Images, PDF, Word, Excel, Text files, Archives
      </div>
    </div>
  );
}