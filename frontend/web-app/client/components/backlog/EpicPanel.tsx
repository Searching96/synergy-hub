import { useState } from "react";
import { X, ChevronRight, ChevronDown, Plus, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useCreateTask } from "@/hooks/useTasks";
import { toast } from "sonner";

interface Epic {
  id: string;
  name: string;
  issueCount: number;
  startDate?: string;
  dueDate?: string;
  isSelected?: boolean;
}

interface EpicPanelProps {
  projectId: number;
  epics: Epic[];
  selectedEpicId?: string;
  onSelectEpic: (epicId: string) => void;
  onClose: () => void;
}

export default function EpicPanel({
  projectId,
  epics,
  selectedEpicId,
  onSelectEpic,
  onClose,
}: EpicPanelProps) {
  const [expandedItems, setExpandedItems] = useState<Set<string>>(
    new Set(["issues-without-epic"])
  );
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [newEpicName, setNewEpicName] = useState("");
  const createTask = useCreateTask();

  const toggleExpanded = (id: string) => {
    const newSet = new Set(expandedItems);
    if (newSet.has(id)) {
      newSet.delete(id);
    } else {
      newSet.add(id);
    }
    setExpandedItems(newSet);
  };

  const handleSelectEpic = (epicId: string) => {
    onSelectEpic(epicId);
  };

  const isExpanded = (id: string) => expandedItems.has(id);

  const handleCreateEpic = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newEpicName.trim()) return;

    try {
      await createTask.mutateAsync({
        title: newEpicName,
        description: "",
        status: "TO_DO",
        priority: "MEDIUM",
        type: "EPIC",
        projectId: projectId,
        storyPoints: null,
      });
      setNewEpicName("");
      setIsCreateDialogOpen(false);
      // Toast handled by hook
    } catch (error) {
      console.error("Failed to create epic", error);
    }
  };


  return (
    <div className="flex flex-col w-[280px] border-r border-gray-200 bg-white flex-shrink-0 overflow-hidden h-full">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200 flex-shrink-0">
        <h3 className="font-semibold text-sm text-gray-900">Epics</h3>
        <button
          onClick={onClose}
          className="p-1 hover:bg-gray-100 rounded transition-colors"
          aria-label="Close epic panel"
        >
          <X className="h-4 w-4 text-gray-500" />
        </button>
      </div>

      {/* Scrollable Content */}
      <div className="flex-1 overflow-y-auto">
        {/* Issues without epic */}
        <div className="border-b border-gray-200">
          <button
            onClick={() => toggleExpanded("issues-without-epic")}
            className={`w-full flex items-center gap-2 px-4 py-3 hover:bg-gray-50 transition-colors text-left ${selectedEpicId === "none"
                ? "bg-blue-50 border-l-4 border-purple-600"
                : ""
              }`}
          >
            {isExpanded("issues-without-epic") ? (
              <ChevronDown className="h-4 w-4 text-gray-500" />
            ) : (
              <ChevronRight className="h-4 w-4 text-gray-500" />
            )}
            <span className="text-sm font-medium text-gray-700">
              Issues without epic
            </span>
          </button>
          {isExpanded("issues-without-epic") && (
            <div className="px-4 py-2 bg-gray-50 border-t border-gray-200">
              <p className="text-xs text-gray-500">No issues</p>
            </div>
          )}
        </div>

        {/* Epic List */}
        {epics.map((epic) => (
          <div key={epic.id} className="border-b border-gray-200">
            <button
              onClick={() => {
                handleSelectEpic(epic.id);
                toggleExpanded(epic.id);
              }}
              className={`w-full flex items-center gap-2 px-4 py-3 hover:bg-gray-50 transition-colors text-left ${selectedEpicId === epic.id
                  ? "bg-blue-50 border-l-4 border-purple-600 pl-3"
                  : ""
                }`}
            >
              {isExpanded(epic.id) ? (
                <ChevronDown className="h-4 w-4 text-gray-500" />
              ) : (
                <ChevronRight className="h-4 w-4 text-gray-500" />
              )}
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">
                  {epic.name}
                </p>
              </div>
            </button>

            {isExpanded(epic.id) && (
              <div className="px-4 py-3 bg-gray-50 border-t border-gray-200 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs text-gray-600">Issues</span>
                  <span className="text-xs font-semibold text-gray-900">
                    {epic.issueCount}
                  </span>
                </div>
                {epic.startDate && (
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-gray-600">Start date</span>
                    <span className="text-xs text-gray-900">
                      {new Date(epic.startDate).toLocaleDateString()}
                    </span>
                  </div>
                )}
                {epic.dueDate && (
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-gray-600">Due date</span>
                    <span className="text-xs text-gray-900">
                      {new Date(epic.dueDate).toLocaleDateString()}
                    </span>
                  </div>
                )}
                {selectedEpicId === epic.id && (
                  <Button
                    size="sm"
                    variant="outline"
                    className="w-full mt-2 text-xs h-8"
                  >
                    View all details
                  </Button>
                )}
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Footer */}
      <div className="border-t border-gray-200 px-4 py-3 flex-shrink-0">
        <Button
          variant="ghost"
          size="sm"
          className="w-full text-left justify-start text-gray-700 hover:text-gray-900 h-8"
          onClick={() => setIsCreateDialogOpen(true)}
        >
          <Plus className="h-4 w-4 mr-2" />
          Create epic
        </Button>
      </div>

      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>Create Epic</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleCreateEpic}>
            <div className="grid gap-4 py-4">
              <div className="grid gap-2">
                <Label htmlFor="epic-name">Epic Name</Label>
                <Input
                  id="epic-name"
                  value={newEpicName}
                  onChange={(e) => setNewEpicName(e.target.value)}
                  placeholder="e.g., Q1 Marketing Launch"
                  autoFocus
                />
              </div>
            </div>
            <DialogFooter>
              <Button type="button" variant="ghost" onClick={() => setIsCreateDialogOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={createTask.isPending || !newEpicName.trim()}>
                {createTask.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : "Create"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
