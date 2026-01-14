import { X, Lock, Eye, Share2, MoreHorizontal, Paperclip, GitBranch, LinkIcon } from "lucide-react";
import { Button } from "@/components/ui/button";

interface IssueDetailPanelProps {
  issueKey: string;
  issueType: string;
  title: string;
  status: string;
  description?: string;
  onClose: () => void;
}

export default function IssueDetailPanel({
  issueKey,
  issueType,
  title,
  status,
  description,
  onClose,
}: IssueDetailPanelProps) {
  const getTypeColor = (type: string) => {
    const colors: Record<string, string> = {
      EPIC: "bg-purple-600",
      STORY: "bg-blue-600",
      TASK: "bg-green-600",
      BUG: "bg-red-600",
      SUBTASK: "bg-gray-600",
    };
    return colors[type] || "bg-gray-600";
  };

  return (
    <div className="flex flex-col w-[420px] border-l border-gray-200 bg-white flex-shrink-0 overflow-hidden shadow-lg">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200">
        <div className="flex items-center gap-2">
          <span className="text-xs font-bold text-gray-600 tracking-wide">
            {issueKey}
          </span>
        </div>
        <div className="flex items-center gap-1">
          <button className="p-1.5 hover:bg-gray-100 rounded transition-colors">
            <Lock className="h-4 w-4 text-gray-500" />
          </button>
          <button className="p-1.5 hover:bg-gray-100 rounded transition-colors">
            <Eye className="h-4 w-4 text-gray-500" />
          </button>
          <button className="p-1.5 hover:bg-gray-100 rounded transition-colors">
            <Share2 className="h-4 w-4 text-gray-500" />
          </button>
          <button className="p-1.5 hover:bg-gray-100 rounded transition-colors">
            <MoreHorizontal className="h-4 w-4 text-gray-500" />
          </button>
          <button
            onClick={onClose}
            className="p-1.5 hover:bg-gray-100 rounded transition-colors"
            aria-label="Close detail panel"
          >
            <X className="h-4 w-4 text-gray-500" />
          </button>
        </div>
      </div>

      {/* Scrollable Content */}
      <div className="flex-1 min-h-0 overflow-y-auto">
        {/* Title Section */}
        <div className="px-4 py-4 border-b border-gray-200">
          <div className="flex items-start gap-3">
            <div className={`${getTypeColor(issueType)} text-white rounded w-6 h-6 flex items-center justify-center flex-shrink-0 text-xs font-bold`}>
              {issueType.charAt(0)}
            </div>
            <div className="flex-1 min-w-0">
              <h2 className="text-lg font-semibold text-gray-900 leading-tight">
                {title}
              </h2>
            </div>
          </div>
        </div>

        {/* Action Bar */}
        <div className="px-4 py-3 border-b border-gray-200 flex gap-2">
          <Button
            variant="outline"
            size="sm"
            className="flex items-center gap-1 text-xs h-8"
          >
            <Paperclip className="h-3.5 w-3.5" />
            Attach
          </Button>
          <Button
            variant="outline"
            size="sm"
            className="flex items-center gap-1 text-xs h-8"
          >
            <GitBranch className="h-3.5 w-3.5" />
            Subtask
          </Button>
          <Button
            variant="outline"
            size="sm"
            className="flex items-center gap-1 text-xs h-8"
          >
            <LinkIcon className="h-3.5 w-3.5" />
            Link
          </Button>
        </div>

        {/* Status */}
        <div className="px-4 py-3 border-b border-gray-200">
          <div className="inline-flex items-center gap-2 px-2 py-1 rounded-full bg-blue-100 text-blue-700 text-xs font-medium">
            {status}
          </div>
        </div>

        {/* Description */}
        <div className="px-4 py-4 border-b border-gray-200">
          <h3 className="text-xs font-bold text-gray-700 mb-2 uppercase tracking-wide">
            Description
          </h3>
          {description ? (
            <p className="text-sm text-gray-700 leading-relaxed">{description}</p>
          ) : (
            <p className="text-sm text-gray-400 italic">Add a description...</p>
          )}
        </div>

        {/* Confluence Pages */}
        <div className="px-4 py-4 border-b border-gray-200">
          <h3 className="text-xs font-bold text-gray-700 mb-2 uppercase tracking-wide">
            Confluence Pages
          </h3>
          <Button
            variant="outline"
            size="sm"
            className="text-xs h-8 w-full justify-start"
          >
            <LinkIcon className="h-3.5 w-3.5 mr-2" />
            Link Confluence pages
          </Button>
        </div>

        {/* Pinned Fields */}
        <div className="px-4 py-4">
          <h3 className="text-xs font-bold text-gray-700 mb-3 uppercase tracking-wide">
            Details
          </h3>
          <div className="space-y-3">
            <div className="flex justify-between items-center">
              <span className="text-xs text-gray-600">Type</span>
              <span className="text-xs font-medium text-gray-900">{issueType}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-xs text-gray-600">Status</span>
              <span className="text-xs font-medium text-gray-900">{status}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
