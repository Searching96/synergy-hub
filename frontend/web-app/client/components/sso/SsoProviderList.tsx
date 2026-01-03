/**
 * SsoProviderList Component
 * Displays all SSO providers in cards with status badges
 * Shows provider type, enable/disable toggles, and delete options
 */

import { SsoProviderResponse, SsoProviderType } from "@/types/sso.types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Switch } from "@/components/ui/switch";
import { cn } from "@/lib/utils";
import { Trash2, Eye } from "lucide-react";

interface SsoProviderListProps {
  providers: SsoProviderResponse[];
  onToggle: (providerId: number, enabled: boolean) => void;
  onDelete: (providerId: number) => void;
  onEdit?: (provider: SsoProviderResponse) => void;
  isLoading?: boolean;
  hasAccess?: boolean;
}

const providerIcons: Record<SsoProviderType, { name: string; bgColor: string }> = {
  [SsoProviderType.OIDC]: { name: "OpenID Connect", bgColor: "bg-blue-100" },
  [SsoProviderType.SAML]: { name: "SAML 2.0", bgColor: "bg-purple-100" },
  [SsoProviderType.OAUTH2]: { name: "OAuth 2.0", bgColor: "bg-green-100" },
};

export function SsoProviderList({
  providers,
  onToggle,
  onDelete,
  onEdit,
  isLoading = false,
  hasAccess = true,
}: SsoProviderListProps) {
  if (!hasAccess) {
    return (
      <div className="flex items-center justify-center h-64 bg-gray-50 rounded-lg border-2 border-dashed border-gray-200">
        <div className="text-center">
          <h3 className="font-semibold text-gray-900 mb-2">Access Denied</h3>
          <p className="text-sm text-gray-600">
            You don't have permission to manage SSO providers
          </p>
        </div>
      </div>
    );
  }

  if (providers.length === 0) {
    return (
      <div className="flex items-center justify-center h-64 bg-gray-50 rounded-lg border-2 border-dashed border-gray-200">
        <div className="text-center">
          <h3 className="font-semibold text-gray-900 mb-2">No SSO Providers</h3>
          <p className="text-sm text-gray-600">
            Get started by registering your first SSO provider
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
      {providers.map((provider) => {
        const providerInfo = providerIcons[provider.providerType];
        return (
          <Card
            key={provider.id}
            className={cn(
              "transition-all hover:shadow-md",
              provider.enabled ? "" : "opacity-75 bg-gray-50"
            )}
          >
            <CardHeader className="pb-3">
              <div className="flex items-start justify-between gap-2">
                <div className="flex-1 min-w-0">
                  <CardTitle className="text-base truncate">
                    {provider.providerName}
                  </CardTitle>
                  <CardDescription className="text-xs mt-1">
                    ID: {provider.clientId.substring(0, 20)}
                    {provider.clientId.length > 20 ? "..." : ""}
                  </CardDescription>
                </div>
                <Badge
                  className={cn(
                    "shrink-0 text-xs font-semibold",
                    provider.enabled
                      ? "bg-green-100 text-green-800"
                      : "bg-gray-200 text-gray-700"
                  )}
                >
                  {provider.enabled ? "Enabled" : "Disabled"}
                </Badge>
              </div>
            </CardHeader>

            <CardContent className="space-y-4">
              {/* Provider Type Badge */}
              <div className={cn("px-3 py-2 rounded text-sm font-medium text-center", providerInfo.bgColor)}>
                {providerInfo.name}
              </div>

              {/* Metadata URL if available */}
              {provider.metadataUrl && (
                <div className="space-y-1">
                  <p className="text-xs text-gray-600">Metadata URL:</p>
                  <p className="text-xs text-blue-600 truncate break-all">
                    {provider.metadataUrl}
                  </p>
                </div>
              )}

              {/* Created Date */}
              <div className="text-xs text-gray-500 border-t pt-2">
                Created {new Date(provider.createdAt).toLocaleDateString()}
              </div>

              {/* Actions */}
              <div className="flex gap-2 pt-2">
                {/* Toggle Button */}
                <div className="flex items-center gap-2 flex-1 px-2 py-1 rounded border border-gray-200 bg-gray-50">
                  <span className="text-xs text-gray-600">
                    {provider.enabled ? "On" : "Off"}
                  </span>
                  <Switch
                    checked={provider.enabled}
                    onCheckedChange={(checked) => onToggle(provider.id, checked)}
                    disabled={isLoading}
                  />
                </div>

                {/* Edit Button */}
                {onEdit && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => onEdit(provider)}
                    disabled={isLoading}
                    className="px-2"
                  >
                    <Eye className="h-4 w-4" />
                  </Button>
                )}

                {/* Delete Button */}
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => onDelete(provider.id)}
                  disabled={isLoading}
                  className="px-2 hover:text-red-600 hover:bg-red-50"
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
