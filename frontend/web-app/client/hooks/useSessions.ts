/**
 * useSessions Hook
 * Manages active sessions queries and mutations with TanStack Query
 * Handles 403 Forbidden (RBAC) gracefully
 */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { sessionService } from "@/services/session.service";
import type { Session } from "@/types/session.types";
import type { ApiResponse } from "@/types/auth.types";
import { toast } from "sonner";
import axios from "axios";

/**
 * Query key factory for TanStack Query
 */
const sessionKeys = {
  all: ["sessions"] as const,
  list: () => ["sessions", "list"] as const,
};

/**
 * Main Sessions Hook
 */
export const useSessions = () => {
  const queryClient = useQueryClient();

  /**
   * Query: Fetch all active sessions
   * Handles 403 Forbidden by returning empty array with error state
   */
  const sessionsQuery = useQuery({
    queryKey: sessionKeys.list(),
    queryFn: sessionService.getSessions,
    staleTime: 1 * 60 * 1000, // 1 minute
    retry: (failureCount, error) => {
      // Don't retry on 403 Forbidden
      if (axios.isAxiosError(error) && error.response?.status === 403) {
        return false;
      }
      return failureCount < 3;
    },
  });

  /**
   * Mutation: Revoke a specific session
   */
  const revokeMutation = useMutation({
    mutationFn: (sessionId: string) => sessionService.revokeSession(sessionId),

    onMutate: async (sessionId) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: sessionKeys.list() });

      // Snapshot previous state
      const previousSessions = queryClient.getQueryData<ApiResponse<Session[]>>(
        sessionKeys.list()
      );

      // Optimistic update: Remove the session from the list
      queryClient.setQueryData<ApiResponse<Session[]>>(
        sessionKeys.list(),
        (old) => {
          if (!old?.data) return old;
          return {
            ...old,
            data: old.data.filter((session) => session.id !== sessionId),
          };
        }
      );

      return { previousSessions };
    },

    onError: (error, _variables, context) => {
      // Rollback on error
      if (context?.previousSessions) {
        queryClient.setQueryData(sessionKeys.list(), context.previousSessions);
      }

      // Handle 403 Forbidden
      if (axios.isAxiosError(error)) {
        if (error.response?.status === 403) {
          toast.error("Access Denied: You do not have permission to revoke sessions");
          return;
        }
      }

      toast.error("Failed to revoke session");
    },

    onSuccess: () => {
      // Invalidate and refetch
      queryClient.invalidateQueries({ queryKey: sessionKeys.list() });
      toast.success("Session revoked successfully");
    },
  });

  /**
   * Mutation: Revoke all other sessions
   */
  const revokeAllOtherMutation = useMutation({
    mutationFn: () => sessionService.revokeAllOtherSessions(),

    onMutate: async () => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: sessionKeys.list() });

      // Snapshot previous state
      const previousSessions = queryClient.getQueryData<ApiResponse<Session[]>>(
        sessionKeys.list()
      );

      // Optimistic update: Keep only the current session
      queryClient.setQueryData<ApiResponse<Session[]>>(
        sessionKeys.list(),
        (old) => {
          if (!old?.data) return old;
          return {
            ...old,
            data: old.data.filter((session) => session.isCurrent),
          };
        }
      );

      return { previousSessions };
    },

    onError: (error, _variables, context) => {
      // Rollback on error
      if (context?.previousSessions) {
        queryClient.setQueryData(sessionKeys.list(), context.previousSessions);
      }

      // Handle 403 Forbidden
      if (axios.isAxiosError(error)) {
        if (error.response?.status === 403) {
          toast.error("Access Denied: You do not have permission to revoke sessions");
          return;
        }
      }

      toast.error("Failed to revoke other sessions");
    },

    onSuccess: () => {
      // Invalidate and refetch
      queryClient.invalidateQueries({ queryKey: sessionKeys.list() });
      toast.success("All other sessions revoked successfully");
    },
  });

  // Check if user has access (not 403 Forbidden)
  const hasAccess =
    !axios.isAxiosError(sessionsQuery.error) ||
    sessionsQuery.error.response?.status !== 403;

  return {
    // Query data
    sessions: sessionsQuery.data?.data || [],
    isLoading: sessionsQuery.isLoading,
    isError: sessionsQuery.isError,
    error: sessionsQuery.error,
    hasAccess,

    // Mutations
    revokeSession: revokeMutation.mutate,
    isRevoking: revokeMutation.isPending,
    revokeAllOther: revokeAllOtherMutation.mutate,
    isRevokingAll: revokeAllOtherMutation.isPending,
  };
};
