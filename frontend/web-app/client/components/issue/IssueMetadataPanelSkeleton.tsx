/**
 * IssueMetadataPanel Skeleton Loader
 * Shows placeholder while metadata is loading
 */

import { Skeleton } from "@/components/ui/skeleton";

export function IssueMetadataPanelSkeleton() {
  return (
    <div className="space-y-6">
      {/* Status skeleton */}
      <div>
        <Skeleton className="h-4 w-12 mb-2" />
        <Skeleton className="h-10 w-full" />
      </div>

      {/* Priority skeleton */}
      <div>
        <Skeleton className="h-4 w-12 mb-2" />
        <Skeleton className="h-10 w-full" />
      </div>

      {/* Assignee skeleton */}
      <div>
        <Skeleton className="h-4 w-16 mb-2" />
        <Skeleton className="h-10 w-full" />
      </div>

      {/* Due date skeleton */}
      <div>
        <Skeleton className="h-4 w-14 mb-2" />
        <Skeleton className="h-10 w-full" />
      </div>

      {/* Story points skeleton */}
      <div>
        <Skeleton className="h-4 w-20 mb-2" />
        <Skeleton className="h-10 w-full" />
      </div>
    </div>
  );
}
