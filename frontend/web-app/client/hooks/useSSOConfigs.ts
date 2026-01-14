/**
 * useSSOConfigs Hook
 * Manages SSO provider queries and mutations with TanStack Query
 * Handles 403 Forbidden (RBAC) gracefully by showing disabled states
 */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback, useMemo } from "react";
import { ssoService } from "@/services/sso.service";
import type {
  SsoProviderResponse,
  RegisterSsoProviderRequest,
  UpdateSsoProviderRequest,
} from "@/types/sso.types";
import { toast } from "sonner";
import axios from "axios";

/**
 * Query key factory for TanStack Query
 */
const ssoKeys = {
  all: ["sso"] as const,
  providers: ["sso", "providers"] as const,
  provider: (providerId: number) => ["sso", "providers", providerId] as const,
};

/**
 * Main SSO Configuration Hook
 */
export const useSSOConfigs = (organizationId?: number) => {
  const queryClient = useQueryClient();

  const orgId = useMemo(() => {
    if (organizationId !== undefined) return organizationId;
    const stored = localStorage.getItem("organizationId");
    return stored ? Number(stored) : null;
  }, [organizationId]);

  const providersEnabled = orgId !== null;

  /**
   * Query: Fetch all SSO providers
   * Handles 403 Forbidden by returning empty array with error state
   */
  const providersQuery = useQuery({
    queryKey: ssoKeys.providers,
    queryFn: () => ssoService.getSsoProviders(orgId || undefined),
    enabled: providersEnabled,
    staleTime: 5 * 60 * 1000, // 5 minutes
    retry: (failureCount, error) => {
      // Don't retry on 403 Forbidden or client errors
      if (axios.isAxiosError(error)) {
        const status = error.response?.status || 0;
        if ([400, 401, 403, 404, 422].includes(status)) {
          return false;
        }
      }
      return failureCount < 2;
    },
  });

  /**
   * Mutation: Register a new SSO provider
   * Optimistic update: Update UI immediately, revert on error
   */
  const registerMutation = useMutation({
    mutationFn: (request: RegisterSsoProviderRequest) =>
      ssoService.registerSsoProvider(request),

    onMutate: async (newProvider) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: ssoKeys.providers });

      // Snapshot previous state
      const previousProviders = queryClient.getQueryData<SsoProviderResponse[]>(
        ssoKeys.providers
      );

      // Optimistic update with a temporary provider
      queryClient.setQueryData<SsoProviderResponse[]>(ssoKeys.providers, (old) => {
        if (!old) return old;
        return [
          ...old,
          {
            id: -1, // Temporary ID
            providerType: newProvider.providerType,
            providerName: newProvider.providerName,
            clientId: newProvider.clientId,
            enabled: false,
            createdAt: new Date().toISOString(),
          },
        ];
      });

      return { previousProviders };
    },

    onError: (error, _variables, context) => {
      // Rollback on error
      if (context?.previousProviders) {
        queryClient.setQueryData(ssoKeys.providers, context.previousProviders);
      }

      // Handle 403 Forbidden
      if (axios.isAxiosError(error)) {
        if (error.response?.status === 403) {
          toast.error("Access Denied: You do not have permission to register SSO providers");
          return;
        }
      }

      toast.error("Failed to register SSO provider");
    },

    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ssoKeys.providers });
      toast.success(`SSO provider "${data.providerName}" registered successfully`);
    },
  });

  /**
   * Mutation: Toggle SSO provider (enable/disable)
   */
  const toggleMutation = useMutation({
    mutationFn: ({
      providerId,
      enabled,
    }: {
      providerId: number;
      enabled: boolean;
    }) =>
      enabled
        ? ssoService.enableSsoProvider(providerId)
        : ssoService.disableSsoProvider(providerId),

    onMutate: async ({ providerId, enabled }) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: ssoKeys.providers });

      // Snapshot previous state
      const previousProviders = queryClient.getQueryData<SsoProviderResponse[]>(
        ssoKeys.providers
      );

      // Optimistic update
      queryClient.setQueryData<SsoProviderResponse[]>(ssoKeys.providers, (old) => {
        if (!old) return old;
        return old.map((provider) =>
          provider.id === providerId ? { ...provider, enabled } : provider
        );
      });

      return { previousProviders };
    },

    onError: (error, { providerId }, context) => {
      // Rollback on error
      if (context?.previousProviders) {
        queryClient.setQueryData(ssoKeys.providers, context.previousProviders);
      }

      // Handle 403 Forbidden
      if (axios.isAxiosError(error)) {
        if (error.response?.status === 403) {
          toast.error("Access Denied: You do not have permission to modify SSO providers");
          return;
        }
      }

      toast.error("Failed to toggle SSO provider");
    },

    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ssoKeys.providers });
      toast.success(
        `SSO provider "${data.providerName}" ${data.enabled ? "enabled" : "disabled"}`
      );
    },
  });

  /**
   * Mutation: Update SSO provider configuration
   */
  const updateMutation = useMutation({
    mutationFn: ({
      providerId,
      request,
    }: {
      providerId: number;
      request: UpdateSsoProviderRequest;
    }) => ssoService.updateSsoProvider(providerId, request),

    onError: (error) => {
      if (axios.isAxiosError(error)) {
        if (error.response?.status === 403) {
          toast.error("Access Denied: You do not have permission to update SSO providers");
          return;
        }
      }

      toast.error("Failed to update SSO provider");
    },

    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ssoKeys.providers });
      toast.success(`SSO provider "${data.providerName}" updated successfully`);
    },
  });

  /**
   * Mutation: Delete SSO provider
   */
  const deleteMutation = useMutation({
    mutationFn: (providerId: number) => ssoService.deleteSsoProvider(providerId),

    onMutate: async (providerId) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: ssoKeys.providers });

      // Snapshot previous state
      const previousProviders = queryClient.getQueryData<SsoProviderResponse[]>(
        ssoKeys.providers
      );

      // Optimistic update: Remove the provider
      queryClient.setQueryData<SsoProviderResponse[]>(ssoKeys.providers, (old) => {
        if (!old) return old;
        return old.filter((provider) => provider.id !== providerId);
      });

      return { previousProviders };
    },

    onError: (error, _providerId, context) => {
      // Rollback on error
      if (context?.previousProviders) {
        queryClient.setQueryData(ssoKeys.providers, context.previousProviders);
      }

      if (axios.isAxiosError(error)) {
        if (error.response?.status === 403) {
          toast.error("Access Denied: You do not have permission to delete SSO providers");
          return;
        }
      }

      toast.error("Failed to delete SSO provider");
    },

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ssoKeys.providers });
      toast.success("SSO provider deleted successfully");
    },
  });

  /**
   * Mutation: Rotate SSO provider secret
   */
  const rotateMutation = useMutation({
    mutationFn: (providerId: number) => ssoService.rotateSsoSecret(providerId),

    onError: (error) => {
      if (axios.isAxiosError(error)) {
        if (error.response?.status === 403) {
          toast.error("Access Denied: You do not have permission to rotate secrets");
          return;
        }
      }

      toast.error("Failed to rotate SSO provider secret");
    },

    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ssoKeys.providers });
      toast.success(`Secret rotated successfully for "${data.providerName}"`);
    },
  });

  /**
   * Check if user has access (403 error handling)
   */
  const hasAccess = useCallback(() => {
    if (!providersEnabled) return false;
    return (
      providersQuery.isError === false ||
      !axios.isAxiosError(providersQuery.error) ||
      providersQuery.error.response?.status !== 403
    );
  }, [providersEnabled, providersQuery.isError, providersQuery.error]);

  return {
    // Queries
    providers: providersQuery.data || [],
    isLoadingProviders: providersQuery.isLoading,
    isErrorProviders: providersQuery.isError,
    errorProviders: providersQuery.error,
    hasAccess: hasAccess(),
    isOrgMissing: !providersEnabled,
    refetchProviders: providersQuery.refetch,

    // Mutations
    register: registerMutation.mutate,
    isRegistering: registerMutation.isPending,
    toggle: toggleMutation.mutate,
    isToggling: toggleMutation.isPending,
    update: updateMutation.mutate,
    isUpdating: updateMutation.isPending,
    delete: deleteMutation.mutate,
    isDeleting: deleteMutation.isPending,
    rotateSecret: rotateMutation.mutate,
    isRotatingSecret: rotateMutation.isPending,
  };
};
