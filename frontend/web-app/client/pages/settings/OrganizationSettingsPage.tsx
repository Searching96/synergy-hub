import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom"; // Added import
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { useAuth } from "@/context/AuthContext";
import { useOrganizationSettings } from "@/hooks/useOrganizationSettings";
import { DeleteOrganizationModal } from "@/components/modals/DeleteOrganizationModal";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { Building2, Mail, MapPin, Save, Trash2, AlertTriangle, Loader2 } from "lucide-react";

import { organizationService } from "@/services/organization.service";
import { toast } from "sonner";

// Zod schema matching backend DTOs
const organizationSchema = z.object({
  name: z.string()
    .min(3, "Organization name must be at least 3 characters")
    .max(100, "Organization name must not exceed 100 characters"),
  address: z.string().optional(),
  contactEmail: z.string()
    .email("Invalid email format")
    .optional()
    .or(z.literal("")),
});

type OrganizationFormData = z.infer<typeof organizationSchema>;

export default function OrganizationSettingsPage() {
  const { user } = useAuth();
  const navigate = useNavigate(); // Hook for navigation
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [accessDenied, setAccessDenied] = useState(false);

  // Get organization ID from user context
  const organizationId = user?.organizationId;

  const {
    organization,
    isLoading,
    error,
    updateOrganization,
    isUpdating,
    deleteOrganization,
    isDeleting,
  } = useOrganizationSettings(organizationId);

  const {
    register,
    handleSubmit,
    formState: { errors, isDirty },
    reset,
  } = useForm<OrganizationFormData>({
    resolver: zodResolver(organizationSchema),
    defaultValues: {
      name: "",
      address: "",
      contactEmail: "",
    },
  });

  // Reset form when organization data loads
  useEffect(() => {
    if (organization) {
      reset({
        name: organization.name || "",
        address: organization.address || "",
        contactEmail: organization.contactEmail || "",
      });
    }
    // Remove 'reset' from dependencies to prevent infinite loop
    // 'organization' changes should be sufficient trigger
  }, [organization]);

  // Handle 403 Forbidden errors
  useEffect(() => {
    if (error && (error as any).response?.status === 403) {
      setAccessDenied(true);
    }
  }, [error]);

  // Redirect if user doesn't have an organization
  if (!organizationId) {
    return (
      <div className="p-6">
        <Alert variant="destructive">
          <AlertTriangle className="h-4 w-4" />
          <AlertDescription>
            <strong>No Organization:</strong> You are not associated with any organization.
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  const onSubmit = (data: OrganizationFormData) => {
    updateOrganization({
      name: data.name,
      address: data.address || undefined,
      contactEmail: data.contactEmail || undefined,
    });
  };

  const handleDeleteConfirm = () => {
    deleteOrganization();
  };

  if (isLoading) {
    return (
      <div className="p-6 space-y-6">
        <div>
          <Skeleton className="h-8 w-64 mb-2" />
          <Skeleton className="h-4 w-96" />
        </div>
        <Card>
          <CardHeader>
            <Skeleton className="h-6 w-32" />
          </CardHeader>
          <CardContent className="space-y-4">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </CardContent>
        </Card>
      </div>
    );
  }

  if (accessDenied) {
    return (
      <div className="p-6">
        <Alert variant="destructive">
          <AlertTriangle className="h-4 w-4" />
          <AlertDescription>
            <strong>Access Denied:</strong> You do not have permission to manage organization settings.
            Only Organization Admins or Global Admins can access this page.
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  if (!organization) {
    return (
      <div className="p-6">
        <Alert variant="destructive">
          <AlertDescription>Organization not found.</AlertDescription>
        </Alert>
      </div>
    );
  }

  // Check if user can edit (should match backend logic: GLOBAL_ADMIN or ORG_ADMIN)
  const canEdit = user?.roles?.includes("GLOBAL_ADMIN") || user?.roles?.includes("ORG_ADMIN");
  const canDelete = user?.roles?.includes("GLOBAL_ADMIN") || user?.roles?.includes("ORG_ADMIN");

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold mb-2">Organization Settings</h1>
          <p className="text-muted-foreground">
            Manage your organization's general information and settings
          </p>
        </div>
        <Button
          onClick={handleSubmit(onSubmit)}
          disabled={!isDirty || isUpdating || !canEdit}
          className="gap-2"
        >
          {isUpdating ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" />
              Saving...
            </>
          ) : (
            <>
              <Save className="h-4 w-4" />
              Save Changes
            </>
          )}
        </Button>
      </div>

      {/* General Information Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Building2 className="h-5 w-5" />
            General Information
          </CardTitle>
          <CardDescription>
            Update your organization's basic information
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {/* Organization Name */}
            <div className="space-y-2">
              <Label htmlFor="name">
                Organization Name <span className="text-red-500">*</span>
              </Label>
              <div className="flex items-center gap-2">
                <Building2 className="h-4 w-4 text-muted-foreground" />
                <Input
                  id="name"
                  {...register("name")}
                  placeholder="Acme Corporation"
                  disabled={!canEdit}
                  className={errors.name ? "border-red-500" : ""}
                />
              </div>
              {errors.name && (
                <p className="text-sm text-red-600">{errors.name.message}</p>
              )}
              {!canEdit && (
                <p className="text-xs text-muted-foreground">
                  You do not have permission to edit this field
                </p>
              )}
            </div>

            {/* Address */}
            <div className="space-y-2">
              <Label htmlFor="address">Address</Label>
              <div className="flex items-center gap-2">
                <MapPin className="h-4 w-4 text-muted-foreground" />
                <Input
                  id="address"
                  {...register("address")}
                  placeholder="123 Business St, City, Country"
                  disabled={!canEdit}
                />
              </div>
            </div>

            {/* Contact Email */}
            <div className="space-y-2">
              <Label htmlFor="contactEmail">Contact Email</Label>
              <div className="flex items-center gap-2">
                <Mail className="h-4 w-4 text-muted-foreground" />
                <Input
                  id="contactEmail"
                  type="email"
                  {...register("contactEmail")}
                  placeholder="contact@organization.com"
                  disabled={!canEdit}
                  className={errors.contactEmail ? "border-red-500" : ""}
                />
              </div>
              {errors.contactEmail && (
                <p className="text-sm text-red-600">{errors.contactEmail.message}</p>
              )}
            </div>
          </form>
        </CardContent>
      </Card>

      {/* Roles & Permissions Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <svg
              className="h-5 w-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"
              />
            </svg>
            Roles & Permissions
          </CardTitle>
          <CardDescription>
            Manage user roles and access permissions within your organization
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-between">
            <div className="space-y-1">
              <h3 className="font-medium text-sm">Role Configuration</h3>
              <p className="text-sm text-muted-foreground">
                Define what users effectively can do in your organization
              </p>
            </div>
            <Button
              variant="outline"
              onClick={() => navigate("/settings/roles")}
              className="gap-2"
            >
              Manage Roles
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Invite Code Card */}
      {canEdit && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Mail className="h-5 w-5" />
              Invite Code
            </CardTitle>
            <CardDescription>
              Generate a unique code for users to join your organization
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between p-4 border rounded-lg bg-muted/50">
              <div className="space-y-1">
                {organization.inviteCode ? (
                  <>
                    <p className="text-sm font-medium text-muted-foreground">Active Invite Code</p>
                    <div className="flex items-center gap-2">
                      <code className="text-xl font-mono font-bold bg-background px-2 py-1 rounded border">
                        {organization.inviteCode}
                      </code>
                      {organization.inviteCodeExpiresAt && new Date(organization.inviteCodeExpiresAt) < new Date() && (
                        <span className="text-xs text-red-500 font-semibold bg-red-100 dark:bg-red-900/30 px-2 py-0.5 rounded">EXPIRED</span>
                      )}
                    </div>
                    {organization.inviteCodeExpiresAt && (
                      <p className="text-xs text-muted-foreground mt-1">
                        Expires: {new Date(organization.inviteCodeExpiresAt).toLocaleDateString()} {new Date(organization.inviteCodeExpiresAt).toLocaleTimeString()}
                      </p>
                    )}
                  </>
                ) : (
                  <p className="text-sm text-muted-foreground">No active invite code generated yet.</p>
                )}
              </div>
              <Button
                onClick={async () => {
                  try {
                    await organizationService.generateInviteCode(organization.id);
                    window.location.reload(); // Simple reload to fetch new code since we don't have a specific hook action for this yet
                  } catch (e) {
                    console.error("Failed to generate code", e);
                  }
                }}
                variant="secondary"
              >
                Generate New Code
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Danger Zone Card */}
      {canDelete && (
        <Card className="border-red-200 dark:border-red-900">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-red-600">
              <AlertTriangle className="h-5 w-5" />
              Danger Zone
            </CardTitle>
            <CardDescription>
              Irreversible actions that affect your organization
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex items-start justify-between p-4 border border-red-200 dark:border-red-900 rounded-lg bg-red-50 dark:bg-red-950/20">
              <div className="space-y-1 flex-1">
                <h3 className="font-semibold text-red-900 dark:text-red-400">
                  Delete Organization
                </h3>
                <p className="text-sm text-red-700 dark:text-red-300">
                  Once deleted, this organization and all associated data will be permanently removed.
                  This action cannot be undone.
                </p>
              </div>
              <Button
                variant="destructive"
                onClick={() => setDeleteModalOpen(true)}
                className="ml-4 gap-2"
                disabled={isDeleting}
              >
                <Trash2 className="h-4 w-4" />
                Delete Organization
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Delete Confirmation Modal */}
      <DeleteOrganizationModal
        open={deleteModalOpen}
        onOpenChange={setDeleteModalOpen}
        organizationName={organization.name}
        onConfirm={handleDeleteConfirm}
        isDeleting={isDeleting}
      />
    </div>
  );
}
