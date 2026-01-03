/**
 * Example: How to use Pagination in a task list
 * 
 * This demonstrates how to implement pagination with the usePagination hook
 * and PaginationControls component for large task lists.
 */

import { usePagination } from "@/hooks/usePagination";
import { PaginationControls } from "@/components/ui/PaginationControls";
import type { Task } from "@/types/task.types";

// Example component showing pagination usage
export function TaskListWithPagination({ tasks }: { tasks: Task[] }) {
  const pagination = usePagination(tasks, 25); // 25 items per page

  return (
    <div className="space-y-4">
      {/* Render only the paginated subset of tasks */}
      <div className="space-y-2">
        {pagination.paginatedData.map((task) => (
          <div key={task.id} className="border rounded p-4">
            <h3 className="font-medium">{task.title}</h3>
            <p className="text-sm text-muted-foreground">{task.status}</p>
          </div>
        ))}
      </div>

      {/* Pagination controls at the bottom */}
      {pagination.totalPages > 1 && (
        <PaginationControls
          page={pagination.page}
          pageSize={pagination.pageSize}
          totalPages={pagination.totalPages}
          totalItems={pagination.totalItems}
          hasNextPage={pagination.hasNextPage}
          hasPreviousPage={pagination.hasPreviousPage}
          onPageChange={pagination.goToPage}
          onPageSizeChange={pagination.setPageSize}
        />
      )}
    </div>
  );
}

/**
 * Usage in BacklogPage or any task list:
 * 
 * ```tsx
 * import { usePagination } from "@/hooks/usePagination";
 * import { PaginationControls } from "@/components/ui/PaginationControls";
 * 
 * const { backlogTasks, isLoading } = useBacklog(projectId);
 * const pagination = usePagination(backlogTasks, 50);
 * 
 * return (
 *   <div>
 *     {pagination.paginatedData.map(task => <TaskCard task={task} />)}
 *     <PaginationControls {...pagination} />
 *   </div>
 * );
 * ```
 */
