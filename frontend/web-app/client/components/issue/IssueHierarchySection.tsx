/**
 * IssueHierarchySection Component
 * Displays parent/epic relationships and subtasks in Jira style
 */

import { Link } from "react-router-dom";
import { ChevronRight, Zap, ListChecks, Bug, CheckSquare, Lightbulb } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import type { Task, TaskType } from "@/types/task.types";

interface IssueHierarchySectionProps {
  task: Task;
  onIssueClick?: (issueId: number) => void;
}

const typeIcons: Record<TaskType, any> = {
  EPIC: Zap,
  STORY: Lightbulb,
  TASK: CheckSquare,
  BUG: Bug,
  SUBTASK: ListChecks,
};

const typeColors: Record<TaskType, string> = {
  EPIC: "text-purple-600 bg-purple-50 border-purple-200",
  STORY: "text-green-600 bg-green-50 border-green-200",
  TASK: "text-blue-600 bg-blue-50 border-blue-200",
  BUG: "text-red-600 bg-red-50 border-red-200",
  SUBTASK: "text-gray-600 bg-gray-50 border-gray-200",
};

export function IssueHierarchySection({ task, onIssueClick }: IssueHierarchySectionProps) {
  const hasHierarchy = task.epic || task.parentTask || (task.subtasks && task.subtasks.length > 0);

  if (!hasHierarchy) {
    return null;
  }

  const handleIssueClick = (e: React.MouseEvent, issueId: number) => {
    e.preventDefault();
    if (onIssueClick) {
      onIssueClick(issueId);
    }
  };

  const renderIssueLink = (issue: { id: number; title: string; type?: TaskType }, label: string) => {
    const Icon = issue.type ? typeIcons[issue.type] : CheckSquare;
    const colorClass = issue.type ? typeColors[issue.type] : typeColors.TASK;

    return (
      <div className="flex items-start gap-2 p-2 rounded-md hover:bg-gray-50 transition-colors cursor-pointer group">
        <div className={cn("flex items-center gap-2 flex-1", colorClass)}>
          <Icon className="h-4 w-4 flex-shrink-0" />
          <div className="flex-1 min-w-0">
            <div className="text-xs text-muted-foreground mb-0.5">{label}</div>
            <button
              onClick={(e) => handleIssueClick(e, issue.id)}
              className="text-sm font-medium hover:underline text-left truncate block w-full"
            >
              {issue.title}
            </button>
          </div>
        </div>
        <ChevronRight className="h-4 w-4 text-gray-400 opacity-0 group-hover:opacity-100 transition-opacity" />
      </div>
    );
  };

  return (
    <div className="space-y-4">
      <Separator />
      <div>
        <h3 className="text-sm font-semibold mb-3">Issue Hierarchy</h3>
        <div className="space-y-2">
          {/* Epic Link */}
          {task.epic && task.type !== "EPIC" && (
            <div className="border rounded-md p-2 bg-purple-50/50">
              {renderIssueLink(
                { id: task.epic.id, title: task.epic.title, type: "EPIC" },
                "Part of Epic"
              )}
            </div>
          )}

          {/* Parent Task Link */}
          {task.parentTask && task.type === "SUBTASK" && (
            <div className="border rounded-md p-2 bg-blue-50/50">
              {renderIssueLink(
                { id: task.parentTask.id, title: task.parentTask.title, type: task.parentTask.type },
                "Parent Issue"
              )}
            </div>
          )}

          {/* Subtasks */}
          {task.subtasks && task.subtasks.length > 0 && (
            <div className="border rounded-md p-3">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-medium text-muted-foreground">
                  {task.type === "EPIC" ? "Child Issues" : "Subtasks"}
                </span>
                <Badge variant="secondary" className="text-xs">
                  {task.subtasks.length}
                </Badge>
              </div>
              <div className="space-y-1">
                {task.subtasks.map((subtask) => {
                  const Icon = typeIcons[subtask.type];
                  const colorClass = typeColors[subtask.type];
                  
                  return (
                    <button
                      key={subtask.id}
                      onClick={(e) => handleIssueClick(e, subtask.id)}
                      className="w-full text-left p-2 rounded hover:bg-gray-50 transition-colors group flex items-center gap-2"
                    >
                      <Icon className={cn("h-3.5 w-3.5", colorClass.split(' ')[0])} />
                      <span className="text-sm flex-1 truncate group-hover:underline">
                        {subtask.title}
                      </span>
                      <Badge 
                        variant="outline" 
                        className="text-xs"
                      >
                        {subtask.status}
                      </Badge>
                    </button>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
