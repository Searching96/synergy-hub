import { Button } from "@/components/ui/button";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { HelpCircle } from "lucide-react";

/**
 * Keyboard shortcut help tooltip
 * Displays available keyboard shortcuts for the modal
 */
export function KeyboardShortcutsHelp() {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button 
          variant="ghost" 
          size="icon"
          className="h-8 w-8"
          title="Show keyboard shortcuts"
        >
          <HelpCircle className="h-4 w-4" />
        </Button>
      </TooltipTrigger>
      <TooltipContent side="bottom" className="max-w-xs">
        <div className="text-xs space-y-1.5">
          <div className="font-semibold mb-2">Keyboard Shortcuts</div>
          <div className="flex items-center gap-2">
            <kbd className="bg-muted px-2 py-0.5 rounded text-xs font-mono">Esc</kbd>
            <span>Close modal</span>
          </div>
          <div className="flex items-center gap-2">
            <kbd className="bg-muted px-2 py-0.5 rounded text-xs font-mono">Ctrl+S</kbd>
            <span>Save title</span>
          </div>
          <div className="flex items-center gap-2">
            <kbd className="bg-muted px-2 py-0.5 rounded text-xs font-mono">Ctrl+D</kbd>
            <span>Mark as done</span>
          </div>
        </div>
      </TooltipContent>
    </Tooltip>
  );
}
