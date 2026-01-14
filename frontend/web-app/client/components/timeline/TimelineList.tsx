import { TimelineSprint, TimelineTask } from '@/services/timeline.service';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { format } from 'date-fns';
import { CheckCircle2, Circle, AlertCircle, Zap } from 'lucide-react';

interface TimelineListProps {
  sprints: TimelineSprint[];
  tasks: TimelineTask[];
}

export default function TimelineList({ sprints, tasks }: TimelineListProps) {
  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'DONE':
        return <CheckCircle2 className="w-5 h-5 text-green-500" />;
      case 'IN_PROGRESS':
        return <Zap className="w-5 h-5 text-blue-500" />;
      case 'IN_REVIEW':
        return <AlertCircle className="w-5 h-5 text-purple-500" />;
      case 'BLOCKED':
        return <AlertCircle className="w-5 h-5 text-red-500" />;
      default:
        return <Circle className="w-5 h-5 text-gray-500" />;
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

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'CRITICAL':
        return 'bg-red-100 text-red-700 border-red-300';
      case 'HIGH':
        return 'bg-orange-100 text-orange-700 border-orange-300';
      case 'MEDIUM':
        return 'bg-yellow-100 text-yellow-700 border-yellow-300';
      case 'LOW':
        return 'bg-green-100 text-green-700 border-green-300';
      default:
        return 'bg-gray-100 text-gray-700 border-gray-300';
    }
  };

  return (
    <div className="space-y-6">
      {/* Sprints Section */}
      {sprints.length > 0 && (
        <div className="space-y-3">
          <h2 className="text-xl font-semibold">Upcoming Sprints</h2>
          {sprints.map((sprint) => (
            <Card key={sprint.id}>
              <CardContent className="pt-6">
                <div className="space-y-4">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <h3 className="text-lg font-semibold">{sprint.name}</h3>
                        <Badge variant="outline">{sprint.status}</Badge>
                      </div>
                      <p className="text-sm text-gray-600">
                        {format(new Date(sprint.startDate), 'MMM dd, yyyy')} -{' '}
                        {format(new Date(sprint.endDate), 'MMM dd, yyyy')}
                      </p>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <div className="flex justify-between text-sm">
                      <span className="text-gray-600">Progress</span>
                      <span className="font-medium">
                        {sprint.completedTasks}/{sprint.totalTasks} completed
                      </span>
                    </div>
                    <Progress value={sprint.completionPercentage} className="h-2" />
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Tasks Section */}
      {tasks.length > 0 && (
        <div className="space-y-3">
          <h2 className="text-xl font-semibold">Timeline Tasks</h2>
          <div className="space-y-2">
            {tasks.map((task) => (
              <Card key={task.id} className="hover:shadow-md transition-shadow">
                <CardContent className="pt-6">
                  <div className="flex items-start gap-4">
                    {/* Status Icon */}
                    <div className="flex-shrink-0 mt-1">
                      {getStatusIcon(task.status)}
                    </div>

                    {/* Task Info */}
                    <div className="flex-1 min-w-0">
                      <h3 className="text-base font-semibold truncate">{task.title}</h3>
                      <div className="flex flex-wrap gap-2 mt-2">
                        <Badge variant="outline" className={getTypeColor(task.type || 'TASK')}>
                          {task.type || 'TASK'}
                        </Badge>
                        {task.priority && (
                          <Badge variant="outline" className={getPriorityColor(task.priority)}>
                            {task.priority}
                          </Badge>
                        )}
                        <Badge variant="outline">{task.status}</Badge>
                      </div>

                      {/* Meta Information */}
                      <div className="text-sm text-gray-600 mt-3 space-y-1">
                        {task.sprintName && <p>Sprint: {task.sprintName}</p>}
                        {task.assigneeName && <p>Assigned to: {task.assigneeName}</p>}
                        {task.dueDate && (
                          <p>Due: {format(new Date(task.dueDate), 'MMM dd, yyyy')}</p>
                        )}
                        {task.storyPoints && <p>Story Points: {task.storyPoints}</p>}
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
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
