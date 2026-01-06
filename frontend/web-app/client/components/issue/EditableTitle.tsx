import { useState, useEffect } from "react";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

interface EditableTitleProps {
  value: string;
  onChange: (newTitle: string) => Promise<void>;
  disabled?: boolean;
  className?: string;
}

export function EditableTitle({ value, onChange, disabled = false, className }: EditableTitleProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [localValue, setLocalValue] = useState(value);
  const [isSaving, setIsSaving] = useState(false);

  // Sync local value with prop changes
  useEffect(() => {
    setLocalValue(value);
  }, [value]);

  const handleSave = async () => {
    if (localValue.trim() && localValue !== value && !isSaving) {
      setIsSaving(true);
      try {
        await onChange(localValue);
      } catch (error) {
        // Error handled by parent component
        setLocalValue(value); // Revert on error
      } finally {
        setIsSaving(false);
      }
    }
    setIsEditing(false);
  };

  const handleCancel = () => {
    setLocalValue(value);
    setIsEditing(false);
  };

  if (isEditing) {
    return (
      <Input
        value={localValue}
        onChange={(e) => setLocalValue(e.target.value)}
        onBlur={handleSave}
        onKeyDown={(e) => {
          if (e.key === "Enter") {
            e.preventDefault();
            handleSave();
          }
          if (e.key === "Escape") {
            e.preventDefault();
            handleCancel();
          }
        }}
        className={cn("text-2xl font-bold border-0 px-0 focus-visible:ring-0", className)}
        autoFocus
        disabled={isSaving}
      />
    );
  }

  return (
    <h1
      className={cn(
        "text-2xl font-bold px-2 py-1 -mx-2 rounded",
        !disabled && "cursor-pointer hover:bg-gray-50 transition-colors",
        disabled && "cursor-not-allowed opacity-60",
        className
      )}
      onClick={() => !disabled && setIsEditing(true)}
      title={disabled ? "Editing disabled" : "Click to edit"}
    >
      {value}
    </h1>
  );
}
