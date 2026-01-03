import { useEffect } from "react";
import type { TaskStatus } from "@/types/task.types";

export interface UseIssueDetailKeyboardShortcutsOptions {
  isOpen: boolean;
  isEditingTitle: boolean;
  canSaveTitle: boolean;
  taskStatus?: TaskStatus;
  onClose: () => void;
  onSaveTitle: () => void;
  onStatusToggle: () => void;
}

/**
 * Custom hook for managing keyboard shortcuts in the Issue Detail Modal
 * 
 * Shortcuts:
 * - Escape: Close modal
 * - Ctrl/Cmd + S: Save title (when editing)
 * - Ctrl/Cmd + D: Toggle Done status
 */
export function useIssueDetailKeyboardShortcuts({
  isOpen,
  isEditingTitle,
  canSaveTitle,
  taskStatus,
  onClose,
  onSaveTitle,
  onStatusToggle,
}: UseIssueDetailKeyboardShortcutsOptions) {
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      // Don't trigger shortcuts if user is typing in a textarea or input
      const target = e.target as HTMLElement;
      const isTextInput = 
        target.tagName === "TEXTAREA" || 
        (target.tagName === "INPUT" && target.getAttribute("type") !== "checkbox");

      // Escape to close (works even in text inputs)
      if (e.key === "Escape") {
        onClose();
        return;
      }

      // Ctrl/Cmd + S to save title (only when editing)
      if ((e.ctrlKey || e.metaKey) && e.key === "s") {
        e.preventDefault();
        if (isEditingTitle && canSaveTitle) {
          onSaveTitle();
        }
        return;
      }

      // Ctrl/Cmd + D to toggle done status (not in text inputs)
      if ((e.ctrlKey || e.metaKey) && e.key === "d" && !isTextInput) {
        e.preventDefault();
        onStatusToggle();
        return;
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, isEditingTitle, canSaveTitle, taskStatus, onClose, onSaveTitle, onStatusToggle]);
}
