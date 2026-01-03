/**
 * useIssueEdit Hook
 * Manages edit state for issue title and description
 * Extracted to reduce component complexity
 */

import { useState, useEffect } from 'react';
import type { Task } from '@/types/task.types';

export interface UseIssueEditState {
  isEditingTitle: boolean;
  setIsEditingTitle: (value: boolean) => void;
  editedTitle: string;
  setEditedTitle: (value: string) => void;
  editedDescription: string;
  setEditedDescription: (value: string) => void;
  canSaveTitle: boolean;
  canSaveDescription: boolean;
}

export function useIssueEdit(task: Task | undefined): UseIssueEditState {
  const [isEditingTitle, setIsEditingTitle] = useState(false);
  const [editedTitle, setEditedTitle] = useState('');
  const [editedDescription, setEditedDescription] = useState('');

  // Initialize state when task loads
  useEffect(() => {
    if (task) {
      setEditedTitle(task.title);
      setEditedDescription(task.description || '');
    }
  }, [task]);

  return {
    isEditingTitle,
    setIsEditingTitle,
    editedTitle,
    setEditedTitle,
    editedDescription,
    setEditedDescription,
    canSaveTitle: editedTitle.trim() && editedTitle !== task?.title,
    canSaveDescription: editedDescription !== task?.description,
  };
}
