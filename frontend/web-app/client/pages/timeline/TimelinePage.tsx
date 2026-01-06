import { useState, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import timelineService, { TimelineView, TimelineSprint, TimelineTask } from '@/services/timeline.service';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import TimelineGantt from '@/components/timeline/TimelineGantt';
import TimelineList from '@/components/timeline/TimelineList';
import { format, addMonths, subMonths } from 'date-fns';

export default function TimelinePage() {
  const { projectId } = useParams<{ projectId: string }>();
  const [view, setView] = useState<'gantt' | 'list'>('gantt');
  const [monthsAhead, setMonthsAhead] = useState(6);

  const { data: timelineResponse, isLoading, error } = useQuery({
    queryKey: ['timeline', projectId, monthsAhead],
    queryFn: () => timelineService.getProjectTimeline(projectId!, monthsAhead),
    enabled: !!projectId,
  });

  const timelineData = timelineResponse?.data;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-lg">Loading timeline...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-lg text-red-600">Error loading timeline</div>
      </div>
    );
  }

  if (!timelineData) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-lg">No timeline data available</div>
      </div>
    );
  }

  const handlePreviousMonth = () => {
    setMonthsAhead(Math.max(1, monthsAhead - 1));
  };

  const handleNextMonth = () => {
    setMonthsAhead(monthsAhead + 1);
  };

  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">{timelineData.projectName}</h1>
          <p className="text-gray-600 mt-1">Timeline View</p>
        </div>
        <div className="flex gap-2">
          <Button
            variant={view === 'gantt' ? 'default' : 'outline'}
            onClick={() => setView('gantt')}
          >
            Gantt Chart
          </Button>
          <Button
            variant={view === 'list' ? 'default' : 'outline'}
            onClick={() => setView('list')}
          >
            List View
          </Button>
        </div>
      </div>

      {/* Date Range Controls */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Timeline Range</CardTitle>
              <CardDescription>
                {format(new Date(timelineData.viewStartDate), 'MMM dd, yyyy')} -{' '}
                {format(new Date(timelineData.viewEndDate), 'MMM dd, yyyy')}
              </CardDescription>
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={handlePreviousMonth}
              >
                <ChevronLeft className="w-4 h-4" />
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={handleNextMonth}
              >
                <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Timeline Content */}
      {view === 'gantt' ? (
        <TimelineGantt
          sprints={timelineData.sprints}
          tasks={timelineData.tasks}
          viewStart={new Date(timelineData.viewStartDate)}
          viewEnd={new Date(timelineData.viewEndDate)}
        />
      ) : (
        <TimelineList
          sprints={timelineData.sprints}
          tasks={timelineData.tasks}
        />
      )}
    </div>
  );
}
