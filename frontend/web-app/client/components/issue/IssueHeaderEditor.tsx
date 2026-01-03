/**
 * IssueHeaderEditor Component
 * Handles title editing with inline save/cancel functionality
 */

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Check, Pencil, X } from "lucide-react";
import type { Task } from "@/types/task.types";

interface IssueHeaderEditorProps {
  task: Task;
  isEditing: boolean;
  isProjectArchived: boolean;
  onEdit: () => void;
  onSave: (title: string) => Promise<void>;
  onCancel: () => void;
}

export function IssueHeaderEditor({
  task,
  isEditing,
  isProjectArchived,
  onEdit,
  onSave,
  onCancel,
}: IssueHeaderEditorProps) {
  const [title, setTitle] = useState(task.title);
  const [isSaving, setIsSaving] = useState(false);

  const handleSave = async () => {
    if (title.trim() && title !== task.title) {
      setIsSaving(true);
      try {
        await onSave(title);
      } finally {
        setIsSaving(false);
      }
    } else {
      onCancel();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      handleSave();
    } else if (e.key === "Escape") {
      onCancel();
    }
  };

  if (isEditing) {
    return (
      <div className="flex items-center gap-2">
        <Input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          onKeyDown={handleKeyDown}
          className="text-2xl font-bold h-10"
          autoFocus
          placeholder="Enter issue title"
        />
        <Button
          size="sm"
          onClick={handleSave}
          disabled={isSaving || !title.trim()}
          className="flex-shrink-0"
        >
          <Check className="h-4 w-4" />
        </Button>
        <Button
          size="sm"
          variant="ghost"
          onClick={() => {
            setTitle(task.title);
            onCancel();
          }}
          disabled={isSaving}
          className="flex-shrink-0"
        >
          <X className="h-4 w-4" />
        </Button>
      </div>
    );
  }

  return (
    <div className="group flex items-center gap-2">
      <h2 className="text-2xl font-bold flex-1 break-words">{task.title}</h2>
      <Button
        variant="ghost"
        size="sm"
        className="opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0"
        onClick={onEdit}
        disabled={isProjectArchived}
        title={isProjectArchived ? "Cannot edit archived projects" : "Click to edit"}
      >
        <Pencil className="h-4 w-4" />
      </Button>
    </div>
  );
}
