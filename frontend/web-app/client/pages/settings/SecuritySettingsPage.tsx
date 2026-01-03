import { useState } from "react";
import axios from "axios";
import { useSSOConfigs } from "@/hooks/useSSOConfigs";
import { SsoProviderForm } from "@/components/sso/SsoProviderForm";
import { SsoProviderList } from "@/components/sso/SsoProviderList";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Shield, Plus, AlertCircle } from "lucide-react";
import { SsoProviderType } from "@/types/sso.types";
import type { RegisterSsoProviderRequest } from "@/types/sso.types";

export default function SecuritySettingsPage() {
  const {
    providers,
    isLoadingProviders,
    isErrorProviders,
    errorProviders,
    hasAccess,
    register,
    isRegistering,
    toggle,
    isToggling,
    delete: deleteProvider,
    isDeleting,
  } = useSSOConfigs();

  const [showForm, setShowForm] = useState(false);
  const [selectedProviderType, setSelectedProviderType] =
    useState<SsoProviderType>(SsoProviderType.OIDC);
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);

  const handleRegister = (data: RegisterSsoProviderRequest) => {
    register(data, {
      onSuccess: () => {
        setShowForm(false);
      },
    });
  };

  const handleToggle = (providerId: number, enabled: boolean) => {
    toggle({ providerId, enabled });
  };

  const handleDeleteClick = (providerId: number) => {
    setDeleteConfirmId(providerId);
  };

  const handleConfirmDelete = () => {
    if (deleteConfirmId !== null) {
      deleteProvider(deleteConfirmId);
      setDeleteConfirmId(null);
    }
  };

  // Handle API errors gracefully
  const isUnauthorized =
    axios.isAxiosError(errorProviders) && errorProviders.response?.status === 403;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Shield className="h-6 w-6 text-blue-600" />
            <h1 className="text-3xl font-bold text-gray-900">Security Settings</h1>
          </div>
          <p className="text-gray-600">
            Manage Single Sign-On (SSO) providers for your organization
          </p>
        </div>
        <Button
          onClick={() => setShowForm(true)}
          disabled={isRegistering || !hasAccess}
          className="gap-2"
        >
          <Plus className="h-4 w-4" />
          Register Provider
        </Button>
      </div>

      {/* 403 Forbidden Alert */}
      {isUnauthorized && (
        <Card className="border-yellow-200 bg-yellow-50">
          <CardContent className="pt-6">
            <div className="flex gap-3">
              <AlertCircle className="h-5 w-5 text-yellow-600 shrink-0 mt-0.5" />
              <div>
                <h3 className="font-semibold text-yellow-900">Access Denied</h3>
                <p className="text-sm text-yellow-800 mt-1">
                  You don't have permission to manage SSO providers. Contact your organization administrator.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Loading State */}
      {isLoadingProviders && (
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center justify-center h-40">
              <div className="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
            </div>
          </CardContent>
        </Card>
      )}

      {/* Providers List */}
      {!isLoadingProviders && (
        <Card>
          <CardHeader>
            <CardTitle>Active SSO Providers</CardTitle>
            <CardDescription>
              {providers.length === 0
                ? "No SSO providers configured yet"
                : `${providers.length} provider${providers.length !== 1 ? "s" : ""} available`}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <SsoProviderList
              providers={providers}
              onToggle={handleToggle}
              onDelete={handleDeleteClick}
              isLoading={isToggling || isDeleting}
              hasAccess={hasAccess}
            />
          </CardContent>
        </Card>
      )}

      {/* Provider Information */}
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">OIDC Configuration</CardTitle>
          </CardHeader>
          <CardContent className="text-sm space-y-2">
            <p className="text-gray-600">
              OpenID Connect is recommended for cloud-native SSO integrations.
            </p>
            <ul className="list-disc list-inside space-y-1 text-gray-600 text-xs">
              <li>Google Workspace</li>
              <li>Azure AD / Entra ID</li>
              <li>Auth0</li>
              <li>Custom OIDC providers</li>
            </ul>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">SAML Configuration</CardTitle>
          </CardHeader>
          <CardContent className="text-sm space-y-2">
            <p className="text-gray-600">
              SAML 2.0 is ideal for enterprise identity providers.
            </p>
            <ul className="list-disc list-inside space-y-1 text-gray-600 text-xs">
              <li>Okta</li>
              <li>Active Directory Federation Services</li>
              <li>OneLogin</li>
              <li>Enterprise IdPs</li>
            </ul>
          </CardContent>
        </Card>
      </div>

      {/* SSO Provider Form Dialog */}
      <SsoProviderForm
        open={showForm}
        onOpenChange={setShowForm}
        onSubmit={handleRegister}
        isLoading={isRegistering}
        providerType={selectedProviderType}
        onProviderTypeChange={setSelectedProviderType}
      />

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={deleteConfirmId !== null} onOpenChange={(open) => {
        if (!open) setDeleteConfirmId(null);
      }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete SSO Provider?</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone. The SSO provider will be permanently
              deleted and users will no longer be able to authenticate using this provider.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <div className="flex gap-3 justify-end">
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleConfirmDelete}
              className="bg-red-600 hover:bg-red-700"
            >
              Delete
            </AlertDialogAction>
          </div>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
