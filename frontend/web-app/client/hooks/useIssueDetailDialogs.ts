import { useState } from "react";

/**
 * Custom hook for managing dialog states in the Issue Detail Modal
 * Centralizes delete and archive dialog state management
 */
export function useIssueDetailDialogs() {
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [archiveDialogOpen, setArchiveDialogOpen] = useState(false);

  return {
    deleteDialogOpen,
    setDeleteDialogOpen,
    archiveDialogOpen,
    setArchiveDialogOpen,
    openDeleteDialog: () => setDeleteDialogOpen(true),
    closeDeleteDialog: () => setDeleteDialogOpen(false),
    openArchiveDialog: () => setArchiveDialogOpen(true),
    closeArchiveDialog: () => setArchiveDialogOpen(false),
  };
}
