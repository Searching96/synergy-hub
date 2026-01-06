import { useMemo } from 'react';
import { TimelineSprint, TimelineTask } from '@/services/timeline.service';
import { format, differenceInDays, addDays } from 'date-fns';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';

interface TimelineGanttProps {
  sprints: TimelineSprint[];
  tasks: TimelineTask[];
  viewStart: Date;
  viewEnd: Date;
}

export default function TimelineGantt({
  sprints,
  tasks,
  viewStart,
  viewEnd,
}: TimelineGanttProps) {
  const totalDays = useMemo(() => {
    return differenceInDays(viewEnd, viewStart) + 1;
  }, [viewStart, viewEnd]);

  const getItemPosition = (startDate: string | undefined, endDate: string | undefined) => {
    if (!startDate || !endDate) return { offset: 0, width: 0 };

    const itemStart = new Date(startDate);
    const itemEnd = new Date(endDate);

    if (itemEnd < viewStart || itemStart > viewEnd) {
      return { offset: 0, width: 0 };
    }

    const adjustedStart = itemStart < viewStart ? viewStart : itemStart;
    const adjustedEnd = itemEnd > viewEnd ? viewEnd : itemEnd;

    const offset = differenceInDays(adjustedStart, viewStart);
    const width = differenceInDays(adjustedEnd, adjustedStart) + 1;

    return {
      offset: (offset / totalDays) * 100,
      width: (width / totalDays) * 100,
    };
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'DONE':
        return 'bg-green-500';
      case 'IN_PROGRESS':
        return 'bg-blue-500';
      case 'IN_REVIEW':
        return 'bg-purple-500';
      case 'BLOCKED':
        return 'bg-red-500';
      default:
        return 'bg-gray-500';
    }
  };

  const getTypeColor = (type: string) => {
    switch (type) {
      case 'BUG':
        return 'bg-red-100 text-red-700 border-red-300';
      case 'STORY':
        return 'bg-blue-100 text-blue-700 border-blue-300';
      case 'EPIC':
        return 'bg-purple-100 text-purple-700 border-purple-300';
      default:
        return 'bg-gray-100 text-gray-700 border-gray-300';
    }
  };

  // Generate date headers
  const dateHeaders = useMemo(() => {
    const headers = [];
    let current = new Date(viewStart);
    while (current <= viewEnd) {
      headers.push(new Date(current));
      current = addDays(current, 1);
    }
    return headers;
  }, [viewStart, viewEnd]);

  return (
    <div className="space-y-6">
      {/* Sprints Section */}
      {sprints.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Sprints Timeline</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {sprints.map((sprint) => {
              const { offset, width } = getItemPosition(sprint.startDate, sprint.endDate);
              if (width === 0) return null;

              return (
                <div key={sprint.id} className="space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <div className="font-semibold text-sm">{sprint.name}</div>
                      <div className="text-xs text-gray-600">
                        {format(new Date(sprint.startDate), 'MMM dd')} -{' '}
                        {format(new Date(sprint.endDate), 'MMM dd')}
                      </div>
                    </div>
                    <Badge variant="outline">{sprint.status}</Badge>
                  </div>

                  {/* Progress bar */}
                  <div className="space-y-1">
                    <div className="flex justify-between text-xs">
                      <span>Progress</span>
                      <span>
                        {sprint.completedTasks}/{sprint.totalTasks}
                      </span>
                    </div>
                    <Progress value={sprint.completionPercentage} className="h-2" />
                  </div>

                  {/* Gantt bar */}
                  <div className="h-8 bg-gray-100 rounded relative border border-gray-300">
                    <div
                      className="h-full bg-blue-500 rounded flex items-center px-2"
                      style={{
                        marginLeft: `${offset}%`,
                        width: `${width}%`,
                      }}
                    >
                      <span className="text-xs text-white font-semibold truncate">
                        {sprint.name}
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
          </CardContent>
        </Card>
      )}

      {/* Tasks Section */}
      {tasks.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Tasks Timeline</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 max-h-96 overflow-y-auto">
            {tasks.map((task) => {
              const { offset, width } = getItemPosition(
                task.dueDate || task.createdAt,
                task.dueDate || task.createdAt
              );

              return (
                <div key={task.id} className="space-y-1">
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex-1 min-w-0">
                      <div className="text-sm font-medium truncate">{task.title}</div>
                      <div className="flex gap-2 mt-1">
                        <Badge variant="outline" className={getTypeColor(task.type || 'TASK')}>
                          {task.type || 'TASK'}
                        </Badge>
                        {task.assigneeName && (
                          <span className="text-xs text-gray-600">{task.assigneeName}</span>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Gantt bar */}
                  <div className="h-6 bg-gray-100 rounded relative border border-gray-200">
                    <div
                      className={`h-full rounded flex items-center px-1.5 ${getStatusColor(task.status)}`}
                      style={{
                        marginLeft: `${offset}%`,
                        width: Math.max(width, 2),
                      }}
                      title={task.title}
                    />
                  </div>
                </div>
              );
            })}
          </CardContent>
        </Card>
      )}

      {/* Empty State */}
      {sprints.length === 0 && tasks.length === 0 && (
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-gray-600">No sprints or tasks in the selected timeline range</p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
