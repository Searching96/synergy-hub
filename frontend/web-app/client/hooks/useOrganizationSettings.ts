import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { organizationService, UpdateOrganizationRequest } from "@/services/organization.service";
import { useToast } from "@/hooks/use-toast";

// Query key factory
export const organizationKeys = {
  all: ['organizations'] as const,
  detail: (id: number) => [...organizationKeys.all, id] as const,
};

export function useOrganizationSettings(organizationId: number | undefined) {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  // Fetch organization details
  const {
    data: organizationResponse,
    isLoading,
    error,
  } = useQuery({
    queryKey: organizationKeys.detail(organizationId!),
    queryFn: () => organizationService.getOrganization(organizationId!),
    enabled: !!organizationId,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  const organization = organizationResponse?.data;

  // Update organization mutation
  const updateOrganizationMutation = useMutation({
    mutationFn: (data: UpdateOrganizationRequest) => {
      if (!organizationId) throw new Error("Organization ID is required");
      return organizationService.updateOrganization(organizationId, data);
    },
    onSuccess: (response) => {
      if (organizationId) {
        queryClient.invalidateQueries({ queryKey: organizationKeys.detail(organizationId) });
      }
      queryClient.invalidateQueries({ queryKey: organizationKeys.all });
      toast({
        title: "Success",
        description: "Organization settings updated successfully",
      });
    },
    onError: (error: any) => {
      const errorMessage = error.response?.data?.message || "Failed to update organization settings";
      toast({
        title: "Error",
        description: errorMessage,
        variant: "destructive",
      });
    },
  });

  // Delete organization mutation
  const deleteOrganizationMutation = useMutation({
    mutationFn: () => {
      if (!organizationId) throw new Error("Organization ID is required");
      return organizationService.deleteOrganization(organizationId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: organizationKeys.all });
      toast({
        title: "Success",
        description: "Organization deleted successfully",
      });
      // Redirect to projects or dashboard after deletion
      window.location.href = "/projects";
    },
    onError: (error: any) => {
      const errorMessage = error.response?.data?.message || "Failed to delete organization";
      toast({
        title: "Error",
        description: errorMessage,
        variant: "destructive",
      });
    },
  });

  return {
    organization,
    isLoading,
    error,
    updateOrganization: updateOrganizationMutation.mutate,
    isUpdating: updateOrganizationMutation.isPending,
    deleteOrganization: deleteOrganizationMutation.mutate,
    isDeleting: deleteOrganizationMutation.isPending,
  };
}
