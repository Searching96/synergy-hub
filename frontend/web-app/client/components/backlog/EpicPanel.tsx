import { useState } from "react";
import { X, ChevronRight, ChevronDown, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";

interface Epic {
  id: string;
  name: string;
  issueCount: number;
  startDate?: string;
  dueDate?: string;
  isSelected?: boolean;
}

interface EpicPanelProps {
  epics: Epic[];
  selectedEpicId?: string;
  onSelectEpic: (epicId: string) => void;
  onClose: () => void;
}

export default function EpicPanel({
  epics,
  selectedEpicId,
  onSelectEpic,
  onClose,
}: EpicPanelProps) {
  const [expandedItems, setExpandedItems] = useState<Set<string>>(
    new Set(["issues-without-epic"])
  );

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

  return (
    <div className="flex flex-col w-[280px] border-r border-gray-200 bg-white flex-shrink-0 overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200">
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
            className={`w-full flex items-center gap-2 px-4 py-3 hover:bg-gray-50 transition-colors text-left ${
              selectedEpicId === "none"
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
              className={`w-full flex items-center gap-2 px-4 py-3 hover:bg-gray-50 transition-colors text-left ${
                selectedEpicId === epic.id
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
      <div className="border-t border-gray-200 px-4 py-3">
        <Button
          variant="ghost"
          size="sm"
          className="w-full text-left justify-start text-gray-700 hover:text-gray-900 h-8"
        >
          <Plus className="h-4 w-4 mr-2" />
          Create epic
        </Button>
      </div>
    </div>
  );
}
