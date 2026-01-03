/**
 * IssueActionMenu Component
 * Dropdown menu for issue actions (archive, delete, copy link, etc.)
 */

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { MoreHorizontal, Trash2, Copy, Link, Archive } from "lucide-react";
import { toast } from "sonner";

interface IssueActionMenuProps {
  taskId: number;
  isArchived: boolean;
  isProjectArchived: boolean;
  onArchive: () => void;
  onUnarchive: () => void;
  onDelete: () => void;
  onCopyLink: () => void;
}

export function IssueActionMenu({
  taskId,
  isArchived,
  isProjectArchived,
  onArchive,
  onUnarchive,
  onDelete,
  onCopyLink,
}: IssueActionMenuProps) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" title="More actions">
          <MoreHorizontal className="h-4 w-4" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-48">
        <DropdownMenuLabel>Actions</DropdownMenuLabel>
        <DropdownMenuSeparator />

        <DropdownMenuItem onClick={onCopyLink} disabled={isProjectArchived}>
          <Link className="h-4 w-4 mr-2" />
          Copy Link
        </DropdownMenuItem>

        <DropdownMenuItem 
          onClick={() => {
            toast.info("Duplicate feature coming soon", {
              description: "This will create a copy of the issue with the same details",
            });
          }}
          disabled={isProjectArchived}
        >
          <Copy className="h-4 w-4 mr-2" />
          Duplicate Issue
        </DropdownMenuItem>

        <DropdownMenuSeparator />

        {isArchived ? (
          <DropdownMenuItem 
            onClick={onUnarchive}
            disabled={isProjectArchived}
          >
            <Archive className="h-4 w-4 mr-2" />
            Unarchive
          </DropdownMenuItem>
        ) : (
          <DropdownMenuItem 
            onClick={onArchive}
            disabled={isProjectArchived}
          >
            <Archive className="h-4 w-4 mr-2" />
            Archive
          </DropdownMenuItem>
        )}

        <DropdownMenuSeparator />

        <DropdownMenuItem
          className="text-red-600 focus:text-red-600 focus:bg-red-50"
          onClick={onDelete}
          disabled={isProjectArchived}
        >
          <Trash2 className="h-4 w-4 mr-2" />
          Delete Permanently
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
